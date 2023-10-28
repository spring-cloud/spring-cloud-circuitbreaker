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

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
public class Resilience4JAutoConfigurationMetricsConfigRun {

	static ReactiveResilience4JCircuitBreakerFactory reactiveCircuitBreakerFactory = spy(
			new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
					TimeLimiterRegistry.ofDefaults(), new Resilience4JConfigurationProperties()));

	static Resilience4JCircuitBreakerFactory circuitBreakerFactory = spy(
			new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(),
					mock(Resilience4jBulkheadProvider.class), new Resilience4JConfigurationProperties()));

	@Test
	public void testWithMetricsConfigReactive() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties("resilience4j.circuitbreaker.metrics.legacy.enabled").sources(TestAppReactive.class)
				.run()) {
			verify(reactiveCircuitBreakerFactory, times(1)).getCircuitBreakerRegistry();
		}
	}

	@Test
	public void testWithMetricsConfig() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.properties("resilience4j.circuitbreaker.metrics.legacy.enabled").sources(TestApp.class).run()) {
			verify(circuitBreakerFactory, times(1)).getCircuitBreakerRegistry();
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	protected static class TestAppReactive {

		@Bean
		ReactiveResilience4JCircuitBreakerFactory reativeCircuitBreakerFactory() {
			return reactiveCircuitBreakerFactory;
		}

		@Bean
		Resilience4JCircuitBreakerFactory circuitBreakerFactory() {
			return circuitBreakerFactory;
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	protected static class TestApp {

		@Bean
		Resilience4JCircuitBreakerFactory circuitBreakerFactory() {
			return circuitBreakerFactory;
		}

	}

}
