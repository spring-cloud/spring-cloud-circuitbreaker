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

import java.time.Duration;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Robert McNees
 */
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = Resilience4JDefaultSemaphoreBulkheadIntegrationTest.Application.class,
		properties = {"management.endpoints.web.exposure.include=*",
				"spring.cloud.circuitbreaker.resilience4j.enableSemaphoreDefaultBulkhead=true"})
@DirtiesContext
public class Resilience4JDefaultSemaphoreBulkheadIntegrationTest {

	@Mock
	static EventConsumer<CircuitBreakerOnErrorEvent> slowErrorConsumer;

	@Mock
	static EventConsumer<CircuitBreakerOnSuccessEvent> slowSuccessConsumer;

	@Autowired
	Resilience4JDefaultSemaphoreBulkheadIntegrationTest.Application.DemoControllerService service;

	@Autowired
	Resilience4JConfigurationProperties configurationProperties;

	@Autowired
	private TestRestTemplate rest;

	@SpyBean
	private ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;

	@SpyBean
	private BulkheadRegistry bulkheadRegistry;

	@Test
	public void testSlowUsesSemaphoreBulkheadDefault() {

		assertThat(service.slow()).isEqualTo("fallback");
		verify(slowErrorConsumer, times(1)).consumeEvent(any());
		verify(slowSuccessConsumer, times(0)).consumeEvent(any());

		verify(this.threadPoolBulkheadRegistry, times(0)).bulkhead(any(String.class),
				any(ThreadPoolBulkheadConfig.class), any(io.vavr.collection.Map.class));

		verify(this.bulkheadRegistry, times(1)).bulkhead(any(String.class), any(BulkheadConfig.class),
				any(io.vavr.collection.Map.class));
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

		@Bean
		public Customizer<Resilience4JCircuitBreakerFactory> slowCustomizer() {
			return factory -> {
				factory.configure(builder -> builder.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
								.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(2)).build()),
						"slow");
				factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
						.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(4)).build())
						.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults()).build());
				factory.addCircuitBreakerCustomizer(circuitBreaker -> circuitBreaker.getEventPublisher()
						.onError(slowErrorConsumer).onSuccess(slowSuccessConsumer), "slow");
			};
		}

		@Service
		public static class DemoControllerService {

			private final CircuitBreakerFactory cbFactory;
			private final CircuitBreaker circuitBreakerSlow;
			private final TestRestTemplate rest;

			DemoControllerService(TestRestTemplate rest, CircuitBreakerFactory cbFactory) {
				this.rest = rest;
				this.cbFactory = cbFactory;
				this.circuitBreakerSlow = cbFactory.create("slow");
			}

			public String slow() {
				return circuitBreakerSlow.run(() -> rest.getForObject("/slow", String.class), t -> "fallback");
			}

		}

	}
}
