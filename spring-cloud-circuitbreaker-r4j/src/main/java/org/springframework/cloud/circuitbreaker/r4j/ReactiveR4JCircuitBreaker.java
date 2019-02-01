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
package org.springframework.cloud.circuitbreaker.r4j;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreaker;


/**
 * @author Ryan Baxter
 */
public class ReactiveR4JCircuitBreaker implements ReactiveCircuitBreaker {

	private String id;
	private R4JConfigBuilder.R4JCircuitBreakerConfiguration config;
	private CircuitBreakerRegistry registry;

	public ReactiveR4JCircuitBreaker(String id, R4JConfigBuilder.R4JCircuitBreakerConfiguration config, CircuitBreakerRegistry circuitBreakerRegistry) {
		this.id = id;
		this.config = config;
		this.registry = circuitBreakerRegistry;
	}

	@Override
	public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback) {
		io.github.resilience4j.circuitbreaker.CircuitBreaker defaultCircuitBreaker = registry.circuitBreaker(id, config.getCircuitBreakerConfig());
		Mono<T> toReturn = toRun.transform(CircuitBreakerOperator.of(defaultCircuitBreaker)).timeout(config.getTimeLimiterConfig().getTimeoutDuration());
		if(fallback != null) {
			toReturn = toReturn.onErrorResume(fallback);
		}
		return toReturn;
	}

	public<T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback) {
		io.github.resilience4j.circuitbreaker.CircuitBreaker defaultCircuitBreaker = registry.circuitBreaker(id, config.getCircuitBreakerConfig());
		Flux<T> toReturn = toRun.transform(CircuitBreakerOperator.of(defaultCircuitBreaker)).timeout(config.getTimeLimiterConfig().getTimeoutDuration());
		if(fallback != null) {
			toReturn = toReturn.onErrorResume(fallback);
		}
		return toReturn;
	}

}
