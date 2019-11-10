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

package org.springframework.cloud.circuitbreaker.failsafe;

import java.time.Duration;

import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedConsumer;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Jakub Marchwicki
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = FailsafeCircuitBreakerIntegrationTest.Application.class)
@DirtiesContext
public class FailsafeCircuitBreakerIntegrationTest {

	@Autowired
	Application.DemoControllerService service;

	@Autowired
	CheckedConsumer onFailureConsumer;

	@Test
	public void testSlow() throws Exception {
		assertThat(service.slow()).isEqualTo("fallback");
		service.verifyTimesSlowInvoked();
		verify(onFailureConsumer, times(11)).accept(any());
	}

	@Test
	public void testNormal() {
		assertThat(service.normal()).isEqualTo("normal");
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		@GetMapping("/slow")
		public String slow() throws InterruptedException {
			Thread.sleep(3000);
			return "slow";
		}

		@GetMapping("/normal")
		public String normal() {
			return "normal";
		}

		@Bean
		public RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder().setReadTimeout(Duration.ofSeconds(1));
		}

		@Bean
		public CheckedConsumer onFailureConsumer() {
			return mock(CheckedConsumer.class);
		}

		@Bean
		public Customizer<FailsafeCircuitBreakerFactory> factoryCustomizer(
				CheckedConsumer onFailureConsumer) {
			return factory -> {
				factory.configureDefault(id -> new FailsafeConfigBuilder(id).build());
				factory.configure(
						builder -> builder
								.retryPolicy(new RetryPolicy<>().withMaxAttempts(1))
								.circuitBreaker(new net.jodah.failsafe.CircuitBreaker<>()
										.handle(Exception.class).withFailureThreshold(1)
										.withDelay(Duration.ofMinutes(1)))
								.build(),
						"slow");
				factory.addFailsafeCustomizers(
						failsafe -> failsafe.onFailure(onFailureConsumer), "slow");
			};
		}

		@Service
		public static class DemoControllerService {

			private TestRestTemplate rest;

			private CircuitBreakerFactory cbFactory;

			DemoControllerService(TestRestTemplate rest,
					CircuitBreakerFactory cbFactory) {
				this.rest = spy(rest);
				this.cbFactory = cbFactory;
			}

			String slow() {
				CircuitBreaker cb = cbFactory.create("slow");
				for (int i = 0; i < 10; i++) {
					cb.run(() -> rest.getForObject("/slow", String.class),
							t -> "fallback");
				}
				return cb.run(() -> rest.getForObject("/slow", String.class),
						t -> "fallback");
			}

			String normal() {
				return cbFactory.create("normal").run(
						() -> rest.getForObject("/normal", String.class),
						t -> "fallback");
			}

			void verifyTimesSlowInvoked() {
				verify(rest, times(1)).getForObject(eq("/slow"), eq(String.class));
			}

		}

	}

}
