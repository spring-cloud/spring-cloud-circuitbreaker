/*
 * Copyright 2013-2021 the original author or authors.
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

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;

import static org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreaker.CIRCUIT_BREAKER_GROUP_TAG;

/**
 * @author Ryan Baxter
 * @author Thomas Vitale
 * @author 荒
 * @author Yavor Chamov
 */
public class ReactiveResilience4JCircuitBreaker implements ReactiveCircuitBreaker {

	private final String id;

	private final String groupName;

	private final ReactiveResilience4jBulkheadProvider bulkheadProvider;

	private final io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig;

	private final CircuitBreakerRegistry circuitBreakerRegistry;

	private final TimeLimiterConfig timeLimiterConfig;

	private final TimeLimiterRegistry timeLimiterRegistry;

	private final Optional<Customizer<CircuitBreaker>> circuitBreakerCustomizer;

	private final boolean disableTimeLimiter;

	@Deprecated
	public ReactiveResilience4JCircuitBreaker(String id, String groupName,
			Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration config,
			CircuitBreakerRegistry circuitBreakerRegistry, TimeLimiterRegistry timeLimiterRegistry,
			Optional<Customizer<CircuitBreaker>> circuitBreakerCustomizer,
			ReactiveResilience4jBulkheadProvider bulkheadProvider) {
		this(id, groupName, config, circuitBreakerRegistry, timeLimiterRegistry, circuitBreakerCustomizer,
				bulkheadProvider, false);
	}

	public ReactiveResilience4JCircuitBreaker(String id, String groupName,
			Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration config,
			CircuitBreakerRegistry circuitBreakerRegistry, TimeLimiterRegistry timeLimiterRegistry,
			Optional<Customizer<CircuitBreaker>> circuitBreakerCustomizer,
			ReactiveResilience4jBulkheadProvider bulkheadProvider, boolean disableTimeLimiter) {
		this.id = id;
		this.groupName = groupName;
		this.circuitBreakerConfig = config.getCircuitBreakerConfig();
		this.circuitBreakerRegistry = circuitBreakerRegistry;
		this.circuitBreakerCustomizer = circuitBreakerCustomizer;
		this.timeLimiterConfig = config.getTimeLimiterConfig();
		this.timeLimiterRegistry = timeLimiterRegistry;
		this.bulkheadProvider = bulkheadProvider;
		this.disableTimeLimiter = disableTimeLimiter;
	}

	@Override
	public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback) {
		final Map<String, String> tags = Map.of(CIRCUIT_BREAKER_GROUP_TAG, this.groupName);
		Tuple2<CircuitBreaker, Optional<TimeLimiter>> tuple = buildCircuitBreakerAndTimeLimiter();
		Mono<T> toReturn;
		if (bulkheadProvider != null) {
			toReturn = bulkheadProvider.decorateMono(groupName, tags, toRun);
		}
		else {
			toReturn = toRun;
		}
		toReturn = toReturn.transform(CircuitBreakerOperator.of(tuple.getT1()));
		if (tuple.getT2().isPresent()) {
			final Duration timeoutDuration = tuple.getT2().get().getTimeLimiterConfig().getTimeoutDuration();
			toReturn = toReturn.timeout(timeoutDuration)
				// Since we are using the Mono timeout we need to tell the circuit
				// breaker
				// about the error
				.doOnError(TimeoutException.class,
						t -> tuple.getT1().onError(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS, t));
		}
		if (fallback != null) {
			toReturn = toReturn.onErrorResume(fallback);
		}
		return toReturn;
	}

	@Override
	public <T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback) {
		final Map<String, String> tags = Map.of(CIRCUIT_BREAKER_GROUP_TAG, this.groupName);
		Tuple2<CircuitBreaker, Optional<TimeLimiter>> tuple = buildCircuitBreakerAndTimeLimiter();
		Flux<T> toReturn;
		if (bulkheadProvider != null) {
			toReturn = bulkheadProvider.decorateFlux(groupName, tags, toRun);
		}
		else {
			toReturn = toRun;
		}
		toReturn = toReturn.transform(CircuitBreakerOperator.of(tuple.getT1()));
		if (tuple.getT2().isPresent()) {
			final Duration timeoutDuration = tuple.getT2().get().getTimeLimiterConfig().getTimeoutDuration();
			toReturn = toReturn.timeout(timeoutDuration)
				// Since we are using the Flux timeout we need to tell the circuit
				// breaker
				// about the error
				.doOnError(TimeoutException.class,
						t -> tuple.getT1().onError(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS, t));
		}
		if (fallback != null) {
			toReturn = toReturn.onErrorResume(fallback);
		}
		return toReturn;
	}

	private Tuple2<CircuitBreaker, Optional<TimeLimiter>> buildCircuitBreakerAndTimeLimiter() {
		final Map<String, String> tags = Map.of(CIRCUIT_BREAKER_GROUP_TAG, this.groupName);
		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(id, circuitBreakerConfig, tags);
		circuitBreakerCustomizer.ifPresent(customizer -> customizer.customize(circuitBreaker));
		if (disableTimeLimiter) {
			/* do not provide/load time-limiter */
			return Tuples.of(circuitBreaker, Optional.empty());
		}
		/* provide time-limiter */
		TimeLimiter timeLimiter = this.timeLimiterRegistry.find(this.id)
			.orElseGet(() -> this.timeLimiterRegistry.find(this.groupName)
				.orElseGet(() -> this.timeLimiterRegistry.timeLimiter(this.id, this.timeLimiterConfig, tags)));
		return Tuples.of(circuitBreaker, Optional.of(timeLimiter));
	}

}
