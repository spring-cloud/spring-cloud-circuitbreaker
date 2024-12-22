/*
 * Copyright 2013-2023 the original author or authors.
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
import java.util.concurrent.TimeoutException;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ReactiveResilience4JBulkheadAndTimeLimiterIntegrationTest.Application.class)
@DirtiesContext
public class ReactiveResilience4JBulkheadAndTimeLimiterIntegrationTest {

	/**
	 * Slow bulkhead name.
	 */
	public static final String SLOW_BULKHEAD = "slowBulkhead";

	@Autowired
	Application.DemoReactiveService service;

	@Test
	public void testBulkheadThreadInterrupted() {

		StepVerifier.create(service.bulkheadWithDelay(1000))
			.expectNext(Application.CompletionStatus.INTERRUPTED)
			.verifyComplete();
	}

	@Test
	public void testBulkheadFastCallNotInterrupted() {
		StepVerifier.create(service.bulkheadWithDelay(300))
			.expectNext(Application.CompletionStatus.SUCCESS)
			.verifyComplete();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	protected static class Application {

		@Bean
		public Customizer<ReactiveResilience4JCircuitBreakerFactory> reactiveSlowBulkheadCustomizer() {
			TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
				.timeoutDuration(Duration.ofMillis(500))
				.build();
			return circuitBreakerFactory -> circuitBreakerFactory
				.configure(builder -> builder.timeLimiterConfig(timeLimiterConfig), SLOW_BULKHEAD);
		}

		@Bean
		public Customizer<ReactiveResilience4jBulkheadProvider> reactiveBulkheadProviderCustomizer() {
			return provider -> provider.addBulkheadCustomizer(bulkhead -> { }, SLOW_BULKHEAD);
		}

		enum CompletionStatus {
			SUCCESS, INTERRUPTED
		}

		@Service
		public static class DemoReactiveService {

			private final ReactiveCircuitBreaker slowBulkheadCircuitBreaker;

			DemoReactiveService(ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory) {
				this.slowBulkheadCircuitBreaker = circuitBreakerFactory.create(SLOW_BULKHEAD);
			}

			public Mono<CompletionStatus> bulkheadWithDelay(long delay) {
				return slowBulkheadCircuitBreaker.run(
					Mono.just(CompletionStatus.SUCCESS)
						.delayElement(Duration.ofMillis(delay)),
					throwable -> {
						if (throwable instanceof TimeoutException || throwable instanceof BulkheadFullException) {
							return Mono.just(CompletionStatus.INTERRUPTED);
						}
						return Mono.error(throwable);
					}
				);
			}
		}
	}
}
