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

package org.springframework.cloud.circuitbreaker.springretry;

import java.time.Duration;

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
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
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
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = SpringRetryCircuitBreakerIntegrationTest.Application.class)
@DirtiesContext
public class SpringRetryCircuitBreakerIntegrationTest {

	@Autowired
	Application.DemoControllerService service;

	@Test
	public void testSlow() {
		assertThat(service.slow()).isEqualTo("fallback");
		service.verifyTimesSlowInvoked();
	}

	@Test
	public void testNormal() {
		assertThat(service.normal()).isEqualTo("normal");
	}

	@Configuration(proxyBeanMethods = false)
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
		public Customizer<SpringRetryCircuitBreakerFactory> factoryCustomizer() {
			return factory -> {
				factory.configureDefault(
						id -> new SpringRetryConfigBuilder(id).retryPolicy(new TimeoutRetryPolicy()).build());
				factory.configure(builder -> builder.retryPolicy(new SimpleRetryPolicy(1)).build(), "slow");
				factory.addRetryTemplateCustomizers(
						retryTemplate -> retryTemplate.registerListener(new RetryListener() {

							@Override
							public <T, E extends Throwable> boolean open(RetryContext context,
									RetryCallback<T, E> callback) {
								return false;
							}

							@Override
							public <T, E extends Throwable> void close(RetryContext context,
									RetryCallback<T, E> callback, Throwable throwable) {

							}

							@Override
							public <T, E extends Throwable> void onError(RetryContext context,
									RetryCallback<T, E> callback, Throwable throwable) {

							}
						}));
			};
		}

		@Service
		public static class DemoControllerService {

			private TestRestTemplate rest;

			private CircuitBreakerFactory cbFactory;

			private CircuitBreaker circuitBreakerSlow;

			DemoControllerService(TestRestTemplate rest, CircuitBreakerFactory cbFactory) {
				this.rest = spy(rest);
				this.cbFactory = cbFactory;
				this.circuitBreakerSlow = cbFactory.create("slow");
			}

			public String slow() {
				for (int i = 0; i < 10; i++) {
					circuitBreakerSlow.run(() -> rest.getForObject("/slow", String.class), t -> "fallback");
				}
				return circuitBreakerSlow.run(() -> rest.getForObject("/slow", String.class), t -> "fallback");
			}

			public String normal() {
				return cbFactory.create("normal").run(() -> rest.getForObject("/normal", String.class),
						t -> "fallback");
			}

			public void verifyTimesSlowInvoked() {
				verify(rest, times(1)).getForObject(eq("/slow"), eq(String.class));
			}

		}

	}

}
