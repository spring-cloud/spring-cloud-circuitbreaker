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

package org.springframework.cloud.circuitbreaker.resilience4j;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ryan Baxter
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "micrometer-core-*.jar", "resilience4j-micrometer-*.jar" })
public class ReactiveResilience4JAutoConfigurationWithoutMetricsTest {

	static ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory = spy(
			new ReactiveResilience4JCircuitBreakerFactory());

	@Test
	public void testWithoutMetrics() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(TestApp.class).run()) {
			verify(circuitBreakerFactory, times(0)).getCircuitBreakerRegistry();
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	protected static class TestApp {

		@Bean
		ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory() {
			return circuitBreakerFactory;
		}

	}

}
