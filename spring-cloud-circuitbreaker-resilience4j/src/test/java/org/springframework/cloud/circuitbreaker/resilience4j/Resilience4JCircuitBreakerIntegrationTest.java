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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.ObservationContextAssert;
import org.assertj.core.api.BDDAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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
				"spring.cloud.circuitbreaker.bulkhead.resilience4j.enabled=false",
				"resilience4j.timelimiter.metrics.legacy.enabled=true" })
@DirtiesContext
@AutoConfigureTestRestTemplate
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
	Application.MyObservationHandler myObservationHandler;

	@Autowired
	private TestRestTemplate rest;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		myObservationHandler.contexts.clear();
	}

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

		// CircuitBreaker and TimeLimiter should have 3 metrics: name, kind, group
		assertThat(((List) rest.getForObject("/actuator/metrics/resilience4j.circuitbreaker.calls", Map.class)
			.get("availableTags"))).hasSize(3);
		assertThat(((List) rest.getForObject("/actuator/metrics/resilience4j.timelimiter.calls", Map.class)
			.get("availableTags"))).hasSize(3);
	}

	@Test
	public void testObservationRegistry() throws InterruptedException {
		// :( some other test from this class is putting an additional context after some
		// time
		Thread.sleep(100);
		myObservationHandler.contexts.clear();

		assertThat(service.withObservationRegistry()).isEqualTo("fallback");

		// CircuitBreaker should have 3 observations: my.observation, for supplier and for
		// function
		// TODO: Convert to usage of test registry assert with the next micrometer release
		List<Observation.Context> contexts = myObservationHandler.contexts;
		assertThat(contexts).hasSize(3);
		assertThat(contexts.get(0)).satisfies(context -> ObservationContextAssert.then(context)
			.hasNameEqualTo("spring.cloud.circuitbreaker")
			.hasContextualNameEqualTo("circuit-breaker")
			.hasLowCardinalityKeyValue("spring.cloud.circuitbreaker.type", "supplier"));
		BDDAssertions.then(contexts.get(1))
			.satisfies(context -> ObservationContextAssert.then(context)
				.hasNameEqualTo("spring.cloud.circuitbreaker")
				.hasContextualNameEqualTo("circuit-breaker fallback")
				.hasLowCardinalityKeyValue("spring.cloud.circuitbreaker.type", "function"));
		BDDAssertions.then(contexts.get(2))
			.satisfies(context -> ObservationContextAssert.then(context).hasNameEqualTo("my.observation"));
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

		@GetMapping("/exception")
		public String exception() {
			throw new IllegalStateException("BOOM!");
		}

		@GetMapping("/normal")
		public String normal() {
			return "normal";
		}

		@GetMapping("/slowOnDemand")
		public String slowOnDemand(@RequestHeader HttpHeaders headers) {
			if (headers.containsHeader("delayInMilliseconds")) {
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
					.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
					.build());
				factory.addCircuitBreakerCustomizer(circuitBreaker -> circuitBreaker.getEventPublisher()
					.onError(slowErrorConsumer)
					.onSuccess(slowSuccessConsumer), "slow");
				factory.addCircuitBreakerCustomizer(circuitBreaker -> circuitBreaker.getEventPublisher()
					.onError(normalErrorConsumer)
					.onSuccess(normalSuccessConsumer), "normal");
			};
		}

		@Component
		public static class MyObservationHandler implements ObservationHandler<Observation.Context> {

			List<Observation.Context> contexts = new ArrayList<>();

			@Override
			public void onStop(Observation.Context context) {
				this.contexts.add(context);
			}

			@Override
			public boolean supportsContext(Observation.Context context) {
				return true;
			}

		}

		@Service
		public static class DemoControllerService {

			private TestRestTemplate rest;

			private final CircuitBreakerFactory cbFactory;

			private final CircuitBreaker circuitBreakerSlow;

			private final ObservationRegistry observationRegistry;

			DemoControllerService(TestRestTemplate rest, CircuitBreakerFactory cbFactory,
					ObservationRegistry observationRegistry) {
				this.rest = rest;
				this.cbFactory = cbFactory;
				this.circuitBreakerSlow = cbFactory.create("slow");
				this.observationRegistry = observationRegistry;
			}

			public String slow() {
				return circuitBreakerSlow.run(() -> rest.getForObject("/slow", String.class), t -> "fallback");
			}

			public String normal() {
				return cbFactory.create("normal")
					.run(() -> rest.getForObject("/normal", String.class), t -> "fallback");
			}

			public String withObservationRegistry() {
				return Observation.createNotStarted("my.observation", observationRegistry)
					.observe(() -> cbFactory.create("exception")
						.run(() -> new RestTemplate().getForObject("/exception", String.class), t -> "fallback"));
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
