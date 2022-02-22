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

package org.springframework.cloud.circuitbreaker.springretry;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

/**
 * @author wind57
 */
class SpringRetryAutoConfigurationTests {

	@Nested
	class NoCustomizers {

		private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(SpringRetryAutoConfiguration.class));

		@Test
		void testNoCustomizers() {

			contextRunner.run(context -> {
				String[] arr = context
						.getBeanNamesForType(ResolvableType.forClassWithGenerics(List.class, Customizer.class));
				Assertions.assertEquals(0, arr.length, "Default auto-configuration must not have any customizers");

				@SuppressWarnings("rawtypes")
				Map<String, CircuitBreakerFactory> map = context.getBeansOfType(CircuitBreakerFactory.class);
				Assertions.assertEquals(1, map.size(), "Spring Retry Circuit Breaker must be present");
			});

		}

	}

	@Nested
	class WithEmptyCustomizers {

		private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(SpringRetryAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(ListWithEmptyCustomizers.class));

		@Test
		void testWithCustomizers() {

			contextRunner.run(context -> {
				String[] arr = context
						.getBeanNamesForType(ResolvableType.forClassWithGenerics(List.class, Customizer.class));
				Assertions.assertEquals(1, arr.length);
				Assertions.assertArrayEquals(arr, new String[] { "customizers" });

				@SuppressWarnings("rawtypes")
				Map<String, CircuitBreakerFactory> map = context.getBeansOfType(CircuitBreakerFactory.class);
				Assertions.assertEquals(1, map.size());
			});

		}

		@Configuration
		static class ListWithEmptyCustomizers {

			@Bean
			List<Customizer<SpringRetryCircuitBreakerFactory>> customizers() {
				return Collections.emptyList();
			}

		}

	}

	@Nested
	@ExtendWith(OutputCaptureExtension.class)
	class WithCustomizers {

		private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(SpringRetryAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(WithCustomizers.MyConfig.class));

		@Test
		void testCustomizer(CapturedOutput output) {

			contextRunner.run(context -> {
				String[] arr = context
						.getBeanNamesForType(ResolvableType.forClassWithGenerics(List.class, Customizer.class));
				Assertions.assertEquals(1, arr.length);
				Assertions.assertArrayEquals(arr, new String[] { "customizers" });

				@SuppressWarnings("rawtypes")
				Map<String, CircuitBreakerFactory> map = context.getBeansOfType(CircuitBreakerFactory.class);
				Assertions.assertEquals(1, map.size());
			});
			Assertions.assertTrue(output.getOut().contains("trying to customize"));

		}

		@Configuration
		static class MyConfig {

			@Bean
			List<Customizer<SpringRetryCircuitBreakerFactory>> customizers() {
				return Collections.singletonList(factory -> {
					System.out.println("trying to customize");
				});
			}

		}

	}

}
