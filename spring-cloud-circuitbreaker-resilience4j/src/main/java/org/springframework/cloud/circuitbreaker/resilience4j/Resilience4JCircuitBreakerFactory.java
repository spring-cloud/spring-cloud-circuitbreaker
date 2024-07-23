/*
 * Copyright 2013-2018 the original author or authors.
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.observation.ObservedCircuitBreaker;
import org.springframework.util.Assert;

/**
 * @author Ryan Baxter
 * @author Andrii Bohutskyi
 * @author 荒
 */
public class Resilience4JCircuitBreakerFactory extends
		CircuitBreakerFactory<Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration, Resilience4JConfigBuilder> {

	private Resilience4jBulkheadProvider bulkheadProvider;

	private Function<String, Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration> defaultConfiguration;

	private CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

	private TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();

	private ExecutorService executorService = Executors.newCachedThreadPool();

	private Function<String, ExecutorService> groupExecutorServiceFactory = group -> Executors.newCachedThreadPool();

	private ConcurrentHashMap<String, ExecutorService> executorServices = new ConcurrentHashMap<>();

	private Map<String, Customizer<CircuitBreaker>> circuitBreakerCustomizers = new HashMap<>();

	private Resilience4JConfigurationProperties resilience4JConfigurationProperties;

	private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

	public Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry circuitBreakerRegistry,
			TimeLimiterRegistry timeLimiterRegistry, Resilience4jBulkheadProvider bulkheadProvider) {
		this(circuitBreakerRegistry, timeLimiterRegistry, bulkheadProvider, new Resilience4JConfigurationProperties());
	}

	public Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry circuitBreakerRegistry,
			TimeLimiterRegistry timeLimiterRegistry, Resilience4jBulkheadProvider bulkheadProvider,
			Resilience4JConfigurationProperties resilience4JConfigurationProperties) {
		this.circuitBreakerRegistry = circuitBreakerRegistry;
		this.timeLimiterRegistry = timeLimiterRegistry;
		this.bulkheadProvider = bulkheadProvider;
		this.defaultConfiguration = id -> new Resilience4JConfigBuilder(id)
			.circuitBreakerConfig(this.circuitBreakerRegistry.getDefaultConfig())
			.timeLimiterConfig(this.timeLimiterRegistry.getDefaultConfig())
			.build();
		this.resilience4JConfigurationProperties = resilience4JConfigurationProperties;
	}

	@Override
	protected Resilience4JConfigBuilder configBuilder(String id) {
		return new Resilience4JConfigBuilder(id);
	}

	@Override
	public void configureDefault(
			Function<String, Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration> defaultConfiguration) {
		this.defaultConfiguration = defaultConfiguration;
	}

	public void configureCircuitBreakerRegistry(CircuitBreakerRegistry registry) {
		this.circuitBreakerRegistry = registry;
	}

	public CircuitBreakerRegistry getCircuitBreakerRegistry() {
		return this.circuitBreakerRegistry;
	}

	public TimeLimiterRegistry getTimeLimiterRegistry() {
		return this.timeLimiterRegistry;
	}

	public Resilience4jBulkheadProvider getBulkheadProvider() {
		return this.bulkheadProvider;
	}

	public void configureExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	/**
	 * configure GroupExecutorService.
	 * @param groupFactory GroupExecutorService Factory
	 */
	public void configureGroupExecutorService(Function<String, ExecutorService> groupFactory) {
		this.groupExecutorServiceFactory = groupFactory;
	}

	@Override
	public org.springframework.cloud.client.circuitbreaker.CircuitBreaker create(String id) {
		Assert.hasText(id, "A CircuitBreaker must have an id.");
		Resilience4JCircuitBreaker resilience4JCircuitBreaker = create(id, id, this.executorService);
		return tryObservedCircuitBreaker(resilience4JCircuitBreaker);
	}

	@Override
	public org.springframework.cloud.client.circuitbreaker.CircuitBreaker create(String id, String groupName) {
		Assert.hasText(id, "A CircuitBreaker must have an id.");
		Assert.hasText(groupName, "A CircuitBreaker must have a group name.");
		final ExecutorService groupExecutorService = executorServices.computeIfAbsent(groupName,
				groupExecutorServiceFactory);
		Resilience4JCircuitBreaker resilience4JCircuitBreaker = create(id, groupName, groupExecutorService);
		return tryObservedCircuitBreaker(resilience4JCircuitBreaker);
	}

	private org.springframework.cloud.client.circuitbreaker.CircuitBreaker tryObservedCircuitBreaker(
			Resilience4JCircuitBreaker resilience4JCircuitBreaker) {
		if (this.observationRegistry.isNoop()) {
			return resilience4JCircuitBreaker;
		}
		return new ObservedCircuitBreaker(resilience4JCircuitBreaker, this.observationRegistry);
	}

	public void addCircuitBreakerCustomizer(Customizer<CircuitBreaker> customizer, String... ids) {
		for (String id : ids) {
			circuitBreakerCustomizers.put(id, customizer);
		}
	}

	/**
	 * Add support group/service config on CircuitBreaker.
	 * <ul>
	 * <li>method(id) config - on specific method or operation</li>
	 * <li>Service(group) config - on specific application service or some operations</li>
	 * <li>global default config</li>
	 * </ul>
	 * Descending priority from top to bottom.
	 * <p/>
	 * @param id operation or method name
	 * @param groupName service group name
	 * @return {@link Resilience4JCircuitBreaker}
	 */
	private Resilience4JCircuitBreaker create(String id, String groupName,
			ExecutorService circuitBreakerExecutorService) {
		Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration defaultConfig = getConfigurations()
			.computeIfAbsent(id, defaultConfiguration);
		CircuitBreakerConfig circuitBreakerConfig = this.circuitBreakerRegistry.getConfiguration(id)
			.orElseGet(() -> this.circuitBreakerRegistry.getConfiguration(groupName)
				.orElseGet(defaultConfig::getCircuitBreakerConfig));
		TimeLimiterConfig timeLimiterConfig = this.timeLimiterRegistry.getConfiguration(id)
			.orElseGet(() -> this.timeLimiterRegistry.getConfiguration(groupName)
				.orElseGet(defaultConfig::getTimeLimiterConfig));
		if (resilience4JConfigurationProperties.isDisableThreadPool()) {
			return new Resilience4JCircuitBreaker(id, groupName, circuitBreakerConfig, timeLimiterConfig,
					circuitBreakerRegistry, timeLimiterRegistry, Optional.ofNullable(circuitBreakerCustomizers.get(id)),
					bulkheadProvider);
		}
		else {
			return new Resilience4JCircuitBreaker(id, groupName, circuitBreakerConfig, timeLimiterConfig,
					circuitBreakerRegistry, timeLimiterRegistry, circuitBreakerExecutorService,
					Optional.ofNullable(circuitBreakerCustomizers.get(id)), bulkheadProvider,
					this.resilience4JConfigurationProperties.isDisableTimeLimiter());
		}

	}

	public void setObservationRegistry(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

}
