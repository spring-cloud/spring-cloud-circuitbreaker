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

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.Test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

/**
 * @author Ryan Baxter
 */
public class Resilience4JAutoConfigurationTests {

	static Resilience4JCircuitBreakerFactory circuitBreakerFactory = spy(
			new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(),
					null, new Resilience4JConfigurationProperties()));

	@Test
	public void meterFilterEnabled() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
			.sources(Resilience4JAutoConfigurationTests.TestApp.class)
			.run()) {
			assertThat(context.getBean("resilience4JMeterFilter")).isNotNull();
		}
	}

	@Test
	public void meterFilterDisabled() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
			.sources(Resilience4JAutoConfigurationTests.TestApp.class)
			.properties("spring.cloud.circuitbreaker.resilience4j.enableGroupMeterFilter=false")
			.run()) {
			assertThat(context.containsBean("resilience4JMeterFilter")).isFalse();
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
