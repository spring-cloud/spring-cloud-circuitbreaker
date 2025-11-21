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

import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.retry.support.DefaultRetryState;
import org.springframework.retry.support.RetryTemplate;

/**
 * @author Ryan Baxter
 * @deprecated in favor of the {@link CircuitBreaker} implementation in
 * spring-cloud-circuitbreaker-framework-retry
 */
@Deprecated
public class SpringRetryCircuitBreaker implements CircuitBreaker {

	private final String id;

	private final SpringRetryConfig config;

	private final @Nullable Customizer<RetryTemplate> retryTemplateCustomizer;

	private final RetryTemplate retryTemplate;

	public SpringRetryCircuitBreaker(String id, SpringRetryConfig config,
			@Nullable Customizer<RetryTemplate> retryTemplateCustomizer) {
		this.id = id;
		this.config = config;
		this.retryTemplateCustomizer = retryTemplateCustomizer;
		this.retryTemplate = new RetryTemplate();
	}

	@Override
	public <T> T run(Supplier<T> toRun, Function<@Nullable Throwable, T> fallback) {

		retryTemplate.setBackOffPolicy(config.getBackOffPolicy());
		retryTemplate.setRetryPolicy(config.getRetryPolicy());

		if (retryTemplateCustomizer != null) {
			retryTemplateCustomizer.customize(retryTemplate);
		}

		return retryTemplate.execute(context -> toRun.get(), context -> fallback.apply(context.getLastThrowable()),
				new DefaultRetryState(id, config.isForceRefreshState(), config.getStateClassifier()));
	}

}
