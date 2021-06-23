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

package org.springframework.cloud.circuitbreaker.resilience4j.common;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * implement for CircuitBreaker.
 * @author dangzhicairang
 */
public class Resilience4JCircuitBreakerCompareAndGetter
	implements CompareAndGetter<CircuitBreaker, CircuitBreakerRegistry, CircuitBreakerConfig> {

	private static Resilience4JCircuitBreakerCompareAndGetter instance;

	public static Resilience4JCircuitBreakerCompareAndGetter getInstance() {
		if (instance == null) {
			instance = new Resilience4JCircuitBreakerCompareAndGetter();
		}
		return instance;
	}

	@Override
	public CircuitBreaker compareAndGet(String id, CircuitBreakerRegistry circuitBreakerRegistry
		, CircuitBreakerConfig circuitBreakerConfig, io.vavr.collection.Map<String, String> tags) {

		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(id);

		// compare and get
		CircuitBreakerConfig realConfig = circuitBreaker.getCircuitBreakerConfig();
		if (!realConfig.toString().equals(circuitBreakerConfig.toString())) {
			circuitBreakerRegistry.remove(id);
			circuitBreaker = circuitBreakerRegistry.circuitBreaker(id, circuitBreakerConfig, tags);
		}

		return circuitBreaker;
	}
}
