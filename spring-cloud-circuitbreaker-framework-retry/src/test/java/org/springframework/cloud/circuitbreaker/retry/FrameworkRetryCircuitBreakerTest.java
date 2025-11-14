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

import org.springframework.core.retry.RetryPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FrameworkRetryCircuitBreaker}. These tests verify Spring Retry-style
 * circuit breaker behavior where the circuit opens after a single failure (all retries
 * exhausted).
 *
 * @author Ryan Baxter
 */
class FrameworkRetryCircuitBreakerTest {

	@Test
	void testRun() {
		FrameworkRetryConfig config = new FrameworkRetryConfigBuilder("test").build();
		FrameworkRetryCircuitBreaker circuitBreaker = new FrameworkRetryCircuitBreaker("test", config);
		String result = circuitBreaker.run(() -> "success", throwable -> "fallback");
		assertThat(result).isEqualTo("success");
	}

	@Test
	void testRunWithFallback() {
		FrameworkRetryConfig config = new FrameworkRetryConfigBuilder("test").build();
		FrameworkRetryCircuitBreaker circuitBreaker = new FrameworkRetryCircuitBreaker("test", config);
		String result = circuitBreaker.run(() -> {
			throw new RuntimeException("Error");
		}, throwable -> "fallback");
		assertThat(result).isEqualTo("fallback");
	}

	@Test
	void testRunWithRetry() {
		// Test that retries are attempted successfully
		AtomicInteger attempts = new AtomicInteger(0);
		FrameworkRetryConfig config = new FrameworkRetryConfigBuilder("test").retryPolicy(RetryPolicy.withMaxRetries(2)) // 1
																															// initial
																															// +
																															// 2
																															// retries
																															// =
																															// 3
																															// attempts
			.build();
		FrameworkRetryCircuitBreaker circuitBreaker = new FrameworkRetryCircuitBreaker("test", config);

		// Supplier that succeeds on 3rd attempt
		String result = circuitBreaker.run(() -> {
			int count = attempts.incrementAndGet();
			if (count < 3) {
				throw new RuntimeException("Error");
			}
			return "success";
		}, throwable -> "fallback");

		// Should have attempted 3 times and succeeded
		assertThat(result).isEqualTo("success");
		assertThat(attempts.get()).isEqualTo(3); // Initial attempt + 2 retries
		// Circuit should remain closed after success
		assertThat(circuitBreaker.getCircuitBreakerPolicy().isOpen()).isFalse();
	}

	@Test
	void testCircuitOpensAfterSingleFailedExecution() {
		// Use no retries so a single failure causes the circuit to open
		FrameworkRetryConfig config = new FrameworkRetryConfigBuilder("test").retryPolicy(RetryPolicy.withMaxRetries(0))
			.openTimeout(Duration.ofSeconds(20))
			.resetTimeout(Duration.ofSeconds(5))
			.build();
		FrameworkRetryCircuitBreaker circuitBreaker = new FrameworkRetryCircuitBreaker("test", config);

		// First failure - circuit opens
		circuitBreaker.run(() -> {
			throw new RuntimeException("Error 1");
		}, throwable -> "fallback");

		assertThat(circuitBreaker.getCircuitBreakerPolicy().isOpen()).isTrue();
	}

	@Test
	void testCircuitBreakerOpenReturnsFallback() {
		FrameworkRetryConfig config = new FrameworkRetryConfigBuilder("test").retryPolicy(RetryPolicy.withMaxRetries(0))
			.openTimeout(Duration.ofSeconds(20))
			.resetTimeout(Duration.ofSeconds(5))
			.build();
		FrameworkRetryCircuitBreaker circuitBreaker = new FrameworkRetryCircuitBreaker("test", config);

		// Open the circuit
		circuitBreaker.run(() -> {
			throw new RuntimeException("Error");
		}, throwable -> "fallback");

		assertThat(circuitBreaker.getCircuitBreakerPolicy().isOpen()).isTrue();

		// Subsequent calls should immediately return fallback without executing supplier
		AtomicInteger executionCount = new AtomicInteger(0);
		String result = circuitBreaker.run(() -> {
			executionCount.incrementAndGet();
			return "success";
		}, throwable -> "fallback");

		assertThat(result).isEqualTo("fallback");
		assertThat(executionCount.get()).isEqualTo(0); // Should not execute the supplier
	}

