/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.circuitbreaker.resilience4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.util.Assert;

/**
 * @author Ryan Baxter
 * @author Thomas Vitale
 * @author 荒
 */
public class ReactiveResilience4JCircuitBreakerFactory extends
		ReactiveCircuitBreakerFactory<Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration, Resilience4JConfigBuilder> {

	private Function<String, Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration> defaultConfiguration;

	private CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

	private TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();

	private Map<String, Customizer<CircuitBreaker>> circuitBreakerCustomizers = new HashMap<>();

	public ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry circuitBreakerRegistry,
			TimeLimiterRegistry timeLimiterRegistry) {
		this.circuitBreakerRegistry = circuitBreakerRegistry;
		this.timeLimiterRegistry = timeLimiterRegistry;
		this.defaultConfiguration = id -> new Resilience4JConfigBuilder(id)
				.circuitBreakerConfig(this.circuitBreakerRegistry.getDefaultConfig())
				.timeLimiterConfig(this.timeLimiterRegistry.getDefaultConfig()).build();
	}

	@Override
	public ReactiveCircuitBreaker create(String id) {
		Assert.hasText(id, "A CircuitBreaker must have an id.");
		return this.create(id, id);
	}

	/**
	 * Add support group/service config on Reactive CircuitBreaker.
	 * <ul>
	 * <li>method(id) config - on specific method or operation</li>
	 * <li>Service(group) config - on specific application service or some operations</li>
	 * <li>global default config</li>
	 * </ul>
	 * Descending priority from top to bottom.
	 * <p/>
	 * @param id operation or method name
	 * @param groupName service group name
	 * @return {@link ReactiveResilience4JCircuitBreaker}
	 */
	@Override
	public ReactiveCircuitBreaker create(String id, String groupName) {
		Assert.hasText(id, "A CircuitBreaker must have an id.");
		Assert.hasText(groupName, "A CircuitBreaker must have a group name.");
		Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration defaultConfig = getConfigurations()
				.computeIfAbsent(id, defaultConfiguration);
		CircuitBreakerConfig circuitBreakerConfig = this.circuitBreakerRegistry.getConfiguration(id)
				.orElseGet(() -> this.circuitBreakerRegistry.getConfiguration(groupName)
						.orElseGet(defaultConfig::getCircuitBreakerConfig));
		TimeLimiterConfig timeLimiterConfig = this.timeLimiterRegistry.getConfiguration(id)
				.orElseGet(() -> this.timeLimiterRegistry.getConfiguration(groupName)
						.orElseGet(defaultConfig::getTimeLimiterConfig));
		Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration config = new Resilience4JConfigBuilder(id)
				.circuitBreakerConfig(circuitBreakerConfig).timeLimiterConfig(timeLimiterConfig).build();
		return new ReactiveResilience4JCircuitBreaker(id, groupName, config, circuitBreakerRegistry,
				timeLimiterRegistry, Optional.ofNullable(circuitBreakerCustomizers.get(id)));
	}

	@Override
	protected Resilience4JConfigBuilder configBuilder(String id) {
		return new Resilience4JConfigBuilder(id);
	}

	public CircuitBreakerRegistry getCircuitBreakerRegistry() {
		return circuitBreakerRegistry;
	}

	public TimeLimiterRegistry getTimeLimiterRegistry() {
		return timeLimiterRegistry;
	}

	@Override
	public void configureDefault(
			Function<String, Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration> defaultConfiguration) {
		this.defaultConfiguration = defaultConfiguration;
	}

	public void configureCircuitBreakerRegistry(CircuitBreakerRegistry registry) {
		this.circuitBreakerRegistry = registry;
	}

	public void addCircuitBreakerCustomizer(Customizer<CircuitBreaker> customizer, String... ids) {
		for (String id : ids) {
			circuitBreakerCustomizers.put(id, customizer);
		}
	}

}
