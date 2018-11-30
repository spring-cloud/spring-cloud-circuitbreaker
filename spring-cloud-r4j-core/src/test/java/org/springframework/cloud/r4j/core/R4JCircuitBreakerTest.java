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

import java.util.concurrent.Executors;
import org.junit.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;

import static org.junit.Assert.assertEquals;


/**
 * @author Ryan Baxter
 */
public class R4JCircuitBreakerTest {

	@Test
	public void run() {
		CircuitBreaker cb = new R4JCircuitBreakerFactory(new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults(), Executors.newSingleThreadExecutor()).create("foo");
		assertEquals("foobar", cb.run(() -> "foobar"));
	}

	@Test
	public void runWithFallback() {
		CircuitBreaker cb = new R4JCircuitBreakerFactory(new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults(), Executors.newSingleThreadExecutor()).create("foo");
		assertEquals("fallback", cb.run(() -> {
			throw new RuntimeException("boom");
		}, t -> "fallback"));
	}
}