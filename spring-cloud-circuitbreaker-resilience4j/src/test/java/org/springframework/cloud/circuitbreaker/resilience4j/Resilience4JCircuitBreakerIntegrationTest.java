/*
 * Copyright 2013-2018 the original author or authors.
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
import java.util.List;
import java.util.Map;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = Resilience4JCircuitBreakerIntegrationTest.Application.class,
		properties = { "management.endpoints.web.exposure.include=*",
				"spring.cloud.circuitbreaker.bulkhead.resilience4j.enabled=false" })
@DirtiesContext
public class Resilience4JCircuitBreakerIntegrationTest {

	@Mock
	static EventConsumer<CircuitBreakerOnErrorEvent> slowErrorConsumer;

	@Mock
	static EventConsumer<CircuitBreakerOnSuccessEvent> slowSuccessConsumer;

	@Mock
	static EventConsumer<CircuitBreakerOnErrorEvent> normalErrorConsumer;

	@Mock
	static EventConsumer<CircuitBreakerOnSuccessEvent> normalSuccessConsumer;

	@Autowired
	Application.DemoControllerService service;

	@Autowired
	private TestRestTemplate rest;

	@Test
	public void testSlow() {
		assertThat(service.slow()).isEqualTo("fallback");
		verify(slowErrorConsumer, times(1)).consumeEvent(any());
		verify(slowSuccessConsumer, times(0)).consumeEvent(any());
	}

	@Test
	public void testNormal() {
		assertThat(service.normal()).isEqualTo("normal");
		verify(normalErrorConsumer, times(0)).consumeEvent(any());
		verify(normalSuccessConsumer, times(1)).consumeEvent(any());
	}

	@Test
	public void testSlowResponsesDontFailSubsequentGoodRequests() {
		assertThat(service.slowOnDemand(5000)).isEqualTo("fallback");
		assertThat(service.slowOnDemand(0)).isEqualTo("normal");
	}

	@Test
	public void testResilience4JMetricsAvailable() {
		assertThat(service.normal()).isEqualTo("normal");
		assertThat(((List) rest.getForObject("/actuator/metrics", Map.class).get("names"))
				.contains("resilience4j.circuitbreaker.calls")).isTrue();
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

		@GetMapping("/slowOnDemand")
		public String slowOnDemand(@RequestHeader HttpHeaders headers) {
			if (headers.containsKey("delayInMilliseconds")) {
				String delayString = headers.getFirst("delayInMilliseconds");
				if (delayString != null) {
					try {
						Thread.sleep(Integer.parseInt(delayString));
					}
					catch (NumberFormatException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			return "normal";
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
				factory.addCircuitBreakerCustomizer(circuitBreaker -> circuitBreaker.getEventPublisher()
						.onError(normalErrorConsumer).onSuccess(normalSuccessConsumer), "normal");
			};
		}

		@Service
		public static class DemoControllerService {

			private TestRestTemplate rest;

			private final CircuitBreakerFactory cbFactory;

			private final CircuitBreaker circuitBreakerSlow;

			DemoControllerService(TestRestTemplate rest, CircuitBreakerFactory cbFactory) {
				this.rest = rest;
				this.cbFactory = cbFactory;
				this.circuitBreakerSlow = cbFactory.create("slow");
			}

			public String slow() {
				return circuitBreakerSlow.run(() -> rest.getForObject("/slow", String.class), t -> "fallback");
			}

			public String normal() {
				return cbFactory.create("normal").run(() -> rest.getForObject("/normal", String.class),
						t -> "fallback");
			}

			public String slowOnDemand(int delayInMilliseconds) {
				return circuitBreakerSlow
						.run(() -> rest
								.exchange("/slowOnDemand", HttpMethod.GET,
										createEntityWithOptionalDelayHeader(delayInMilliseconds), String.class)
								.getBody(), t -> "fallback");
			}

			private HttpEntity<String> createEntityWithOptionalDelayHeader(int delayInMilliseconds) {
				HttpHeaders headers = new HttpHeaders();
				if (delayInMilliseconds > 0) {
					headers.set("delayInMilliseconds", Integer.toString(delayInMilliseconds));
				}
				return new HttpEntity<>(null, headers);
			}

		}

	}

}
