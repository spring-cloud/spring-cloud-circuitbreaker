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

import java.util.concurrent.ExecutorService;
import org.springframework.cloud.circuitbreaker.commons.CircuitBreaker;
import org.springframework.cloud.circuitbreaker.commons.CircuitBreakerFactory;

/**
 * @author Ryan Baxter
 */
public class R4JCircuitBreakerFactory implements CircuitBreakerFactory {

	private String id;
	private R4JConfigFactory r4JConfigFactory;
	private CircuitBreakerRegistry circuitBreakerRegistry;
	private ExecutorService executorService;

	public R4JCircuitBreakerFactory(R4JConfigFactory r4JConfigFactory, CircuitBreakerRegistry circuitBreakerRegistry,
									ExecutorService executorService) {
		this.r4JConfigFactory = r4JConfigFactory;
		this.circuitBreakerRegistry = circuitBreakerRegistry;
		this.executorService = executorService;
	}

	@Override
	public CircuitBreaker create(String id) {
		return new R4JCircuitBreaker(id, r4JConfigFactory.getCircuitBreakerConfig(id), r4JConfigFactory.getTimeLimiterConfig(id),
				circuitBreakerRegistry, executorService);
	}
}
