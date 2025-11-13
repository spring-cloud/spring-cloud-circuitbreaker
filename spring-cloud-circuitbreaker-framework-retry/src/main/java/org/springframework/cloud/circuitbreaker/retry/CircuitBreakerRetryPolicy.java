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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;

import org.springframework.core.retry.RetryPolicy;

/**
 * A stateful circuit breaker implementation modeled after Spring Retry's
 * CircuitBreakerRetryPolicy. It maintains state about failures and implements circuit
 * breaker semantics (closed, open, half-open states).
 *
 * <p>
 * The circuit breaker opens after a single failed retry attempt (all retries exhausted).
 * Once open, requests fail immediately for the duration of the openTimeout. After the
 * openTimeout, the circuit transitions to half-open, allowing one test request. If that
 * succeeds, the circuit closes; if it fails, the circuit reopens.
 * </p>
 *
 * <p>
 * The resetTimeout determines how long to wait after a failure before resetting the
 * state. If a failure occurs and no more failures happen within the resetTimeout period,
 * the circuit breaker state is reset.
 * </p>
 *
 * @author Ryan Baxter
 */
public class CircuitBreakerRetryPolicy {

	/**
	 * Circuit breaker state enumeration.
	 */
	public enum State {

		/**
		 * Circuit is closed - requests pass through normally.
		 */
		CLOSED,

		/**
		 * Circuit is open - requests fail immediately.
		 */
		OPEN,

		/**
		 * Circuit is half-open - one test request is allowed through.
		 */
		HALF_OPEN

	}

	private final RetryPolicy retryPolicy;

	private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

	private final AtomicLong openedAt = new AtomicLong(0);

	private final AtomicLong lastFailureTime = new AtomicLong(0);

	private final AtomicReference<Throwable> lastException = new AtomicReference<>();

	private final Duration openTimeout;

	private final Duration resetTimeout;

	/**
	 * Create a new circuit breaker retry policy with default settings.
	 * @param retryPolicy the underlying retry policy to use when the circuit is closed
	 */
	public CircuitBreakerRetryPolicy(RetryPolicy retryPolicy) {
		this(retryPolicy, Duration.ofSeconds(20), Duration.ofSeconds(5));
	}

	/**
	 * Create a new circuit breaker retry policy.
	 * @param retryPolicy the underlying retry policy to use when the circuit is closed
	 * @param openTimeout the time the circuit stays open before transitioning to
	 * half-open
	 * @param resetTimeout the time to wait after a failure before resetting the circuit
	 * breaker state
	 */
	public CircuitBreakerRetryPolicy(RetryPolicy retryPolicy, Duration openTimeout, Duration resetTimeout) {
		this.retryPolicy = retryPolicy;
		this.openTimeout = openTimeout;
		this.resetTimeout = resetTimeout;
	}

	/**
	 * Check if a retry should be attempted based on circuit breaker state.
	 * @return true if retry should be attempted, false otherwise
	 */
	public boolean canRetry() {
		State currentState = this.state.get();
		long now = System.currentTimeMillis();

		// Check if we should reset due to resetTimeout
		long lastFailure = this.lastFailureTime.get();
		if (lastFailure > 0 && (now - lastFailure >= this.resetTimeout.toMillis())) {
			// Reset the circuit breaker if enough time has passed since last failure
			if (currentState == State.OPEN && this.state.compareAndSet(State.OPEN, State.CLOSED)) {
				this.lastException.set(null);
			}
			currentState = this.state.get();
		}

		// If circuit is open, check if openTimeout has passed
		if (currentState == State.OPEN) {
			long opened = this.openedAt.get();
			if (now - opened >= this.openTimeout.toMillis()) {
				// Try to transition to half-open
				if (this.state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
					// Allow one request through in half-open state
					return true;
				}
			}
			// Circuit is open and timeout hasn't passed
			return false;
		}

		// Circuit is closed or half-open, allow retry
		return true;
	}

	/**
	 * Record a successful execution.
	 */
	public void recordSuccess() {
		State currentState = this.state.get();

		if (currentState == State.HALF_OPEN) {
			// Successful request in half-open state, close the circuit
			if (this.state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
				this.lastException.set(null);
				this.lastFailureTime.set(0);
			}
		}
		else if (currentState == State.CLOSED) {
			// Reset state on success
			this.lastException.set(null);
			this.lastFailureTime.set(0);
		}
	}

	/**
	 * Record a failed execution. The circuit will open immediately after a single failed
	 * execution (after all retries are exhausted).
	 * @param exception the exception that caused the failure
	 */
	public void recordFailure(Throwable exception) {
		this.lastException.set(exception);
		this.lastFailureTime.set(System.currentTimeMillis());
		State currentState = this.state.get();

		if (currentState == State.HALF_OPEN) {
			// Failed request in half-open state, open the circuit again
			if (this.state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
				this.openedAt.set(System.currentTimeMillis());
			}
		}
		else if (currentState == State.CLOSED) {
			// Open the circuit immediately on first failure
			if (this.state.compareAndSet(State.CLOSED, State.OPEN)) {
				this.openedAt.set(System.currentTimeMillis());
			}
		}
	}

	/**
	 * Check if the circuit is currently open.
	 * @return true if the circuit is open, false otherwise
	 */
	public boolean isOpen() {
		return this.state.get() == State.OPEN;
	}

	/**
	 * Get the current state of the circuit breaker.
	 * @return the current state
	 */
	public State getState() {
		return this.state.get();
	}

	/**
	 * Get the last exception that occurred.
	 * @return the last exception, or null if no exception has occurred
	 */
	public @Nullable Throwable getLastException() {
		return this.lastException.get();
	}

	/**
	 * Get the underlying retry policy.
	 * @return the retry policy
	 */
	public RetryPolicy getRetryPolicy() {
		return this.retryPolicy;
	}

	/**
	 * Get the open timeout duration.
	 * @return the open timeout
	 */
	public Duration getOpenTimeout() {
		return this.openTimeout;
	}

	/**
	 * Get the reset timeout duration.
	 * @return the reset timeout
	 */
	public Duration getResetTimeout() {
		return this.resetTimeout;
	}

	/**
	 * Reset the circuit breaker to its initial state.
	 */
	public void reset() {
		this.state.set(State.CLOSED);
		this.openedAt.set(0);
		this.lastFailureTime.set(0);
		this.lastException.set(null);
	}

}
