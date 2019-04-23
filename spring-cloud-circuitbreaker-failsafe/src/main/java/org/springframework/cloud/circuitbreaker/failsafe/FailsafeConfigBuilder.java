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

	private RetryPolicy<? extends Object> retryPolicy = new RetryPolicy<>();

	private CircuitBreaker<? extends Object> circuitBreaker = new CircuitBreaker<>();

	/**
	 * Constructor.
	 * @param id The id of the circuit breaker.
	 */
	public FailsafeConfigBuilder(String id) {
		this.id = id;
	}

	public <T> FailsafeConfigBuilder retryPolicy(RetryPolicy<T> retryPolicy) {
		this.retryPolicy = retryPolicy;
		return this;
	}

	public <T> FailsafeConfigBuilder circuitBreaker(CircuitBreaker<T> circuitBreaker) {
		this.circuitBreaker = circuitBreaker;
		return this;
	}

	@Override
	public FailsafeConfig build() {
		FailsafeConfig config = new FailsafeConfig();
		config.setId(id);
		config.setRetryPolicy(retryPolicy);
		config.setCircuitBreaker(circuitBreaker);
		return config;
	}

	public static class FailsafeConfig {

		private String id;

		private RetryPolicy retryPolicy;

		private CircuitBreaker circuitBreaker;

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

	}

}
