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

package org.springframework.cloud.circuitbreaker.retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link FrameworkRetryCircuitBreaker}.
 *
 * @author Ryan Baxter
 */
@SpringBootTest(classes = FrameworkRetryCircuitBreakerIntegrationTest.TestConfiguration.class)
class FrameworkRetryCircuitBreakerIntegrationTest {

	@Autowired
	private CircuitBreakerFactory circuitBreakerFactory;

	@Autowired
	private TestService testService;

	@Test
	void testCircuitBreakerIntegration() {
		CircuitBreaker circuitBreaker = circuitBreakerFactory.create("test");

		// Test successful execution
		String result = circuitBreaker.run(() -> "success", throwable -> "fallback");
		assertThat(result).isEqualTo("success");

		// Test fallback on failure
		result = circuitBreaker.run(() -> {
			throw new RuntimeException("Test error");
		}, throwable -> "fallback");
		assertThat(result).isEqualTo("fallback");
	}

	@Test
	void testCircuitBreakerWithService() {
		testService.resetCounter();
		String result = testService.callWithCircuitBreaker();
		assertThat(result).isEqualTo("success");
	}

	@Test
	void testCircuitBreakerOpensAndClosesWithService() throws InterruptedException {
		testService.resetCounter();
		testService.setFailUntil(10); // Fail for 10 calls

		// Make calls that will fail and open the circuit
		for (int i = 0; i < 5; i++) {
			String result = testService.callWithCircuitBreaker();
			assertThat(result).isEqualTo("fallback");
		}

		// Circuit should be open now, so calls return fallback immediately
		int callsBefore = testService.getCallCount();
		String result = testService.callWithCircuitBreaker();
		assertThat(result).isEqualTo("fallback");
		// No additional calls to the service when circuit is open
		assertThat(testService.getCallCount()).isEqualTo(callsBefore);

		// Wait for circuit to half-open
		Thread.sleep(150);

		// Allow success
		testService.setFailUntil(0);

		// Next call should succeed and close the circuit
		result = testService.callWithCircuitBreaker();
		assertThat(result).isEqualTo("success");

		// Verify circuit is closed by making another successful call
		result = testService.callWithCircuitBreaker();
		assertThat(result).isEqualTo("success");
	}

	@Configuration
	@EnableAutoConfiguration
	static class TestConfiguration {

		@Bean
		public Customizer<FrameworkRetryCircuitBreakerFactory> customizer() {
			return factory -> {
				factory.configureDefault(
						id -> new FrameworkRetryConfigBuilder(id).retryPolicy(RetryPolicy.withMaxRetries(2))
							.openTimeout(Duration.ofMillis(100))
							.resetTimeout(Duration.ofSeconds(5))
							.build());
			};
		}

		@Bean
		public TestService testService(CircuitBreakerFactory circuitBreakerFactory) {
			return new TestService(circuitBreakerFactory);
		}

	}

	@Service
	static class TestService {

		private final CircuitBreakerFactory circuitBreakerFactory;

		private final AtomicInteger callCount = new AtomicInteger(0);

		private volatile int failUntil = 0;

		TestService(CircuitBreakerFactory circuitBreakerFactory) {
			this.circuitBreakerFactory = circuitBreakerFactory;
		}

		String callWithCircuitBreaker() {
			CircuitBreaker circuitBreaker = circuitBreakerFactory.create("testService");
			return circuitBreaker.run(() -> doWork(), throwable -> "fallback");
		}

		private String doWork() {
			int count = callCount.incrementAndGet();
			if (count <= failUntil) {
				throw new RuntimeException("Simulated failure");
			}
			return "success";
		}

		void resetCounter() {
			callCount.set(0);
			failUntil = 0;
		}

		void setFailUntil(int count) {
			this.failUntil = count;
		}

		int getCallCount() {
			return callCount.get();
		}

	}

}
