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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(
	webEnvironment = RANDOM_PORT
	, classes = Resilience4JCAGIntegrationTest.Application.class
)
@ActiveProfiles(profiles = "test-properties")
@DirtiesContext
@RunWith(SpringRunner.class)
public class Resilience4JCAGIntegrationTest {

	@Autowired
	CircuitBreakerFactory circuitBreakerFactory;

	@Mock
	static EventConsumer<CircuitBreakerOnStateTransitionEvent> cbStateConsumer;

	@Mock
	static EventConsumer<BulkheadOnCallRejectedEvent> bhStateConsumer;

	@Autowired
	TestRestTemplate testRestTemplate;

	@Test
	public void testCircuitBreaker() {
		TestService mock = mock(TestService.class);
		given(mock.test()).willThrow(new RuntimeException());

		IntStream.range(0, 6).forEach(i -> {
			circuitBreakerFactory
				.create("test_cag")
				.run(mock::test, e -> "error");
		});

		verify(cbStateConsumer, times(4)).consumeEvent(any());
	}

	@Test
	public void testTimeLimiter() {
		Supplier<String> supplier = () -> {
			try {
				TimeUnit.SECONDS.sleep(2);
			}
			catch (InterruptedException e) {
			}
			return "success";
		};

		circuitBreakerFactory
			.create("test_cag")
			.run(supplier);

		verify(cbStateConsumer, times(0)).consumeEvent(any());
	}

	@Test
	public void testBulkhead() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(4);
		executorService.submit(() -> testRestTemplate.getForObject("/test", String.class));
		executorService.submit(() -> testRestTemplate.getForObject("/test", String.class));
		executorService.submit(() -> testRestTemplate.getForObject("/test", String.class));
		executorService.submit(() -> testRestTemplate.getForObject("/test", String.class));
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);
		verify(bhStateConsumer, times(1)).consumeEvent(any());
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	@RestController
	static class Application {

		@Autowired
		CircuitBreakerFactory circuitBreakerFactory;

		@Bean
		public Customizer<Resilience4JCircuitBreakerFactory> circuitBreakerFactoryCustomizer() {
			return factory -> {
				factory.configure(
					builder -> builder
						.circuitBreakerConfig(CircuitBreakerConfig.custom().minimumNumberOfCalls(4).build())
						.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(3)).build())
						.build()
					, "test_cag"
				);
				factory.addCircuitBreakerCustomizer(
					circuitBreaker -> {
						circuitBreaker.getEventPublisher().onStateTransition(cbStateConsumer);
					}
					, "test_cag"
				);
			};
		}

		@Bean
		public Customizer<Resilience4jBulkheadProvider> bulkheadProviderCustomizer() {
			return provider -> {
				provider.configure(
					builder -> builder
						.bulkheadConfig(BulkheadConfig.custom().maxConcurrentCalls(4).build())
						.threadPoolBulkheadConfig(ThreadPoolBulkheadConfig
							.custom()
							.maxThreadPoolSize(2)
							.coreThreadPoolSize(1)
							.queueCapacity(1)
							.keepAliveDuration(Duration.ZERO)
							.build())
					, "test_cag"
				);
				provider.addThreadPoolBulkheadCustomizer(
					bulkhead -> bulkhead.getEventPublisher().onCallRejected(bhStateConsumer)
					, "test_cag"
				);
			};
		}

		@RequestMapping(value = "/test", method = RequestMethod.GET)
		public String test() {

			return circuitBreakerFactory.create("test_cag").run(() -> "test");
		}
	}

	interface TestService {

		String test();

	}
}
