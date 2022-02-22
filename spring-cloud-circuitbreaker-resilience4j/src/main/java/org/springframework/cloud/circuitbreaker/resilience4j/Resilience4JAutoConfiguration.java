/*
 * Copyright 2013-2022 the original author or authors.
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
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetricsPublisher;
import io.github.resilience4j.micrometer.tagged.TaggedThreadPoolBulkheadMetrics;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(Resilience4JConfigurationProperties.class)
@ConditionalOnProperty(name = { "spring.cloud.circuitbreaker.resilience4j.enabled",
		"spring.cloud.circuitbreaker.resilience4j.blocking.enabled" }, matchIfMissing = true)
public class Resilience4JAutoConfiguration {

	@Autowired(required = false)
	private List<Customizer<Resilience4JCircuitBreakerFactory>> customizers = new ArrayList<>();

	@Bean
	@ConditionalOnMissingBean(CircuitBreakerFactory.class)
	public Resilience4JCircuitBreakerFactory resilience4jCircuitBreakerFactory(
			CircuitBreakerRegistry circuitBreakerRegistry, TimeLimiterRegistry timeLimiterRegistry,
			@Autowired(required = false) Resilience4jBulkheadProvider bulkheadProvider) {
		Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(circuitBreakerRegistry,
				timeLimiterRegistry, bulkheadProvider);
		customizers.forEach(customizer -> customizer.customize(factory));
		return factory;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Bulkhead.class)
	@ConditionalOnProperty(value = "spring.cloud.circuitbreaker.bulkhead.resilience4j.enabled", matchIfMissing = true)
	public static class Resilience4jBulkheadConfiguration {

		@Autowired(required = false)
		private List<Customizer<Resilience4jBulkheadProvider>> bulkheadCustomizers = new ArrayList<>();

		@Bean
		public Resilience4jBulkheadProvider bulkheadProvider(ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
				BulkheadRegistry bulkheadRegistry,
				Resilience4JConfigurationProperties resilience4JConfigurationProperties) {
			Resilience4jBulkheadProvider resilience4jBulkheadProvider = new Resilience4jBulkheadProvider(
					threadPoolBulkheadRegistry, bulkheadRegistry, resilience4JConfigurationProperties);
			bulkheadCustomizers.forEach(customizer -> customizer.customize(resilience4jBulkheadProvider));
			return resilience4jBulkheadProvider;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean({ MeterRegistry.class })
	public static class MicrometerResilience4JGroupCustomizerConfiguration {

		private static final String RESILIENCE4J_METER_PREFIX = "resilience4j";

		@Bean
		@ConditionalOnProperty(value = "spring.cloud.circuitbreaker.resilience4j.enableGroupMeterFilter",
				havingValue = "true", matchIfMissing = true)
		MeterFilter resilience4JMeterFilter(Resilience4JConfigurationProperties properties) {
			return new MeterFilter() {
				@Override
				public Meter.Id map(Meter.Id id) {
					if (id.getName().startsWith(RESILIENCE4J_METER_PREFIX)
							&& id.getTag(Resilience4JCircuitBreaker.CIRCUIT_BREAKER_GROUP_TAG) == null) {
						return id.withTag(Tag.of(Resilience4JCircuitBreaker.CIRCUIT_BREAKER_GROUP_TAG,
								properties.getDefaultGroupTag()));
					}
					return id;
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean({ MeterRegistry.class })
	@ConditionalOnClass(name = { "io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics",
			"io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetricsPublisher" })
	@ConditionalOnMissingBean({ TaggedCircuitBreakerMetricsPublisher.class })
	public static class MicrometerResilience4JCustomizerConfiguration {

		@Autowired(required = false)
		private Resilience4JCircuitBreakerFactory factory;

		@Autowired(required = false)
		private Resilience4jBulkheadProvider bulkheadProvider;

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
				TaggedThreadPoolBulkheadMetrics
						.ofThreadPoolBulkheadRegistry(bulkheadProvider.getThreadPoolBulkheadRegistry())
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
