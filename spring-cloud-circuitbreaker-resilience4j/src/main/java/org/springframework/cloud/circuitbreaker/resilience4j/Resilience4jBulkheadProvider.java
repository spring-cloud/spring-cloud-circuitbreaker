/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.circuitbreaker.resilience4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.jspecify.annotations.Nullable;

import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.util.Assert;

/**
 * @author Andrii Bohutskyi
 * @author Renette Ros
 */
public class Resilience4jBulkheadProvider {

	private final ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;

	private final BulkheadRegistry bulkheadRegistry;

	private final ConcurrentHashMap<String, Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration> configurations = new ConcurrentHashMap<>();

	private Function<String, Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration> defaultConfiguration;

	private boolean semaphoreDefaultBulkhead = false;

	public Resilience4jBulkheadProvider(ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
			BulkheadRegistry bulkheadRegistry,
			Resilience4JConfigurationProperties resilience4JConfigurationProperties) {
		this.bulkheadRegistry = bulkheadRegistry;
		this.threadPoolBulkheadRegistry = threadPoolBulkheadRegistry;
		defaultConfiguration = id -> new Resilience4jBulkheadConfigurationBuilder()
			.bulkheadConfig(this.bulkheadRegistry.getDefaultConfig())
			.threadPoolBulkheadConfig(this.threadPoolBulkheadRegistry.getDefaultConfig())
			.build();
		this.semaphoreDefaultBulkhead = resilience4JConfigurationProperties.isEnableSemaphoreDefaultBulkhead();
	}

	public void configureDefault(
			Function<String, Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration> defaultConfiguration) {
		Assert.notNull(defaultConfiguration, "Default configuration must not be null");
		this.defaultConfiguration = defaultConfiguration;
	}

	public void configure(Consumer<Resilience4jBulkheadConfigurationBuilder> consumer, String... ids) {
		for (String id : ids) {
			Resilience4jBulkheadConfigurationBuilder builder = new Resilience4jBulkheadConfigurationBuilder();
			consumer.accept(builder);
			Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration configuration = builder.build();
			configurations.put(id, configuration);
		}
	}

	public void addBulkheadCustomizer(Customizer<Bulkhead> customizer, String... ids) {
		for (String id : ids) {
			Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration configuration = configurations
				.computeIfAbsent(id, defaultConfiguration);
			BulkheadConfig bulkheadConfig = configuration.getBulkheadConfig();
			Assert.notNull(bulkheadConfig, "Bulkhead configuration must not be null");
			Bulkhead bulkhead = bulkheadRegistry.bulkhead(id, bulkheadConfig);
			customizer.customize(bulkhead);
		}
	}

	public void addThreadPoolBulkheadCustomizer(Customizer<ThreadPoolBulkhead> customizer, String... ids) {
		for (String id : ids) {
			Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration configuration = configurations
				.computeIfAbsent(id, defaultConfiguration);
			ThreadPoolBulkheadConfig threadPoolBulkheadConfig = configuration.getThreadPoolBulkheadConfig();
			Assert.notNull(threadPoolBulkheadConfig, "ThreadPoolBulkhead configuration must not be null");
			ThreadPoolBulkhead threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(id, threadPoolBulkheadConfig);
			customizer.customize(threadPoolBulkhead);
		}
	}

	protected BulkheadRegistry getBulkheadRegistry() {
		return bulkheadRegistry;
	}

	protected ThreadPoolBulkheadRegistry getThreadPoolBulkheadRegistry() {
		return threadPoolBulkheadRegistry;
	}

	public <T> T run(String id, Supplier<T> toRun, Function<Throwable, T> fallback, CircuitBreaker circuitBreaker,
			@Nullable TimeLimiter timeLimiter, Map<String, String> tags) {
		Supplier<CompletionStage<T>> bulkheadCall = decorateBulkhead(id, tags, toRun);
		final Callable<T> timeLimiterCall = decorateTimeLimiter(bulkheadCall, timeLimiter);
		final Callable<T> circuitBreakerCall = circuitBreaker.decorateCallable(timeLimiterCall);
		try {
			return circuitBreakerCall.call();
		}
		catch (Throwable t) {
			return fallback.apply(t);
		}
	}

