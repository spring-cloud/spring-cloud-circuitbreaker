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

package org.springframework.cloud.circuitbreaker.failsafe;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.cloud.circuitbreaker.commons.ConfigBuilder;

/**
 * Allows consumers to easily construct a {@link FailsafeConfig} object.
 *
 * @author Jakub Marchwicki
 */
public class FailsafeConfigBuilder
		implements ConfigBuilder<FailsafeConfigBuilder.FailsafeConfig> {

	private String id;

	// private BackOffPolicy backOffPolicy = new NoBackOffPolicy();
	//
	private RetryPolicy<? extends Object> retryPolicy = new RetryPolicy<>();

	private CircuitBreaker<? extends Object> circuitBreaker = new CircuitBreaker<>();

	// private boolean forceRefreshState = false;

	// private Classifier<Throwable, Boolean> stateClassifier = new Classifier<Throwable,
	// Boolean>() {
	// @Override
	// public Boolean classify(Throwable classifiable) {
	// return false;
	// }
	// };

	/**
	 * Constructor.
	 * @param id The id of the circuit breaker.
	 */
	public FailsafeConfigBuilder(String id) {
		this.id = id;
	}

	// /**
	// * Sets the backoff policy when retrying a failed request.
	// * @param backOffPolicy The {@link BackOffPolicy} to use.
	// * @return The builder.
	// */
	// public FailsafeConfigBuilder backOffPolicy(BackOffPolicy backOffPolicy) {
	// this.backOffPolicy = backOffPolicy;
	// return this;
	// }
	//

	public <T> FailsafeConfigBuilder retryPolicy(RetryPolicy<T> retryPolicy) {
		this.retryPolicy = retryPolicy;
		return this;
	}

	public <T> FailsafeConfigBuilder circuitBreaker(CircuitBreaker<T> circuitBreaker) {
		this.circuitBreaker = circuitBreaker;
		return this;
	}

	//
	// /**
	// * Forces a refresh on the {@link DefaultRetryState} object.
	// * @param refersh True to refresh, false othrwise.
	// * @return The builder.
	// */
	// public FailsafeConfigBuilder forceRefreshState(boolean refersh) {
	// this.forceRefreshState = forceRefreshState;
	// return this;
	// }
	//
	// /**
	// * The {@link Classifier} used by the {@link DefaultRetryState} object.
	// * @param classifier The {@code Classifier} to set.
	// * @return The builder.
	// */
	// public FailsafeConfigBuilder stateClassifier(
	// Classifier<Throwable, Boolean> classifier) {
	// this.stateClassifier = classifier;
	// return this;
	// }
	//
	@Override
	public FailsafeConfig build() {
		FailsafeConfig config = new FailsafeConfig();
		// config.setBackOffPolicy(this.backOffPolicy);
		config.setId(id);
		config.setRetryPolicy(retryPolicy);
		config.setCircuitBreaker(circuitBreaker);
		// config.setForceRefreshState(forceRefreshState);
		// config.setStateClassifier(stateClassifier);
		return config;
	}

	public static class FailsafeConfig {

		private String id;

		// TODO do we need this?
		// private RetryContext retryContext;
		//
		// private BackOffPolicy backOffPolicy;
		//
		private RetryPolicy retryPolicy;

		private CircuitBreaker circuitBreaker;

		//
		// private boolean forceRefreshState;
		//
		// private Classifier<Throwable, Boolean> stateClassifier;
		//
		// boolean isForceRefreshState() {
		// return forceRefreshState;
		// }
		//
		// void setForceRefreshState(boolean forceRefreshState) {
		// this.forceRefreshState = forceRefreshState;
		// }
		//
		// Classifier<Throwable, Boolean> getStateClassifier() {
		// return stateClassifier;
		// }
		//
		// void setStateClassifier(Classifier<Throwable, Boolean> stateClassifier) {
		// this.stateClassifier = stateClassifier;
		// }
		//
		<T> CircuitBreaker<T> getCircuitBreaker() {
			return circuitBreaker;
		}

		<T> void setCircuitBreaker(CircuitBreaker<T> circuitBreaker) {
			this.circuitBreaker = circuitBreaker;
		}

		<T> RetryPolicy<T> getRetryPolicy() {
			return retryPolicy;
		}

		<T> void setRetryPolicy(RetryPolicy<T> retryPolicy) {
			this.retryPolicy = retryPolicy;
		}

		String getId() {
			return id;
		}

		void setId(String id) {
			this.id = id;
		}

		// RetryContext getRetryContext() {
		// return retryContext;
		// }
		//
		// void setRetryContext(RetryContext retryContext) {
		// this.retryContext = retryContext;
		// }
		//
		// BackOffPolicy getBackOffPolicy() {
		// return backOffPolicy;
		// }
		//
		// void setBackOffPolicy(BackOffPolicy backOffPolicy) {
		// this.backOffPolicy = backOffPolicy;
		// }
		//

	}

}
