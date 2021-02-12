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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedThreadPoolBulkheadMetrics;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ryan Baxter
 * @author Eric Bussieres
 * @author Andrii Bohutskyi
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = { "spring.cloud.circuitbreaker.resilience4j.enabled",
		"spring.cloud.circuitbreaker.resilience4j.blocking.enabled" }, matchIfMissing = true)
public class Resilience4JAutoConfiguration {

	@Autowired(required = false)
	private List<Customizer<Resilience4JCircuitBreakerFactory>> customizers = new ArrayList<>();

	@Bean
	@ConditionalOnMissingBean(CircuitBreakerFactory.class)
	public Resilience4JCircuitBreakerFactory resilience4jCircuitBreakerFactory(
			@Autowired(required = false) Resilience4jBulkheadProvider bulkheadProvider) {
		Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(bulkheadProvider);
		customizers.forEach(customizer -> customizer.customize(factory));
		return factory;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Bulkhead.class, Decorators.class })
	@ConditionalOnProperty("spring.cloud.bulkhead.resilience4j.enable")
	public static class Resilience4jBulkheadConfiguration {

		@Autowired(required = false)
		private List<Customizer<Resilience4jBulkheadProvider>> bulkheadCustomizers = new ArrayList<>();

		@Bean
		public Resilience4jBulkheadProvider bulkheadProvider() {
			Resilience4jBulkheadProvider resilience4jBulkheadProvider = new Resilience4jBulkheadProvider();
			bulkheadCustomizers.forEach(customizer -> customizer.customize(resilience4jBulkheadProvider));
			return resilience4jBulkheadProvider;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean({ MeterRegistry.class })
	@ConditionalOnClass(name = { "io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics" })
	public static class MicrometerResilience4JCustomizerConfiguration {

		@Autowired(required = false)
		private Resilience4JCircuitBreakerFactory factory;

		@Autowired(required = false)
		private Resilience4jBulkheadProvider bulkheadProvider;

		@Autowired
		private MeterRegistry meterRegistry;

		@PostConstruct
		public void init() {
			if (factory != null) {
				TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(factory.getCircuitBreakerRegistry())
						.bindTo(meterRegistry);
			}
			if (bulkheadProvider != null) {
				TaggedBulkheadMetrics.ofBulkheadRegistry(bulkheadProvider.getBulkheadRegistry()).bindTo(meterRegistry);
				TaggedThreadPoolBulkheadMetrics
						.ofThreadPoolBulkheadRegistry(bulkheadProvider.getThreadPoolBulkheadRegistry())
						.bindTo(meterRegistry);
			}
		}

	}

}
