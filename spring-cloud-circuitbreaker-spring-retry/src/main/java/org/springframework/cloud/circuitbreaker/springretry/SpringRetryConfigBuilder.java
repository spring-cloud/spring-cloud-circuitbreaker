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

import org.springframework.cloud.circuitbreaker.commons.ConfigBuilder;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;

/**
 * @author Ryan Baxter
 */
public class SpringRetryConfigBuilder implements ConfigBuilder<SpringRetryConfigBuilder.SpringRetryConfig> {

	private String id;
	private RetryContext retryContext;
	private BackOffPolicy backOffPolicy;
	private RetryPolicy retryPolicy;

	public SpringRetryConfigBuilder(String id) {
		this.id = id;
	}

	public SpringRetryConfigBuilder retryContext(RetryContext retryContext) {
		this.retryContext = retryContext;
		return this;
	}

	public SpringRetryConfigBuilder backOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
		return this;
	}

	public SpringRetryConfigBuilder retryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
		return this;
	}

	@Override
	public SpringRetryConfig build() {
		SpringRetryConfig config = new SpringRetryConfig();
		config.setBackOffPolicy(this.backOffPolicy);
		config.setId(id);
		config.setRetryContext(retryContext);
		config.setRetryPolicy(retryPolicy);
		return config;
	}

	public static class SpringRetryConfig {
		private String id;
		//TODO do we need this?
		private RetryContext retryContext;
		private BackOffPolicy backOffPolicy;
		private RetryPolicy retryPolicy;

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
