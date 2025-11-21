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

import org.jspecify.annotations.Nullable;

import org.springframework.core.retry.RetryPolicy;

/**
 * Configuration for a Framework Retry circuit breaker.
 *
 * @author Ryan Baxter
 */
public class FrameworkRetryConfig {

	private @Nullable String id;

	private @Nullable CircuitBreakerRetryPolicy circuitBreakerRetryPolicy;

	private @Nullable RetryPolicy retryPolicy;

	/**
	 * Get the circuit breaker identifier.
	 * @return the identifier
	 */
	public @Nullable String getId() {
		return this.id;
	}

	/**
	 * Set the circuit breaker identifier.
	 * @param id the identifier
	 * @return this config instance
	 */
	FrameworkRetryConfig setId(String id) {
		this.id = id;
		return this;
	}

	/**
	 * Get the circuit breaker retry policy.
	 * @return the circuit breaker retry policy
	 */
	public @Nullable CircuitBreakerRetryPolicy getCircuitBreakerRetryPolicy() {
		return this.circuitBreakerRetryPolicy;
	}

	/**
	 * Set the circuit breaker retry policy.
	 * @param circuitBreakerRetryPolicy the circuit breaker retry policy
	 * @return this config instance
	 */
	FrameworkRetryConfig setCircuitBreakerRetryPolicy(CircuitBreakerRetryPolicy circuitBreakerRetryPolicy) {
		this.circuitBreakerRetryPolicy = circuitBreakerRetryPolicy;
		return this;
	}

	/**
	 * Get the underlying retry policy.
	 * @return the retry policy
	 */
	public @Nullable RetryPolicy getRetryPolicy() {
		return this.retryPolicy;
	}

	/**
	 * Set the underlying retry policy.
	 * @param retryPolicy the retry policy
	 * @return this config instance
	 */
	FrameworkRetryConfig setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
		return this;
	}

}
