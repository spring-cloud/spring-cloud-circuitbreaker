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

import java.time.Duration;

import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.core.retry.RetryPolicy;

/**
 * Builder for {@link FrameworkRetryConfig}. Configuration follows the Spring Retry
 * CircuitBreakerRetryPolicy model with openTimeout and resetTimeout.
 *
 * @author Ryan Baxter
 */
public class FrameworkRetryConfigBuilder implements ConfigBuilder<FrameworkRetryConfig> {

	private final String id;

	private RetryPolicy retryPolicy = RetryPolicy.withDefaults();

	private Duration openTimeout = Duration.ofSeconds(20);

	private Duration resetTimeout = Duration.ofSeconds(5);

	/**
	 * Create a new builder for the given circuit breaker id.
	 * @param id the circuit breaker identifier
	 */
	public FrameworkRetryConfigBuilder(String id) {
		this.id = id;
	}

	/**
	 * Set the underlying retry policy to use when the circuit is closed. By default, uses
	 * {@link RetryPolicy#withDefaults()} which provides 3 retry attempts with a 1 second
	 * delay.
	 * @param retryPolicy the retry policy
	 * @return this builder
	 */
	public FrameworkRetryConfigBuilder retryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
		return this;
	}

	/**
	 * Set the open timeout - how long the circuit stays open before transitioning to
	 * half-open. Default is 20 seconds.
	 * @param openTimeout the open timeout duration
	 * @return this builder
	 */
	public FrameworkRetryConfigBuilder openTimeout(Duration openTimeout) {
		this.openTimeout = openTimeout;
		return this;
	}

	/**
	 * Set the reset timeout - how long to wait after a failure before resetting the
	 * circuit breaker state. If no failures occur within this timeout, the circuit
	 * breaker resets. Default is 5 seconds.
	 * @param resetTimeout the reset timeout duration
	 * @return this builder
	 */
	public FrameworkRetryConfigBuilder resetTimeout(Duration resetTimeout) {
		this.resetTimeout = resetTimeout;
		return this;
	}

	@Override
	public FrameworkRetryConfig build() {
		CircuitBreakerRetryPolicy circuitBreakerPolicy = new CircuitBreakerRetryPolicy(this.retryPolicy,
				this.openTimeout, this.resetTimeout);
		return new FrameworkRetryConfig().setId(this.id)
			.setRetryPolicy(this.retryPolicy)
			.setCircuitBreakerRetryPolicy(circuitBreakerPolicy);
	}

}
