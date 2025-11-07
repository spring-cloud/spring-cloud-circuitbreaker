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

import org.junit.jupiter.api.Test;

import org.springframework.cloud.circuitbreaker.retry.CircuitBreakerRetryPolicy.State;
import org.springframework.core.retry.RetryPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CircuitBreakerRetryPolicy}. These tests verify Spring Retry-style
 * circuit breaker behavior where the circuit opens after a single failure (all retries
 * exhausted).
 *
 * @author Ryan Baxter
 */
class CircuitBreakerRetryPolicyTest {

	@Test
	void testCircuitBreakerStartsClosed() {
		CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(RetryPolicy.withDefaults());
		assertThat(policy.isOpen()).isFalse();
		assertThat(policy.getState()).isEqualTo(State.CLOSED);
	}

	@Test
	void testCircuitBreakerOpensAfterSingleFailure() {
		CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(RetryPolicy.withDefaults(),
				Duration.ofSeconds(20), Duration.ofSeconds(5));

		Exception testException = new RuntimeException("Test error");

		// Record a single failure - circuit should open immediately
		policy.recordFailure(testException);
		assertThat(policy.isOpen()).isTrue();
		assertThat(policy.getState()).isEqualTo(State.OPEN);
	}

	@Test
	void testCircuitBreakerResetOnSuccess() throws InterruptedException {
		CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(RetryPolicy.withDefaults(),
				Duration.ofMillis(100), Duration.ofSeconds(5));

		Exception testException = new RuntimeException("Test error");

		// Open the circuit
		policy.recordFailure(testException);
		assertThat(policy.isOpen()).isTrue();

		// Wait for openTimeout to transition to half-open
		Thread.sleep(150);
		policy.canRetry();
		assertThat(policy.getState()).isEqualTo(State.HALF_OPEN);

		// Record success - should close the circuit
		policy.recordSuccess();
		assertThat(policy.isOpen()).isFalse();
		assertThat(policy.getState()).isEqualTo(State.CLOSED);
	}

	@Test
	void testCircuitBreakerCannotRetryWhenOpen() {
		CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(RetryPolicy.withDefaults(),
				Duration.ofSeconds(20), Duration.ofSeconds(5));

		Exception testException = new RuntimeException("Test error");

		// Open the circuit
		policy.recordFailure(testException);
		assertThat(policy.isOpen()).isTrue();

		// Should not be able to retry immediately
		assertThat(policy.canRetry()).isFalse();
	}

	@Test
	void testCircuitBreakerHalfOpenAfterOpenTimeout() throws InterruptedException {
		// Use a very short openTimeout for testing
		CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(RetryPolicy.withDefaults(),
				Duration.ofMillis(100), Duration.ofSeconds(5));

		Exception testException = new RuntimeException("Test error");

		// Open the circuit
		policy.recordFailure(testException);
		assertThat(policy.isOpen()).isTrue();

		// Wait for openTimeout
		Thread.sleep(150);

		// Circuit should allow one retry (transitions to half-open state)
		assertThat(policy.canRetry()).isTrue();
		assertThat(policy.getState()).isEqualTo(State.HALF_OPEN);
	}

	@Test
	void testCircuitBreakerClosesAfterSuccessInHalfOpen() throws InterruptedException {
		CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(RetryPolicy.withDefaults(),
				Duration.ofMillis(100), Duration.ofSeconds(5));

		Exception testException = new RuntimeException("Test error");

		// Open the circuit
		policy.recordFailure(testException);
		assertThat(policy.isOpen()).isTrue();

		// Wait for openTimeout
		Thread.sleep(150);

		// Enter half-open state
		policy.canRetry();
		assertThat(policy.getState()).isEqualTo(State.HALF_OPEN);

		// Success in half-open state should close the circuit
		policy.recordSuccess();
		assertThat(policy.isOpen()).isFalse();
		assertThat(policy.getState()).isEqualTo(State.CLOSED);
	}

	@Test
	void testCircuitBreakerReopensAfterFailureInHalfOpen() throws InterruptedException {
		CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(RetryPolicy.withDefaults(),
				Duration.ofMillis(100), Duration.ofSeconds(5));

		Exception testException = new RuntimeException("Test error");

		// Open the circuit
		policy.recordFailure(testException);
		assertThat(policy.isOpen()).isTrue();

		// Wait for openTimeout
		Thread.sleep(150);

		// Enter half-open state
		policy.canRetry();
		assertThat(policy.getState()).isEqualTo(State.HALF_OPEN);

		// Failure in half-open state should reopen the circuit
		policy.recordFailure(testException);
		assertThat(policy.isOpen()).isTrue();
		assertThat(policy.getState()).isEqualTo(State.OPEN);
	}

	@Test
	void testResetCircuitBreaker() {
		CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(RetryPolicy.withDefaults(),
				Duration.ofSeconds(20), Duration.ofSeconds(5));

		Exception testException = new RuntimeException("Test error");

		// Open the circuit
		policy.recordFailure(testException);
		assertThat(policy.isOpen()).isTrue();

		// Reset
		policy.reset();
		assertThat(policy.isOpen()).isFalse();
		assertThat(policy.getState()).isEqualTo(State.CLOSED);
		assertThat(policy.getLastException()).isNull();
	}

	@Test
	void testGetLastException() {
		CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(RetryPolicy.withDefaults());

		Exception testException = new RuntimeException("Test error");
		policy.recordFailure(testException);

		assertThat(policy.getLastException()).isSameAs(testException);
	}

	@Test
	void testResetTimeoutClosesCircuit() throws InterruptedException {
		// Short resetTimeout for testing
		CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(RetryPolicy.withDefaults(),
				Duration.ofSeconds(20), Duration.ofMillis(100));

		Exception testException = new RuntimeException("Test error");

		// Open the circuit
		policy.recordFailure(testException);
		assertThat(policy.isOpen()).isTrue();

		// Wait for resetTimeout
		Thread.sleep(150);

		// Circuit should transition back to closed due to resetTimeout
		assertThat(policy.canRetry()).isTrue();
		assertThat(policy.getState()).isEqualTo(State.CLOSED);
	}

	@Test
	void testGetters() {
		Duration openTimeout = Duration.ofSeconds(30);
		Duration resetTimeout = Duration.ofSeconds(10);
		RetryPolicy retryPolicy = RetryPolicy.withDefaults();

		CircuitBreakerRetryPolicy policy = new CircuitBreakerRetryPolicy(retryPolicy, openTimeout, resetTimeout);

		assertThat(policy.getRetryPolicy()).isSameAs(retryPolicy);
		assertThat(policy.getOpenTimeout()).isEqualTo(openTimeout);
		assertThat(policy.getResetTimeout()).isEqualTo(resetTimeout);
	}

}
