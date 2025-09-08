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

package org.springframework.cloud.circuitbreaker.resilience4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * @author Yavor Chamov
 */
public class ReactiveResilience4jBulkheadProvider {

	private final BulkheadRegistry bulkheadRegistry;

	private final ConcurrentHashMap<String, Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration> configurations = new ConcurrentHashMap<>();

	private Function<String, Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration> defaultConfiguration;

	public ReactiveResilience4jBulkheadProvider(BulkheadRegistry bulkheadRegistry) {
		this.bulkheadRegistry = bulkheadRegistry;
		this.defaultConfiguration = id -> new Resilience4jBulkheadConfigurationBuilder()
			.bulkheadConfig(this.bulkheadRegistry.getDefaultConfig())
			.build();
	}

	public void configureDefault(
			@NonNull Function<String, Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration> defaultConfiguration) {
		Assert.notNull(defaultConfiguration, "Default configuration must not be null");
		this.defaultConfiguration = defaultConfiguration;
	}

	public void configure(Consumer<Resilience4jBulkheadConfigurationBuilder> consumer, String... ids) {
		for (String id : ids) {
			Resilience4jBulkheadConfigurationBuilder builder = new Resilience4jBulkheadConfigurationBuilder();
			consumer.accept(builder);
			Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration configuration = builder.build();
			configurations.put(id, configuration);
		}
	}

	public void addBulkheadCustomizer(Consumer<Bulkhead> customizer, String... ids) {
		for (String id : ids) {
			Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration configuration = configurations
				.computeIfAbsent(id, defaultConfiguration);
			Bulkhead bulkhead = bulkheadRegistry.bulkhead(id, configuration.getBulkheadConfig());
			customizer.accept(bulkhead);
		}
	}

	public BulkheadRegistry getBulkheadRegistry() {
		return bulkheadRegistry;
	}

	public <T> Mono<T> decorateMono(String id, Map<String, String> tags, Mono<T> mono) {
		Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration configuration = configurations
			.computeIfAbsent(id, this::getConfiguration);
		Bulkhead bulkhead = bulkheadRegistry.bulkhead(id, configuration.getBulkheadConfig(), tags);
		return mono.transformDeferred(BulkheadOperator.of(bulkhead));
	}

	public <T> Flux<T> decorateFlux(String id, Map<String, String> tags, Flux<T> flux) {
		Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration configuration = configurations
			.computeIfAbsent(id, this::getConfiguration);
		Bulkhead bulkhead = bulkheadRegistry.bulkhead(id, configuration.getBulkheadConfig(), tags);
		return flux.transformDeferred(BulkheadOperator.of(bulkhead));
	}

	private Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration getConfiguration(String id) {
		Resilience4jBulkheadConfigurationBuilder builder = new Resilience4jBulkheadConfigurationBuilder();
		Resilience4jBulkheadConfigurationBuilder.BulkheadConfiguration defaultConfiguration = this.defaultConfiguration
			.apply(id);
		Optional<BulkheadConfig> bulkheadConfiguration = bulkheadRegistry.getConfiguration(id);
		builder.bulkheadConfig(bulkheadConfiguration.orElse(defaultConfiguration.getBulkheadConfig()));
		return builder.build();
	}

}
