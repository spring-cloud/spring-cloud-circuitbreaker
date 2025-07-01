/*
 * Copyright 2013-2025 the original author or authors.
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

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.test.client.TestRestTemplate;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.policy.CircuitBreakerRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 */
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = SpringRetryCircuitBreakerIntegrationTest.Application.class)
class SpringRetryCircuitBreakerIntegrationTest {

	@Autowired
	private Application.DemoControllerService service;

	@Test
	void testSlow() {
		assertThat(service.slow()).isEqualTo("fallback");
		service.verifyTimesSlowInvoked();
	}

	@Test
	void testNormal() {
		assertThat(service.normal()).isEqualTo("normal");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		@GetMapping("/slow")
		String slow() throws InterruptedException {
			Thread.sleep(3000);
			return "slow";
		}

		@GetMapping("/normal")
		String normal() {
			return "normal";
		}

		@Bean
		RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder().readTimeout(Duration.ofSeconds(1));
		}

		@Bean
		Customizer<SpringRetryCircuitBreakerFactory> factoryCustomizer() {
			return factory -> {
				CircuitBreakerRetryPolicy circuitBreakerRetryPolicy = new CircuitBreakerRetryPolicy(
						new SimpleRetryPolicy(2));
				circuitBreakerRetryPolicy.setOpenTimeout(1000);
				circuitBreakerRetryPolicy.setResetTimeout(1000);
				factory.configureDefault(
						id -> new SpringRetryConfigBuilder(id).retryPolicy(circuitBreakerRetryPolicy).build());
				factory.configure(builder -> builder.retryPolicy(new SimpleRetryPolicy(1)).build(), "slow");
				factory
					.addRetryTemplateCustomizers(retryTemplate -> retryTemplate.registerListener(new RetryListener() {

						@Override
						public <T, E extends Throwable> boolean open(RetryContext context,
								RetryCallback<T, E> callback) {
							return false;
						}

						@Override
						public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
								Throwable throwable) {

						}

						@Override
						public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
								Throwable throwable) {

						}
					}));
			};
		}

		@Service
		private static class DemoControllerService {

			private final TestRestTemplate rest;

			private final CircuitBreakerFactory<?, ?> cbFactory;

			private final CircuitBreaker circuitBreakerSlow;

			DemoControllerService(TestRestTemplate rest, CircuitBreakerFactory<?, ?> cbFactory) {
				this.rest = spy(rest);
				this.cbFactory = cbFactory;
				this.circuitBreakerSlow = cbFactory.create("slow");
			}

			String slow() {
				for (int i = 0; i < 10; i++) {
					circuitBreakerSlow.run(() -> rest.getForObject("/slow", String.class), t -> "fallback");
				}
				return circuitBreakerSlow.run(() -> rest.getForObject("/slow", String.class), t -> "fallback");
			}

			String normal() {
				return cbFactory.create("normal")
					.run(() -> rest.getForObject("/normal", String.class), t -> "fallback");
			}

			void verifyTimesSlowInvoked() {
				verify(rest, times(1)).getForObject(eq("/slow"), eq(String.class));
			}

		}

	}

}
