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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.Customizer;

/**
 * @author Ryan Baxter
 * @author Andrii Bohutskyi
 * @author 荒
 * @author Renette Ros
 */
public class Resilience4JCircuitBreaker implements CircuitBreaker {

	static final String CIRCUIT_BREAKER_GROUP_TAG = "group";

	private final String id;

	private final String groupName;

	private final Map<String, String> tags;

	private Resilience4jBulkheadProvider bulkheadProvider;

	private final io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig;

	private final CircuitBreakerRegistry registry;

	private final TimeLimiterRegistry timeLimiterRegistry;

	private final TimeLimiterConfig timeLimiterConfig;

	private final ExecutorService executorService;

	private final Optional<Customizer<io.github.resilience4j.circuitbreaker.CircuitBreaker>> circuitBreakerCustomizer;

	private final boolean disableTimeLimiter;

	public Resilience4JCircuitBreaker(String id, String groupName,
			io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig,
			TimeLimiterConfig timeLimiterConfig, CircuitBreakerRegistry circuitBreakerRegistry,
			TimeLimiterRegistry timeLimiterRegistry, ExecutorService executorService,
			Optional<Customizer<io.github.resilience4j.circuitbreaker.CircuitBreaker>> circuitBreakerCustomizer,
			Resilience4jBulkheadProvider bulkheadProvider, boolean disableTimeLimiter) {
		this.id = id;
		this.groupName = groupName;
		this.circuitBreakerConfig = circuitBreakerConfig;
		this.registry = circuitBreakerRegistry;
		this.timeLimiterRegistry = timeLimiterRegistry;
		this.timeLimiterConfig = timeLimiterConfig;
		this.executorService = executorService;
		this.circuitBreakerCustomizer = circuitBreakerCustomizer;
		this.bulkheadProvider = bulkheadProvider;
		this.disableTimeLimiter = disableTimeLimiter;
		this.tags = Map.of(CIRCUIT_BREAKER_GROUP_TAG, this.groupName);
	}

	public Resilience4JCircuitBreaker(String id, String groupName,
			io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig,
			TimeLimiterConfig timeLimiterConfig, CircuitBreakerRegistry circuitBreakerRegistry,
			TimeLimiterRegistry timeLimiterRegistry,
			Optional<Customizer<io.github.resilience4j.circuitbreaker.CircuitBreaker>> circuitBreakerCustomizer,
			Resilience4jBulkheadProvider bulkheadProvider) {
		this(id, groupName, circuitBreakerConfig, timeLimiterConfig, circuitBreakerRegistry, timeLimiterRegistry, null,
				circuitBreakerCustomizer, bulkheadProvider, false);
	}

	@Override
	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
		final Map<String, String> tags = Map.of(CIRCUIT_BREAKER_GROUP_TAG, this.groupName);
		Optional<TimeLimiter> timeLimiter = loadTimeLimiter();
		io.github.resilience4j.circuitbreaker.CircuitBreaker defaultCircuitBreaker = registry.circuitBreaker(this.id,
				this.circuitBreakerConfig, tags);
		circuitBreakerCustomizer.ifPresent(customizer -> customizer.customize(defaultCircuitBreaker));
		if (bulkheadProvider != null) {

			if (executorService != null) {
				Supplier<Future<T>> futureSupplier = () -> executorService.submit(toRun::get);
				/* conditionally wrap in time-limiter */
				Callable<T> timeLimitedCall = timeLimiter
					.map(tl -> TimeLimiter.decorateFutureSupplier(tl, futureSupplier))
					.orElse(() -> futureSupplier.get().get());
				Callable<T> bulkheadCall = bulkheadProvider.decorateCallable(this.groupName, tags, timeLimitedCall);
				Callable<T> circuitBreakerCall = io.github.resilience4j.circuitbreaker.CircuitBreaker
					.decorateCallable(defaultCircuitBreaker, bulkheadCall);
				return getAndApplyFallback(circuitBreakerCall, fallback);
			}
			else {
				Callable<T> bulkheadCall = bulkheadProvider.decorateCallable(this.groupName, tags, toRun::get);
				Callable<T> circuitBreakerCall = io.github.resilience4j.circuitbreaker.CircuitBreaker
					.decorateCallable(defaultCircuitBreaker, bulkheadCall);
				return getAndApplyFallback(circuitBreakerCall, fallback);
			}
		}
		else {
			if (executorService != null) {
				Supplier<Future<T>> futureSupplier = () -> executorService.submit(toRun::get);
				/* conditionally wrap in time-limiter */
				Callable<T> restrictedCall = timeLimiter
					.map(tl -> TimeLimiter.decorateFutureSupplier(tl, futureSupplier))
					.orElse(() -> futureSupplier.get().get());
				Callable<T> callable = io.github.resilience4j.circuitbreaker.CircuitBreaker
					.decorateCallable(defaultCircuitBreaker, restrictedCall);
				return getAndApplyFallback(callable, fallback);
			}
			else {
				Supplier<T> decorator = io.github.resilience4j.circuitbreaker.CircuitBreaker
					.decorateSupplier(defaultCircuitBreaker, toRun);
				return getAndApplyFallback(decorator, fallback);
			}
		}
	}

	private static <T> T getAndApplyFallback(Supplier<T> supplier, Function<Throwable, T> fallback) {
		try {
			return supplier.get();
		}
		catch (Throwable t) {
			return fallback.apply(t);
		}
	}

	private static <T> T getAndApplyFallback(Callable<T> callable, Function<Throwable, T> fallback) {
		try {
			return callable.call();
		}
		catch (Throwable t) {
			return fallback.apply(t);
		}
	}

	private Optional<TimeLimiter> loadTimeLimiter() {
		if (disableTimeLimiter) {
			return Optional.empty();
		}
		return Optional.of(this.timeLimiterRegistry.find(this.id)
			.orElseGet(() -> this.timeLimiterRegistry.find(this.groupName)
				.orElseGet(() -> this.timeLimiterRegistry.timeLimiter(this.id, this.timeLimiterConfig, this.tags))));
	}

}