	@Test
	void testCircuitHalfOpenAfterTimeout() throws InterruptedException {
		FrameworkRetryConfig config = new FrameworkRetryConfigBuilder("test").retryPolicy(RetryPolicy.withMaxRetries(0))
			.openTimeout(Duration.ofMillis(100))
			.resetTimeout(Duration.ofSeconds(5))
			.build();
		FrameworkRetryCircuitBreaker circuitBreaker = new FrameworkRetryCircuitBreaker("test", config);

		// Open the circuit
		circuitBreaker.run(() -> {
			throw new RuntimeException("Error");
		}, throwable -> "fallback");

		assertThat(circuitBreaker.getCircuitBreakerPolicy().isOpen()).isTrue();

		// Wait for openTimeout
		Thread.sleep(150);

		// Circuit should be half-open and allow a request through
		String result = circuitBreaker.run(() -> "success", throwable -> "fallback");

		assertThat(result).isEqualTo("success");
		assertThat(circuitBreaker.getCircuitBreakerPolicy().isOpen()).isFalse();
	}

	@Test
	void testCircuitReopensOnFailureInHalfOpen() throws InterruptedException {
		FrameworkRetryConfig config = new FrameworkRetryConfigBuilder("test").retryPolicy(RetryPolicy.withMaxRetries(0))
			.openTimeout(Duration.ofMillis(100))
			.resetTimeout(Duration.ofSeconds(5))
			.build();
		FrameworkRetryCircuitBreaker circuitBreaker = new FrameworkRetryCircuitBreaker("test", config);

		// Open the circuit
		circuitBreaker.run(() -> {
			throw new RuntimeException("Error 1");
		}, throwable -> "fallback");

		assertThat(circuitBreaker.getCircuitBreakerPolicy().isOpen()).isTrue();

		// Wait for openTimeout
		Thread.sleep(150);

		// Attempt in half-open state, but it fails
		circuitBreaker.run(() -> {
			throw new RuntimeException("Error in half-open");
		}, throwable -> "fallback");

		// Circuit should reopen
		assertThat(circuitBreaker.getCircuitBreakerPolicy().isOpen()).isTrue();
	}

	@Test
	void testGettersReturnCorrectValues() {
		FrameworkRetryConfig config = new FrameworkRetryConfigBuilder("test-id").build();
		FrameworkRetryCircuitBreaker circuitBreaker = new FrameworkRetryCircuitBreaker("test-id", config);

		assertThat(circuitBreaker.getId()).isEqualTo("test-id");
		assertThat(circuitBreaker.getConfig()).isSameAs(config);
		assertThat(circuitBreaker.getCircuitBreakerPolicy()).isNotNull();
	}

	@Test
	void testCircuitOpensAfterAllRetriesExhausted() {
		// With retries enabled, circuit opens after all retries are exhausted
		FrameworkRetryConfig config = new FrameworkRetryConfigBuilder("test").retryPolicy(RetryPolicy.withMaxRetries(2))
			.openTimeout(Duration.ofSeconds(20))
			.resetTimeout(Duration.ofSeconds(5))
			.build();
		FrameworkRetryCircuitBreaker circuitBreaker = new FrameworkRetryCircuitBreaker("test", config);

		// This will fail all retries and open the circuit
		circuitBreaker.run(() -> {
			throw new RuntimeException("Error");
		}, throwable -> "fallback");

		// Circuit should be open
		assertThat(circuitBreaker.getCircuitBreakerPolicy().isOpen()).isTrue();
	}

	@Test
	void testResetTimeoutClosesCircuit() throws InterruptedException {
		FrameworkRetryConfig config = new FrameworkRetryConfigBuilder("test").retryPolicy(RetryPolicy.withMaxRetries(0))
			.openTimeout(Duration.ofSeconds(20))
			.resetTimeout(Duration.ofMillis(100))
			.build();
		FrameworkRetryCircuitBreaker circuitBreaker = new FrameworkRetryCircuitBreaker("test", config);

		// Open the circuit
		circuitBreaker.run(() -> {
			throw new RuntimeException("Error");
		}, throwable -> "fallback");

		assertThat(circuitBreaker.getCircuitBreakerPolicy().isOpen()).isTrue();

		// Wait for resetTimeout
		Thread.sleep(150);

		// Circuit should automatically close due to resetTimeout
		String result = circuitBreaker.run(() -> "success", throwable -> "fallback");

		assertThat(result).isEqualTo("success");
		assertThat(circuitBreaker.getCircuitBreakerPolicy().isOpen()).isFalse();
	}

}
