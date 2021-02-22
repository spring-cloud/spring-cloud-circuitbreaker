/*
 * Copyright 2013-2018 the original author or authors.
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

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.Test;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 * @author Andrii Bohutskyi
 */
public class Resilience4JCircuitBreakerTest {

	@Test
	public void run() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(), null).create("foo");
		assertThat(cb.run(() -> "foobar")).isEqualTo("foobar");
	}

	@Test
	public void runWithFallback() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(), null).create("foo");
		assertThat((String) cb.run(() -> {
			throw new RuntimeException("boom");
		}, t -> "fallback")).isEqualTo("fallback");
	}

	@Test
	public void runWithBulkheadProvider() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(), new Resilience4jBulkheadProvider(
						ThreadPoolBulkheadRegistry.ofDefaults(), BulkheadRegistry.ofDefaults())).create("foo");
		assertThat(cb.run(() -> "foobar")).isEqualTo("foobar");
	}

	@Test
	public void runWithFallbackBulkheadProvider() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(), new Resilience4jBulkheadProvider(
						ThreadPoolBulkheadRegistry.ofDefaults(), BulkheadRegistry.ofDefaults())).create("foo");
		assertThat((String) cb.run(() -> {
			throw new RuntimeException("boom");
		}, t -> "fallback")).isEqualTo("fallback");
	}

}
