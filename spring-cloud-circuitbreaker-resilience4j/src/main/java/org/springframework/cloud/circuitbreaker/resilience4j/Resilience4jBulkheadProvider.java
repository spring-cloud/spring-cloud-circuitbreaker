/*
 * Copyright 2013-2020 the original author or authors.
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
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

import org.springframework.cloud.client.circuitbreaker.Customizer;

/**
 * @author Andrii Bohutskyi
 */
public class Resilience4jBulkheadProvider {

	private final ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;

	private final BulkheadRegistry bulkheadRegistry;

	private final ConcurrentHashMap<String, Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration> configurations = new ConcurrentHashMap<>();

	private Function<String, Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration> defaultConfiguration = id -> new Resilience4jBulkheadConfigurationBuilder()
			.bulkheadConfig(BulkheadConfig.ofDefaults()).threadPoolBulkheadConfig(ThreadPoolBulkheadConfig.ofDefaults())
			.build();

	public Resilience4jBulkheadProvider(ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
			BulkheadRegistry bulkheadRegistry) {
		this.bulkheadRegistry = bulkheadRegistry;
		this.threadPoolBulkheadRegistry = threadPoolBulkheadRegistry;
	}

	public void configureDefault(
			Function<String, Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration> defaultConfiguration) {
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
			Bulkhead bulkhead = bulkheadRegistry.bulkhead(id, configuration.getBulkheadConfig());
			customizer.customize(bulkhead);
		}
	}

	public void addThreadPoolBulkheadCustomizer(Customizer<ThreadPoolBulkhead> customizer, String... ids) {
		for (String id : ids) {
			Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration configuration = configurations
					.computeIfAbsent(id, defaultConfiguration);
			ThreadPoolBulkhead threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(id,
					configuration.getThreadPoolBulkheadConfig());
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
			TimeLimiter timeLimiter) {
		Supplier<CompletionStage<T>> bulkheadCall = decorateBulkhead(id, toRun);
		Supplier<CompletionStage<T>> timeLimiterCall = timeLimiter
				.decorateCompletionStage(Executors.newSingleThreadScheduledExecutor(), bulkheadCall);
		Supplier<CompletionStage<T>> circuitBreakerCall = circuitBreaker.decorateCompletionStage(timeLimiterCall);
		try {
			return circuitBreakerCall.get().toCompletableFuture().get();
		}
		catch (Exception e) {
			System.out.println("exception " + e.getMessage());
			return fallback.apply(e);
		}
	}

	private <T> Supplier<CompletionStage<T>> decorateBulkhead(final String id, final Supplier<T> supplier) {
		Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration configuration = configurations
				.computeIfAbsent(id, defaultConfiguration);

		if (bulkheadRegistry.find(id).isPresent() && !threadPoolBulkheadRegistry.find(id).isPresent()) {
			Bulkhead bulkhead = bulkheadRegistry.bulkhead(id, configuration.getBulkheadConfig());
			CompletableFuture<T> asyncCall = CompletableFuture.supplyAsync(supplier);
			return Bulkhead.decorateCompletionStage(bulkhead, () -> asyncCall);
		}
		else {
			ThreadPoolBulkhead threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(id,
					configuration.getThreadPoolBulkheadConfig());
			return threadPoolBulkhead.decorateSupplier(supplier);
		}
	}

}
