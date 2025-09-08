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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests that time limiter threads are interrupted correctly when used with a bulkhead.
 *
 * @author Renette Ros
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Resilience4JBulkheadAndTimeLimiterIntegrationTest.Application.class)
@DirtiesContext
public class Resilience4JBulkheadAndTimeLimiterIntegrationTest {

	/**
	 * Slow bulkghead name.
	 */
	public static final String SLOW_BULKHEAD = "slowBulkhead";

	/**
	 * Slow thread pool bulkhead name.
	 */
	public static final String SLOW_THREAD_POOL_BULKHEAD = "slowThreadPoolBulkhead";

	@Autowired
	Application.DemoService service;

	@Test
	public void testBulkheadThreadInterrupted() {
		InterruptibleTask interruptibleTask = new InterruptibleTask();
		assertThatExceptionOfType(NoFallbackAvailableException.class)
			.isThrownBy(() -> service.bulkheadTimeout(interruptibleTask));
		assertThat(interruptibleTask.interrupted()).isTrue();
	}

	@Test
	public void testThreadPoolBulkheadThreadInterrupted() {
		InterruptibleTask interruptibleTask = new InterruptibleTask();
		assertThatExceptionOfType(NoFallbackAvailableException.class)
			.isThrownBy(() -> service.threadPoolBulkheadTimeout(interruptibleTask))
			.havingCause()
			.withCauseExactlyInstanceOf(TimeoutException.class);
		assertThat(interruptibleTask.interrupted()).isTrue();
	}

	@Test
	public void testBulkheadFastCallNotInterrupted() {
		assertThat(service.bulkheadFast()).isEqualTo(Application.CompletionStatus.SUCCESS);
	}

	@Test
	public void testThreadPoolFastCallNotInterrupted() {
		assertThat(service.threadPoolBulkheadFast()).isEqualTo(Application.CompletionStatus.SUCCESS);
	}

	static class InterruptibleTask {

		private final AtomicBoolean interrupted = new AtomicBoolean(false);

		public Application.CompletionStatus run(int sleepMillis) {
			try {
				Thread.sleep(sleepMillis);
				return Application.CompletionStatus.SUCCESS;
			}
			catch (InterruptedException ignored) {
				interrupted.set(true);
				Thread.currentThread().interrupt();
				return Application.CompletionStatus.INTERRUPTED;
			}
		}

		boolean interrupted() {
			return interrupted.get();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		@Bean
		public Customizer<Resilience4JCircuitBreakerFactory> slowBulkheadCustomizer() {
			TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
				.timeoutDuration(Duration.ofMillis(500))
				.build();
			return circuitBreakerFactory -> circuitBreakerFactory
				.configure(builder -> builder.timeLimiterConfig(timeLimiterConfig), SLOW_BULKHEAD);
		}

		@Bean
		public Customizer<Resilience4jBulkheadProvider> bulkheadProviderCustomizer() {
			return provider -> {
				provider.addBulkheadCustomizer(bulkhead -> {
				}, SLOW_BULKHEAD);
				provider.addThreadPoolBulkheadCustomizer(builder -> {
				}, SLOW_THREAD_POOL_BULKHEAD);
			};
		}

		enum CompletionStatus {

			SUCCESS, INTERRUPTED

		}

		@Service
		public static class DemoService {

			private final CircuitBreaker bulkhead;

			private final CircuitBreaker threadPoolBulkhead;

			DemoService(CircuitBreakerFactory<?, ?> cbFactory) {
				this.bulkhead = cbFactory.create(SLOW_BULKHEAD);
				this.threadPoolBulkhead = cbFactory.create(SLOW_THREAD_POOL_BULKHEAD);
			}

			public CompletionStatus bulkheadTimeout(InterruptibleTask interruptibleTask) {
				return bulkhead.run(() -> interruptibleTask.run(2_000));
			}

			public CompletionStatus threadPoolBulkheadTimeout(InterruptibleTask interruptibleTask) {
				return threadPoolBulkhead.run(() -> interruptibleTask.run(2_000));
			}

			public CompletionStatus bulkheadFast() {
				return bulkhead.run(() -> new InterruptibleTask().run(100));
			}

			public CompletionStatus threadPoolBulkheadFast() {
				return threadPoolBulkhead.run(() -> new InterruptibleTask().run(100));
			}

		}

	}

}
