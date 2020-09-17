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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = ReactiveResilience4JCircuitBreakerIntegrationTest.Application.class,
		properties = { "management.endpoints.web.exposure.include=*" })
@DirtiesContext
public class ReactiveResilience4JCircuitBreakerIntegrationTest {

	@Mock
	static EventConsumer<CircuitBreakerOnErrorEvent> slowErrorConsumer;

	@Mock
	static EventConsumer<CircuitBreakerOnSuccessEvent> slowSuccessConsumer;

	@Mock
	static EventConsumer<CircuitBreakerOnErrorEvent> normalErrorConsumer;

	@Mock
	static EventConsumer<CircuitBreakerOnSuccessEvent> normalSuccessConsumer;

	@Mock
	static EventConsumer<CircuitBreakerOnErrorEvent> slowFluxErrorConsumer;

	@Mock
	static EventConsumer<CircuitBreakerOnSuccessEvent> slowFluxSuccessConsumer;

	@Mock
	static EventConsumer<CircuitBreakerOnErrorEvent> normalFluxErrorConsumer;

	@Mock
	static EventConsumer<CircuitBreakerOnSuccessEvent> normalFluxSuccessConsumer;

	@LocalServerPort
	int port = 0;

	@Autowired
	ReactiveResilience4JCircuitBreakerIntegrationTest.Application.DemoControllerService service;

	@Autowired
	private WebTestClient webClient;

	@Before
	public void setup() {
		service.setPort(port);
	}

	@Test
	public void test() {
		StepVerifier.create(service.normal()).expectNext("normal").expectComplete().verify();
		verify(normalErrorConsumer, times(0)).consumeEvent(any());
		verify(normalSuccessConsumer, times(1)).consumeEvent(any());
		StepVerifier.withVirtualTime(() -> service.slow()).expectSubscription().expectNoEvent(Duration.ofSeconds(2))
				.expectNext("fallback").expectComplete().verify();
		verify(slowErrorConsumer, times(1)).consumeEvent(any());
		verify(slowSuccessConsumer, times(0)).consumeEvent(any());
		StepVerifier.create(service.normalFlux()).expectNext("normalflux").verifyComplete();
		verify(normalFluxErrorConsumer, times(0)).consumeEvent(any());
		verify(normalFluxSuccessConsumer, times(1)).consumeEvent(any());
		StepVerifier.create(service.slowFlux()).expectNext("fluxfallback").verifyComplete();
		verify(slowFluxErrorConsumer, times(1)).consumeEvent(any());
		verify(slowSuccessConsumer, times(0)).consumeEvent(any());
		assertThat(
				((List) webClient.get().uri("/actuator/metrics").exchange().expectStatus().isOk().expectBody(Map.class)
						.returnResult().getResponseBody().get("names")).contains("resilience4j.circuitbreaker.calls"))
								.isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		@GetMapping("/slow")
		public Mono<String> slow() {
			return Mono.just("slow").delayElement(Duration.ofSeconds(3));
		}

		@GetMapping("/normal")
		public Mono<String> normal() {
			return Mono.just("normal");
		}

		@GetMapping("/slowflux")
		public Flux<String> slowFlux() {
			return Flux.just("slow", "flux").delayElements(Duration.ofSeconds(3));
		}

		@GetMapping("normalflux")
		public Flux<String> normalFlux() {
			return Flux.just("normal", "flux");
		}

		@Bean
		public Customizer<ReactiveResilience4JCircuitBreakerFactory> slowCusomtizer() {
			return factory -> {
				factory.configureDefault(
						id -> new Resilience4JConfigBuilder(id).circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
								.timeLimiterConfig(
										TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(4)).build())
								.build());
				factory.configure(builder -> builder
						.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(2)).build())
						.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults()), "slow", "slowflux");
				factory.addCircuitBreakerCustomizer(circuitBreaker -> circuitBreaker.getEventPublisher()
						.onError(slowErrorConsumer).onSuccess(slowSuccessConsumer), "slow");
				factory.addCircuitBreakerCustomizer(circuitBreaker -> circuitBreaker.getEventPublisher()
						.onError(normalErrorConsumer).onSuccess(normalSuccessConsumer), "normal");
				factory.addCircuitBreakerCustomizer(circuitBreaker -> circuitBreaker.getEventPublisher()
						.onError(slowFluxErrorConsumer).onSuccess(slowFluxSuccessConsumer), "slowflux");
				factory.addCircuitBreakerCustomizer(circuitBreaker -> circuitBreaker.getEventPublisher()
						.onError(normalFluxErrorConsumer).onSuccess(normalFluxSuccessConsumer), "normalflux");
			};
		}

		@Service
		public static class DemoControllerService {

			private int port = 0;

			private final ReactiveCircuitBreakerFactory cbFactory;

			private final ReactiveCircuitBreaker circuitBreakerSlow;

			DemoControllerService(ReactiveCircuitBreakerFactory cbFactory) {
				this.cbFactory = cbFactory;
				this.circuitBreakerSlow = cbFactory.create("slow");
			}

			public Mono<String> slow() {
				return WebClient.builder().baseUrl("http://localhost:" + port).build().get().uri("/slow").retrieve()
						.bodyToMono(String.class).transform(it -> circuitBreakerSlow.run(it, t -> {
							t.printStackTrace();
							return Mono.just("fallback");
						}));
			}

			public Mono<String> normal() {
				return WebClient.builder().baseUrl("http://localhost:" + port).build().get().uri("/normal").retrieve()
						.bodyToMono(String.class).transform(it -> cbFactory.create("normal").run(it, t -> {
							t.printStackTrace();
							return Mono.just("fallback");
						}));
			}

			public Flux<String> slowFlux() {
				return WebClient.builder().baseUrl("http://localhost:" + port).build().get().uri("/slowflux").retrieve()
						.bodyToFlux(new ParameterizedTypeReference<String>() {
						}).transform(it -> cbFactory.create("slowflux").run(it, t -> {
							t.printStackTrace();
							return Flux.just("fluxfallback");
						}));
			}

			public Flux<String> normalFlux() {
				return WebClient.builder().baseUrl("http://localhost:" + port).build().get().uri("/normalflux")
						.retrieve().bodyToFlux(String.class)
						.transform(it -> cbFactory.create("normalflux").run(it, t -> {
							t.printStackTrace();
							return Flux.just("fluxfallback");
						}));
			}

			public void setPort(int port) {
				this.port = port;
			}

		}

	}

}
