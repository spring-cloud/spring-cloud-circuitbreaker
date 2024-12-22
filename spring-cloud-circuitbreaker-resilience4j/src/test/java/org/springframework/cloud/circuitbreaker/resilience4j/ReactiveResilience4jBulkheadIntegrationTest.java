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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = ReactiveResilience4jBulkheadIntegrationTest.Application.class,
	properties = {"management.endpoints.web.exposure.include=*"})
@DirtiesContext
public class ReactiveResilience4jBulkheadIntegrationTest {

	@Autowired
	Application.DemoControllerService service;

	@Autowired
	private TestRestTemplate rest;

	@Test
	public void testSlow() {
		StepVerifier.create(service.slow())
			.expectNext("fallback")
			.verifyComplete();
	}

	@Test
	public void testNormal() {
		StepVerifier.create(service.normal())
			.expectNext("normal")
			.verifyComplete();
	}

	@Test
	public void testSlowResponsesDontFailSubsequentGoodRequests() {
		StepVerifier.create(service.slowOnDemand(5000))
			.expectNext("fallback")
			.verifyComplete();

		StepVerifier.create(service.slowOnDemand(0))
			.expectNext("normal")
			.verifyComplete();
	}

	@Test
	public void testBulkheadConcurrentCallLimit() {
		List<String> results = Collections.synchronizedList(new ArrayList<>());

		Flux.merge(
				Mono.defer(() -> service.slowBulkhead()
					.doOnNext(results::add)),
				Mono.defer(() -> service.slowBulkhead()
					.doOnNext(results::add))
			).blockLast();

		assertThat(results).containsExactlyInAnyOrder("slowBulkhead", "rejected");
	}

	@Test
	public void testResilience4JMetricsAvailable() {
		StepVerifier.create(service.normal())
			.expectNext("normal")
			.verifyComplete();

		@SuppressWarnings("unchecked")
		Map<String, Object> metrics = rest.getForObject("/actuator/metrics", Map.class);
		List<String> metricNames = (List<String>) metrics.get("names");

		assertThat(metricNames).contains("resilience4j.bulkhead.max.allowed.concurrent.calls");
		assertThat(rest
			.getForObject("/actuator/metrics/resilience4j.bulkhead.max.allowed.concurrent.calls", Map.class)
			.get("availableTags")).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		private static final Log LOG = LogFactory.getLog(Application.class);

		@GetMapping("/slow")
		public Mono<String> slow() {
			return Mono.delay(Duration.ofSeconds(3)).thenReturn("slow");
		}

		@GetMapping("/normal")
		public Mono<String> normal() {
			return Mono.just("normal");
		}

		@GetMapping("/slowOnDemand")
		public Mono<String> slowOnDemand(@RequestHeader HttpHeaders headers) {
			if (headers.containsKey("delayInMilliseconds")) {
				String delayString = headers.getFirst("delayInMilliseconds");
				LOG.info("delay header: " + delayString);
				if (delayString != null) {
					try {
						long delay = Long.parseLong(delayString);
						return Mono.delay(Duration.ofMillis(delay)).thenReturn("normal");
					}
					catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
			return Mono.just("normal");
		}

		@GetMapping("/slowBulkhead")
		public Mono<String> slowBulkhead() {
			return Mono.delay(Duration.ofSeconds(3)).thenReturn("slowBulkhead");
		}

		@Bean
		public Customizer<ReactiveResilience4JCircuitBreakerFactory> slowCustomizer() {
			return factory -> {
				factory.configure(builder -> builder.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
						.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(3)).build()),
					"slow");
				factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
					.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(4)).build())
					.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
					.build());
			};
		}

		@Bean
		public Customizer<ReactiveResilience4jBulkheadProvider> bulkheadCustomizer() {
			return provider -> {
				provider.configure(builder -> builder.bulkheadConfig(
					BulkheadConfig.custom().maxConcurrentCalls(1).build()), "slowBulkhead");
			};
		}

		@Service
		public static class DemoControllerService {

			private final ReactiveCircuitBreakerFactory cbFactory;

			private final ReactiveCircuitBreaker circuitBreakerSlow;

			DemoControllerService(ReactiveCircuitBreakerFactory cbFactory) {
				this.cbFactory = cbFactory;
				this.circuitBreakerSlow = cbFactory.create("slow");
			}

			public Mono<String> slow() {
				return circuitBreakerSlow.run(
					Mono.delay(Duration.ofSeconds(4))
						.thenReturn("slow"),
					t -> Mono.just("fallback")
				);
			}

			public Mono<String> normal() {
				return cbFactory.create("normal").run(Mono.just("normal"), t -> Mono.just("fallback"));
			}

			public Mono<String> slowOnDemand(int delayInMilliseconds) {
				LOG.info("delay: " + delayInMilliseconds);
				return circuitBreakerSlow.run(
					Mono.delay(Duration.ofMillis(delayInMilliseconds)).thenReturn("normal"),
					t -> Mono.just("fallback")
				);
			}

			public Mono<String> slowBulkhead() {
				return cbFactory.create("slowBulkhead")
					.run(
						Mono.delay(Duration.ofSeconds(2)).thenReturn("slowBulkhead"),
						throwable -> {
							if (throwable instanceof BulkheadFullException) {
								return Mono.just("rejected");
							}
							return Mono.just("fallback");
						}
					);
			}
		}
	}
}
