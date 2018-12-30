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

import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreaker;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreakerFactory;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * @author Ryan Baxter
 */
public class ReactiveR4JCircuitBreakerFactory implements ReactiveCircuitBreakerFactory {

	private R4JConfigFactory r4JConfigFactory;
	private CircuitBreakerRegistry circuitBreakerRegistry;
	private R4JCircuitBreakerCustomizer r4JCircuitBreakerCustomizer;

	public ReactiveR4JCircuitBreakerFactory(R4JConfigFactory r4JConfigFactory,
			CircuitBreakerRegistry circuitBreakerRegistry,
			R4JCircuitBreakerCustomizer r4JCircuitBreakerCustomizer) {
		this.r4JConfigFactory = r4JConfigFactory;
		this.circuitBreakerRegistry = circuitBreakerRegistry;
		this.r4JCircuitBreakerCustomizer = r4JCircuitBreakerCustomizer;
	}

	@Override
	public ReactiveCircuitBreaker create(String id) {
		return new ReactiveR4JCircuitBreaker(id, r4JConfigFactory.getCircuitBreakerConfig(id), r4JConfigFactory.getTimeLimiterConfig(id),
				circuitBreakerRegistry, r4JCircuitBreakerCustomizer);
	}
}
