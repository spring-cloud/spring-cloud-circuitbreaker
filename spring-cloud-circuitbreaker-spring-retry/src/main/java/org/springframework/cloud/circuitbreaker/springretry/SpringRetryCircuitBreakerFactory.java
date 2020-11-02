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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * @author Ryan Baxter
 */
public class SpringRetryCircuitBreakerFactory
		extends CircuitBreakerFactory<SpringRetryConfigBuilder.SpringRetryConfig, SpringRetryConfigBuilder> {

	private Function<String, SpringRetryConfigBuilder.SpringRetryConfig> defaultConfig = id -> new SpringRetryConfigBuilder(
			id).build();

	private Map<String, Customizer<RetryTemplate>> retryTemplateCustomizers = new HashMap<>();

	@Override
	protected SpringRetryConfigBuilder configBuilder(String id) {
		return new SpringRetryConfigBuilder(id);
	}

	@Override
	public void configureDefault(Function<String, SpringRetryConfigBuilder.SpringRetryConfig> defaultConfiguration) {
		this.defaultConfig = defaultConfiguration;
	}

	@Override
	public CircuitBreaker create(String id) {
		Assert.hasText(id, "A circuit breaker must have an id");
		SpringRetryConfigBuilder.SpringRetryConfig config = getConfigurations().computeIfAbsent(id, defaultConfig);
		return new SpringRetryCircuitBreaker(id, config, Optional.ofNullable(retryTemplateCustomizers.get(id)));
	}

	public void addRetryTemplateCustomizers(Customizer<RetryTemplate> customizer, String... ids) {
		for (String id : ids) {
			this.retryTemplateCustomizers.put(id, customizer);
		}
	}

}