	private <T> Supplier<CompletionStage<T>> decorateBulkhead(final String id, final Map<String, String> tags,
			final Supplier<T> supplier) {
		// If the configuration was supplied via a customizer use that configuration, else
		// check if the configuration is present
		// in the registries, and if its not present in either place, use the default
		// configuration
		Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration configuration = configurations
			.computeIfAbsent(id, this::getConfiguration);

		if (useSemaphoreBulkhead(id)) {
			BulkheadConfig bulkheadConfig = configuration.getBulkheadConfig();
			Assert.notNull(bulkheadConfig, "Bulkhead configuration must not be null");
			Bulkhead bulkhead = bulkheadRegistry.bulkhead(id, bulkheadConfig, tags);
			Supplier<CompletionStage<T>> completionStageSupplier = () -> CompletableFuture.supplyAsync(supplier);
			return Bulkhead.decorateCompletionStage(bulkhead, completionStageSupplier);
		}
		else {
			ThreadPoolBulkheadConfig threadPoolBulkheadConfig = configuration.getThreadPoolBulkheadConfig();
			Assert.notNull(threadPoolBulkheadConfig, "ThreadPoolBulkhead configuration must not be null");
			ThreadPoolBulkhead threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(id, threadPoolBulkheadConfig,
					tags);
			return threadPoolBulkhead.decorateSupplier(supplier);
		}
	}

	private Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration getConfiguration(String id) {
		Resilience4jBulkheadConfigurationBuilder builder = new Resilience4jBulkheadConfigurationBuilder();
		Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration defaultConfiguration = this.defaultConfiguration
			.apply(id);
		Optional<BulkheadConfig> bulkheadConfiguration = bulkheadRegistry.getConfiguration(id);
		Optional<ThreadPoolBulkheadConfig> threadPoolBulkheadConfig = threadPoolBulkheadRegistry.getConfiguration(id);
		builder.bulkheadConfig(bulkheadConfiguration.orElse(defaultConfiguration.getBulkheadConfig()));
		builder.threadPoolBulkheadConfig(
				threadPoolBulkheadConfig.orElse(defaultConfiguration.getThreadPoolBulkheadConfig()));
		return builder.build();
	}

	public <T> Callable<T> decorateCallable(final String id, final Map<String, String> tags,
			final Callable<T> callable) {
		Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration configuration = configurations
			.computeIfAbsent(id, this::getConfiguration);

		if (useSemaphoreBulkhead(id)) {
			BulkheadConfig bulkheadConfig = configuration.getBulkheadConfig();
			Assert.notNull(bulkheadConfig, "Bulkhead configuration must not be null");
			Bulkhead bulkhead = bulkheadRegistry.bulkhead(id, bulkheadConfig, tags);
			return Bulkhead.decorateCallable(bulkhead, callable);
		}
		else {
			ThreadPoolBulkheadConfig threadPoolBulkheadConfig = configuration.getThreadPoolBulkheadConfig();
			Assert.notNull(threadPoolBulkheadConfig, "ThreadPoolBulkhead configuration must not be null");
			ThreadPoolBulkhead threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(id, threadPoolBulkheadConfig,
					tags);
			return () -> threadPoolBulkhead.decorateCallable(callable).get().toCompletableFuture().get();
		}
	}

	private boolean useSemaphoreBulkhead(String id) {
		// If we find a configuration in the threadPoolBulkheadRegistry, we assume the
		// user configured the bulkhead specifically to
		// use a threadpool so regardless of what
		// spring.cloud.circuitbreaker.resilience4j.enableSemaphoreDefaultBulkhead is set
		// to
		// we will use a threadpool for the bulkhead
		if (threadPoolBulkheadRegistry.find(id).isPresent()) {
			return false;
		}
		// If we did not find a configuration in the threadPoolBulkheadRegistry, then use
		// a semaphore bulkhead if
		// spring.cloud.circuitbreaker.resilience4j.enableSemaphoreDefaultBulkhead is set
		// to true or if we find a configuration in the
		// bulkheadRegistry. We will return false if
		// spring.cloud.circuitbreaker.resilience4j.enableSemaphoreDefaultBulkhead is
		// false and we don't
		// find a configuration in the bulkheadRegistry or the threadPoolBulkheadRegistry
		// and this will result in the bulkhead using a threadpool
		return semaphoreDefaultBulkhead || bulkheadRegistry.find(id).isPresent();
	}

	private static <T> Callable<T> decorateTimeLimiter(final Supplier<? extends CompletionStage<T>> supplier,
			@Nullable TimeLimiter timeLimiter) {
		final Supplier<Future<T>> futureSupplier = () -> supplier.get().toCompletableFuture();
		if (timeLimiter == null) {
			/* execute without time-limiter */
			return () -> futureSupplier.get().get();
		}
		return timeLimiter.decorateFutureSupplier(futureSupplier);
	}

}
