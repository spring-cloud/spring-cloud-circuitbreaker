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

import java.util.ArrayList;
import java.util.List;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetricsPublisher;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ryan Baxter
 * @author Eric Bussieres
 * @author Thomas Vitale
 * @author Yavor Chamov
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = { "reactor.core.publisher.Mono", "reactor.core.publisher.Flux",
		"io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator" })
@ConditionalOnProperty(name = { "spring.cloud.circuitbreaker.resilience4j.enabled",
		"spring.cloud.circuitbreaker.resilience4j.reactive.enabled" }, matchIfMissing = true)
@EnableConfigurationProperties(Resilience4JConfigurationProperties.class)
public class ReactiveResilience4JAutoConfiguration {

	@Autowired(required = false)
	private List<Customizer<ReactiveResilience4JCircuitBreakerFactory>> customizers = new ArrayList<>();

	@Bean
	@ConditionalOnMissingBean(ReactiveCircuitBreakerFactory.class)
	public ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory(
			CircuitBreakerRegistry circuitBreakerRegistry, TimeLimiterRegistry timeLimiterRegistry,
			@Autowired(required = false) ReactiveResilience4jBulkheadProvider bulkheadProvider,
			Resilience4JConfigurationProperties resilience4JConfigurationProperties) {
		ReactiveResilience4JCircuitBreakerFactory factory = new ReactiveResilience4JCircuitBreakerFactory(
				circuitBreakerRegistry, timeLimiterRegistry, bulkheadProvider, resilience4JConfigurationProperties);
		customizers.forEach(customizer -> customizer.customize(factory));
		return factory;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Bulkhead.class)
	@ConditionalOnProperty(value = "spring.cloud.circuitbreaker.bulkhead.resilience4j.enabled", matchIfMissing = true)
	public static class Resilience4jBulkheadConfiguration {

		@Autowired(required = false)
		private List<Customizer<ReactiveResilience4jBulkheadProvider>> bulkheadCustomizers = new ArrayList<>();

		@Value("${spring.cloud.circuitbreaker.resilience4j.enableSemaphoreDefaultBulkhead:true}")
		private boolean enableSemaphoreDefaultBulkhead;

		@Bean
		public ReactiveResilience4jBulkheadProvider reactiveBulkheadProvider(BulkheadRegistry bulkheadRegistry) {

			if (!enableSemaphoreDefaultBulkhead) {
				LoggerFactory.getLogger(Resilience4jBulkheadConfiguration.class)
					.warn("Ignoring 'spring.cloud.circuitbreaker.resilience4j.enableSemaphoreDefaultBulkhead=false'. "
							+ "ReactiveResilience4jBulkheadProvider only supports SemaphoreBulkhead.");
			}

			ReactiveResilience4jBulkheadProvider reactiveResilience4JCircuitBreaker = new ReactiveResilience4jBulkheadProvider(
					bulkheadRegistry);
			bulkheadCustomizers.forEach(customizer -> customizer.customize(reactiveResilience4JCircuitBreaker));
			return reactiveResilience4JCircuitBreaker;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = { "reactor.core.publisher.Mono", "reactor.core.publisher.Flux",
			"io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics",
			"io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetricsPublisher" })
	@ConditionalOnBean({ MeterRegistry.class })
	@ConditionalOnMissingBean({ TaggedCircuitBreakerMetricsPublisher.class })
	public static class MicrometerReactiveResilience4JCustomizerConfiguration {

		@Autowired(required = false)
		private ReactiveResilience4JCircuitBreakerFactory factory;

		@Autowired(required = false)
		private ReactiveResilience4jBulkheadProvider bulkheadProvider;

		@Autowired(required = false)
		private TaggedCircuitBreakerMetrics taggedCircuitBreakerMetrics;

		@Autowired
		private MeterRegistry meterRegistry;

		@PostConstruct
		public void init() {
			if (factory != null) {
				if (taggedCircuitBreakerMetrics == null) {
					taggedCircuitBreakerMetrics = TaggedCircuitBreakerMetrics
						.ofCircuitBreakerRegistry(factory.getCircuitBreakerRegistry());
				}
				taggedCircuitBreakerMetrics.bindTo(meterRegistry);
			}
			if (bulkheadProvider != null) {
				TaggedBulkheadMetrics.ofBulkheadRegistry(bulkheadProvider.getBulkheadRegistry()).bindTo(meterRegistry);
			}
		}

	}

}
