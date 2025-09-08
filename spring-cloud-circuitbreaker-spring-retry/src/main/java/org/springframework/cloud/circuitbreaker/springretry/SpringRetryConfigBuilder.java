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

package org.springframework.cloud.circuitbreaker.springretry;

import org.springframework.classify.Classifier;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.CircuitBreakerRetryPolicy;
import org.springframework.retry.support.DefaultRetryState;

/**
 * Allows consumers to easily construct a {@link SpringRetryConfig} object.
 *
 * @author Ryan Baxter
 */
public class SpringRetryConfigBuilder implements ConfigBuilder<SpringRetryConfig> {

	private final String id;

	private BackOffPolicy backOffPolicy = new NoBackOffPolicy();

	private RetryPolicy retryPolicy = new CircuitBreakerRetryPolicy();

	private boolean forceRefreshState = false;

	private Classifier<Throwable, Boolean> stateClassifier = classifiable -> false;

	/**
	 * Constructor.
	 * @param id The id of the circuit breaker.
	 */
	public SpringRetryConfigBuilder(String id) {
		this.id = id;
	}

	/**
	 * Sets the backoff policy when retrying a failed request.
	 * @param backOffPolicy The {@link BackOffPolicy} to use.
	 * @return The builder.
	 */
	public SpringRetryConfigBuilder backOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
		return this;
	}

	/**
	 * Sets the {@link RetryPolicy} to use. The {@code RetryPolicy} set here will be
	 * wrapped in a {@link CircuitBreakerRetryPolicy}.
	 * @param retryPolicy The {@code RetryPolicy} to use.
	 * @return The builder.
	 */
	public SpringRetryConfigBuilder retryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = new CircuitBreakerRetryPolicy(retryPolicy);
		return this;
	}

	/**
	 * Sets the {@link CircuitBreakerRetryPolicy} to use.
	 * @param retryPolicy The {@code CircuitBreakerRetryPolicy} to use.
	 * @return The builder.
	 */
	public SpringRetryConfigBuilder retryPolicy(CircuitBreakerRetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
		return this;
	}

	/**
	 * Forces a refresh on the {@link DefaultRetryState} object.
	 * @param refresh true to refresh, false otherwise.
	 * @return The builder.
	 */
	public SpringRetryConfigBuilder forceRefreshState(boolean refresh) {
		this.forceRefreshState = refresh;
		return this;
	}

	/**
	 * The {@link Classifier} used by the {@link DefaultRetryState} object.
	 * @param classifier The {@code Classifier} to set.
	 * @return The builder.
	 */
	public SpringRetryConfigBuilder stateClassifier(Classifier<Throwable, Boolean> classifier) {
		this.stateClassifier = classifier;
		return this;
	}

	@Override
	public SpringRetryConfig build() {
		return new SpringRetryConfig().setBackOffPolicy(backOffPolicy)
			.setId(id)
			.setRetryPolicy(retryPolicy)
			.setForceRefreshState(forceRefreshState)
			.setStateClassifier(stateClassifier);
	}

}
