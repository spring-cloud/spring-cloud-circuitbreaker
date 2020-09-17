/*
 * Copyright 2013-2019 the original author or authors.
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
import org.springframework.retry.RetryContext;
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
public class SpringRetryConfigBuilder implements ConfigBuilder<SpringRetryConfigBuilder.SpringRetryConfig> {

	private String id;

	private BackOffPolicy backOffPolicy = new NoBackOffPolicy();

	private RetryPolicy retryPolicy = new CircuitBreakerRetryPolicy();

	private boolean forceRefreshState = false;

	private Classifier<Throwable, Boolean> stateClassifier = new Classifier<Throwable, Boolean>() {
		@Override
		public Boolean classify(Throwable classifiable) {
			return false;
		}
	};

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
	 * Forces a refresh on the {@link DefaultRetryState} object.
	 * @param refersh True to refresh, false othrwise.
	 * @return The builder.
	 */
	public SpringRetryConfigBuilder forceRefreshState(boolean refersh) {
		this.forceRefreshState = forceRefreshState;
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
		SpringRetryConfig config = new SpringRetryConfig();
		config.setBackOffPolicy(this.backOffPolicy);
		config.setId(id);
		config.setRetryPolicy(retryPolicy);
		config.setForceRefreshState(forceRefreshState);
		config.setStateClassifier(stateClassifier);
		return config;
	}

	public static class SpringRetryConfig {

		private String id;

		// TODO do we need this?
		private RetryContext retryContext;

		private BackOffPolicy backOffPolicy;

		private RetryPolicy retryPolicy;

		private boolean forceRefreshState;

		private Classifier<Throwable, Boolean> stateClassifier;

		boolean isForceRefreshState() {
			return forceRefreshState;
		}

		void setForceRefreshState(boolean forceRefreshState) {
			this.forceRefreshState = forceRefreshState;
		}

		Classifier<Throwable, Boolean> getStateClassifier() {
			return stateClassifier;
		}

		void setStateClassifier(Classifier<Throwable, Boolean> stateClassifier) {
			this.stateClassifier = stateClassifier;
		}

		RetryPolicy getRetryPolicy() {
			return retryPolicy;
		}

		void setRetryPolicy(RetryPolicy retryPolicy) {
			this.retryPolicy = retryPolicy;
		}

		String getId() {
			return id;
		}

		void setId(String id) {
			this.id = id;
		}

		RetryContext getRetryContext() {
			return retryContext;
		}

		void setRetryContext(RetryContext retryContext) {
			this.retryContext = retryContext;
		}

		BackOffPolicy getBackOffPolicy() {
			return backOffPolicy;
		}

		void setBackOffPolicy(BackOffPolicy backOffPolicy) {
			this.backOffPolicy = backOffPolicy;
		}

	}

}
