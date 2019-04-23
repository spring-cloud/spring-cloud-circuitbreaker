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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import net.jodah.failsafe.FailsafeExecutor;

import org.springframework.cloud.circuitbreaker.commons.CircuitBreaker;
import org.springframework.cloud.circuitbreaker.commons.CircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.commons.Customizer;
import org.springframework.util.Assert;

/**
 * @author Jakub Marchwicki
 */
public class FailsafeCircuitBreakerFactory extends
		CircuitBreakerFactory<FailsafeConfigBuilder.FailsafeConfig, FailsafeConfigBuilder> {

	private Function<String, FailsafeConfigBuilder.FailsafeConfig> defaultConfig = id -> new FailsafeConfigBuilder(
			id).build();

	private Map<String, Customizer<FailsafeExecutor>> failsafeCustomizers = new HashMap<>();

	@Override
	protected FailsafeConfigBuilder configBuilder(String id) {
		return new FailsafeConfigBuilder(id);
	}

	@Override
	public void configureDefault(
			Function<String, FailsafeConfigBuilder.FailsafeConfig> defaultConfiguration) {
		this.defaultConfig = defaultConfiguration;
	}

	@Override
	public CircuitBreaker create(String id) {
		Assert.hasText(id, "A circuit breaker must have an id");
		FailsafeConfigBuilder.FailsafeConfig config = getConfigurations()
				.computeIfAbsent(id, defaultConfig);
		return new FailsafeCircuitBreaker(id, config,
				Optional.ofNullable(failsafeCustomizers.get(id)));
	}

	public void addRetryTemplateCustomizers(Customizer<FailsafeExecutor> customizer,
			String... ids) {
		for (String id : ids) {
			this.failsafeCustomizers.put(id, customizer);
		}
	}

}
