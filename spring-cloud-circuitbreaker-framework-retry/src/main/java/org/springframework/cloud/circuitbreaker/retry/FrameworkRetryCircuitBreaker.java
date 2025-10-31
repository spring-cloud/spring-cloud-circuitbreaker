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

package org.springframework.cloud.circuitbreaker.retry;

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.core.retry.RetryTemplate;

/**
 * Circuit breaker implementation using Spring Framework 7's retry support.
 *
 * @author Ryan Baxter
 */
public class FrameworkRetryCircuitBreaker implements CircuitBreaker {

	private final String id;

	private final FrameworkRetryConfig config;

	private final CircuitBreakerRetryPolicy circuitBreakerPolicy;

	/**
	 * Create a new circuit breaker.
	 * @param id the circuit breaker identifier
	 * @param config the configuration for this circuit breaker
	 */
	public FrameworkRetryCircuitBreaker(String id, FrameworkRetryConfig config) {
		this.id = id;
		this.config = config;
		this.circuitBreakerPolicy = config.getCircuitBreakerRetryPolicy();
	}

	@Override
	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
		// Check if circuit breaker allows execution (handles open -> half-open
		// transition)
		if (!this.circuitBreakerPolicy.canRetry()) {
			// Circuit is open and timeout hasn't elapsed
			Throwable lastException = this.circuitBreakerPolicy.getLastException();
			if (lastException == null) {
				lastException = new IllegalStateException("Circuit breaker is open for: " + this.id);
			}
			return fallback.apply(lastException);
		}

		// Create a retry template with the configured policy
		RetryTemplate retryTemplate = new RetryTemplate(this.circuitBreakerPolicy.getRetryPolicy());

		try {
			// Execute with retry
			T result = retryTemplate.execute(toRun::get);

			// Record success if we get here
			this.circuitBreakerPolicy.recordSuccess();
			return result;
		}
		catch (Throwable t) {
			// Record failure after all retries are exhausted
			// This matches Spring Retry CircuitBreakerRetryPolicy behavior where
			// a "failure" is one complete failed invocation (all retries exhausted)
			this.circuitBreakerPolicy.recordFailure(t);
			return fallback.apply(t);
		}
	}

	/**
	 * Get the circuit breaker identifier.
	 * @return the identifier
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Get the circuit breaker configuration.
	 * @return the configuration
	 */
	public FrameworkRetryConfig getConfig() {
		return this.config;
	}

	/**
	 * Get the circuit breaker retry policy.
	 * @return the circuit breaker retry policy
	 */
	public CircuitBreakerRetryPolicy getCircuitBreakerPolicy() {
		return this.circuitBreakerPolicy;
	}

}
