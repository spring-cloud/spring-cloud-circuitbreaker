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

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.util.Assert;

/**
 * Factory for creating {@link FrameworkRetryCircuitBreaker} instances.
 *
 * @author Ryan Baxter
 */
public class FrameworkRetryCircuitBreakerFactory
		extends CircuitBreakerFactory<FrameworkRetryConfig, FrameworkRetryConfigBuilder> {

	private Function<String, FrameworkRetryConfig> defaultConfig = id -> new FrameworkRetryConfigBuilder(id).build();

	@Override
	protected FrameworkRetryConfigBuilder configBuilder(String id) {
		return new FrameworkRetryConfigBuilder(id);
	}

	@Override
	public void configureDefault(Function<String, FrameworkRetryConfig> defaultConfiguration) {
		this.defaultConfig = defaultConfiguration;
	}

	@Override
	public CircuitBreaker create(String id) {
		Assert.hasText(id, "A circuit breaker must have an id");
		FrameworkRetryConfig config = getConfigurations().computeIfAbsent(id, this.defaultConfig);
		return new FrameworkRetryCircuitBreaker(id, config);
	}

}
