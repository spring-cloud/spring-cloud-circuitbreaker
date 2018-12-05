/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.r4j.core;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;

/**
 * @author Ryan Baxter
 */
public class ReactiveR4JCircuitBreaker implements ReactiveCircuitBreaker {

	private String id;
	private io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig;
	private CircuitBreakerRegistry registry;
	private TimeLimiterConfig timeLimiterConfig;

	public ReactiveR4JCircuitBreaker(String id, io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig,
									 TimeLimiterConfig timeLimiterConfig, CircuitBreakerRegistry circuitBreakerRegistry) {
		this.id = id;
		this.circuitBreakerConfig = circuitBreakerConfig;
		this.registry = circuitBreakerRegistry;
		this.timeLimiterConfig = timeLimiterConfig;
	}

	@Override
	public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback) {
		io.github.resilience4j.circuitbreaker.CircuitBreaker defaultCircuitBreaker = registry.circuitBreaker(id, circuitBreakerConfig);
		Mono<T> toReturn = toRun.transform(CircuitBreakerOperator.of(defaultCircuitBreaker)).timeout(timeLimiterConfig.getTimeoutDuration());
		if(fallback != null) {
			toReturn = toReturn.onErrorResume(fallback);
		}
		return toReturn;
	}

	public<T> Flux<T> run(Flux<T> toRun) {
		return run(toRun, null);
	}

	public<T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback) {
		io.github.resilience4j.circuitbreaker.CircuitBreaker defaultCircuitBreaker = registry.circuitBreaker(id, circuitBreakerConfig);
		Flux<T> toReturn = toRun.transform(CircuitBreakerOperator.of(defaultCircuitBreaker)).timeout(timeLimiterConfig.getTimeoutDuration());
		if(fallback != null) {
			toReturn = toReturn.onErrorResume(fallback);
		}
		return toReturn;
	}

}
