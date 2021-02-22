/*
 * Copyright 2013-2020 the original author or authors.
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

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
 * @author Andrii Bohutskyi
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = Resilience4jBulkheadIntegrationTest.Application.class,
		properties = { "management.endpoints.web.exposure.include=*" })
@DirtiesContext
public class Resilience4jBulkheadIntegrationTest {

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
	public void testBulkheadTwoParallelSlowOneNotPermitted() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.submit(() -> service.slowBulkhead());
		executorService.submit(() -> service.slowBulkhead());
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);

		verify(Application.slowRejectedConsumer, times(1)).consumeEvent(any());
		verify(Application.slowFinishedConsumer, times(1)).consumeEvent(any());
	}

	@Test
	public void testThreadPoolBulkheadThreeParallelSlowOneNotPermitted() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(3);
		executorService.submit(() -> service.slowThreadPoolBulkhead());
		executorService.submit(() -> service.slowThreadPoolBulkhead());
		executorService.submit(() -> service.slowThreadPoolBulkhead());
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);

		verify(Application.slowThreadPoolFinishedConsumer, times(2)).consumeEvent(any());
		verify(Application.slowThreadPoolRejectedConsumer, times(1)).consumeEvent(any());
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

		@GetMapping("/slowBulkhead")
		public String slowBulkhead() throws InterruptedException {
			Thread.sleep(3000);
			return "slowBulkhead";
		}

		@GetMapping("/slowThreadPoolBulkhead")
		public String slowThreadPoolBulkhead() throws InterruptedException {
			Thread.sleep(3000);
			return "slowThreadPoolBulkhead";
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

		static EventConsumer<BulkheadOnCallRejectedEvent> slowRejectedConsumer = Mockito.mock(EventConsumer.class);
		static EventConsumer<BulkheadOnCallFinishedEvent> slowFinishedConsumer = Mockito.mock(EventConsumer.class);

		@Bean
		public Customizer<Resilience4jBulkheadProvider> slowBulkheadProviderCustomizer() {
			return provider -> {
				provider.configure(
						builder -> builder.bulkheadConfig(BulkheadConfig.custom().maxConcurrentCalls(1).build()),
						"slowBulkhead");
				provider.addBulkheadCustomizer(bulkhead -> bulkhead.getEventPublisher()
						.onCallRejected(slowRejectedConsumer).onCallFinished(slowFinishedConsumer), "slowBulkhead");
			};
		}

		static EventConsumer<BulkheadOnCallRejectedEvent> slowThreadPoolRejectedConsumer = Mockito
				.mock(EventConsumer.class);
		static EventConsumer<BulkheadOnCallFinishedEvent> slowThreadPoolFinishedConsumer = Mockito
				.mock(EventConsumer.class);

		@Bean
		Customizer<Resilience4jBulkheadProvider> slowBulkheadThreadPoolProviderCustomizer() {
			return provider -> provider
					.configure(
							builder -> builder.threadPoolBulkheadConfig(ThreadPoolBulkheadConfig.custom()
									.coreThreadPoolSize(1).maxThreadPoolSize(1).queueCapacity(1).build()),
							"slowThreadPoolBulkhead");
		}

		@Bean
		public Customizer<Resilience4JCircuitBreakerFactory> slowThreadPoolCustomizer() {
			return factory -> factory.configure(
					builder -> builder.timeLimiterConfig(
							TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(8)).build()),
					"slowThreadPoolBulkhead");
		}

		@Bean
		public Customizer<Resilience4jBulkheadProvider> slowThreadPoolBulkheadCustomizer() {
			return provider -> provider.addThreadPoolBulkheadCustomizer(threadPoolBulkhead -> threadPoolBulkhead
					.getEventPublisher().onCallRejected(slowThreadPoolRejectedConsumer)
					.onCallFinished(slowThreadPoolFinishedConsumer), "slowThreadPoolBulkhead");
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

			public String slowBulkhead() {
				return cbFactory.create("slowBulkhead").run(() -> rest.getForObject("/slowBulkhead", String.class),
						t -> "fallback");
			}

			public String slowThreadPoolBulkhead() {
				return cbFactory.create("slowThreadPoolBulkhead")
						.run(() -> rest.getForObject("/slowThreadPoolBulkhead", String.class), t -> "fallback");
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
