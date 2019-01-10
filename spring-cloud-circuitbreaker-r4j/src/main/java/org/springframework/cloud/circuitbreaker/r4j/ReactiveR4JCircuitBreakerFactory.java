/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.circuitbreaker.r4j;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import java.util.function.Function;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreaker;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreakerFactory;
import org.springframework.util.Assert;

/**
 * @author Ryan Baxter
 */
public class ReactiveR4JCircuitBreakerFactory extends ReactiveCircuitBreakerFactory<R4JConfigBuilder.R4JCircuitBreakerConfiguration, R4JConfigBuilder> {

	private Function<String, R4JConfigBuilder.R4JCircuitBreakerConfiguration> defaultConfiguration = id ->
			new R4JConfigBuilder(id)
					.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
					.timeLimiterConfig(TimeLimiterConfig.ofDefaults())
					.build();

	private CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

	@Override
	public ReactiveCircuitBreaker create(String id) {
		Assert.hasText(id, "A CircuitBreaker must have an id.");
		R4JConfigBuilder.R4JCircuitBreakerConfiguration config = getConfigurations().computeIfAbsent(id, defaultConfiguration);
		return new ReactiveR4JCircuitBreaker(id, config,
				circuitBreakerRegistry);
	}

	@Override
	protected R4JConfigBuilder configBuilder(String id) {
		return new R4JConfigBuilder(id);
	}

	@Override
	public void configureDefault(Function<String, R4JConfigBuilder.R4JCircuitBreakerConfiguration> defaultConfiguration) {
		this.defaultConfiguration = defaultConfiguration;
	}

	public void configureCircuitBreakerRegistry(CircuitBreakerRegistry registry) {
		this.circuitBreakerRegistry = registry;
	}
}
