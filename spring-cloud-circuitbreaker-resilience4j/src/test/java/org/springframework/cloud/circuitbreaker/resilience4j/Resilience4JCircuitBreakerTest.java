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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 * @author Andrii Bohutskyi
 */
public class Resilience4JCircuitBreakerTest {

	Resilience4JConfigurationProperties properties = null;

	@Before
	public void before() {
		properties = new Resilience4JConfigurationProperties();
	}

	@Test
	public void run() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(), null, properties)
			.create("foo");
		assertThat(cb.run(() -> "foobar")).isEqualTo("foobar");
	}

	@Test
	public void runWithOutProperties() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(), null)
			.create("foo");
		assertThat(cb.run(() -> "foobar")).isEqualTo("foobar");
	}

	@Test
	public void runWithGroupName() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(), null)
			.create("foo", "groupFoo");
		assertThat(cb.run(() -> "foobar")).isEqualTo("foobar");

	}

	@Test
	public void runWithoutThreadPool() {
		properties.setDisableThreadPool(true);
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(), null, properties)
			.create("foo", "groupFoo");
		assertThat(cb.run(() -> "foobar")).isEqualTo("foobar");

	}

	@Test
	public void runWithFallback() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(), null, properties)
			.create("foo");
		assertThat((String) cb.run(() -> {
			throw new RuntimeException("boom");
		}, t -> "fallback")).isEqualTo("fallback");
	}

	@Test
	public void runWithFallbackAndGroupName() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(), null, properties)
			.create("foo", "groupFoo");
		assertThat((String) cb.run(() -> {
			throw new RuntimeException("boom");
		}, t -> "fallback")).isEqualTo("fallback");
	}

	@Test
	public void runWithBulkheadProvider() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(),
				new Resilience4jBulkheadProvider(ThreadPoolBulkheadRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
						new Resilience4JConfigurationProperties()),
				properties)
			.create("foo");
		assertThat(cb.run(() -> "foobar")).isEqualTo("foobar");
	}

	@Test
	public void runWithBulkheadProviderAndNoThreadPool() {
		properties.setDisableThreadPool(true);
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(),
				new Resilience4jBulkheadProvider(ThreadPoolBulkheadRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
						new Resilience4JConfigurationProperties()),
				properties)
			.create("foo");
		assertThat(cb.run(() -> "foobar")).isEqualTo("foobar");
	}

	@Test
	public void runWithBulkheadProviderAndGroupName() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(),
				new Resilience4jBulkheadProvider(ThreadPoolBulkheadRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
						new Resilience4JConfigurationProperties()),
				properties)
			.create("foo", "groupFoo");
		assertThat(cb.run(() -> "foobar")).isEqualTo("foobar");
	}

	@Test
	public void runWithBulkheadProviderAndGroupNameAndNoThreadPool() {
		properties.setDisableThreadPool(true);
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(),
				new Resilience4jBulkheadProvider(ThreadPoolBulkheadRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
						new Resilience4JConfigurationProperties()),
				properties)
			.create("foo", "groupFoo");
		assertThat(cb.run(() -> "foobar")).isEqualTo("foobar");
	}

	@Test
	public void runWithFallbackBulkheadProvider() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(),
				new Resilience4jBulkheadProvider(ThreadPoolBulkheadRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
						new Resilience4JConfigurationProperties()),
				properties)
			.create("foo");
		assertThat((String) cb.run(() -> {
			throw new RuntimeException("boom");
		}, t -> "fallback")).isEqualTo("fallback");
	}

	@Test
	public void runWithFallbackBulkheadProviderAndGroupName() {
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(),
				new Resilience4jBulkheadProvider(ThreadPoolBulkheadRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
						new Resilience4JConfigurationProperties()),
				properties)
			.create("foo", "groupFoo");
		assertThat((String) cb.run(() -> {
			throw new RuntimeException("boom");
		}, t -> "fallback")).isEqualTo("fallback");
	}

	/**
	 * Run circuit breaker with default time limiter and expects everything to run without
	 * errors.
	 */
	@Test
	public void runWithDefaultTimeLimiter() {
		final TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				timeLimiterRegistry, null, properties)
			.create("foo");
		assertThat(cb.run(() -> {
			try {
				/* sleep less than time limit allows us to */
				TimeUnit.MILLISECONDS
					.sleep(Math.min(timeLimiterRegistry.getDefaultConfig().getTimeoutDuration().toMillis() / 2L, 0L));
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("thread got interrupted", e);
			}
			return "foobar";
		})).isEqualTo("foobar");
	}

	/**
	 * Run circuit breaker with default time limiter and expects the time limit to get
	 * exceeded.
	 */
	@Test(expected = NoFallbackAvailableException.class)
	public void runWithDefaultTimeLimiterTooSlow() {
		final TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				timeLimiterRegistry, null, properties)
			.create("foo");
		cb.run(() -> {
			try {
				/* sleep longer than time limit allows us to */
				TimeUnit.MILLISECONDS
					.sleep(Math.max(timeLimiterRegistry.getDefaultConfig().getTimeoutDuration().toMillis(), 100L) * 2);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError("thread got interrupted -> sleep failed", e);
			}
			return null;
		});
		Assertions.fail("timeout did not happen as expected");
	}

	/**
	 * Run circuit breaker with default time limiter and exceed time limit. Due to the
	 * disabled time limiter execution, everything should finish without errors.
	 */
	@Test
	public void runWithDisabledTimeLimiter() {
		properties.setDisableTimeLimiter(true);
		final TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				timeLimiterRegistry, null, properties)
			.create("foo");
		assertThat(cb.run(() -> {
			try {
				/* sleep longer than limit allows us to */
				TimeUnit.MILLISECONDS
					.sleep(Math.max(timeLimiterRegistry.getDefaultConfig().getTimeoutDuration().toMillis(), 100L) * 2);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("thread got interrupted", e);
			}
			return "foobar";
		})).isEqualTo("foobar");
	}

	/**
	 * Run circuit breaker with default time limiter map of two circuit breaker instances
	 * and exceed time limit. Due to the disabled time limiter execution, everything
	 * should finish without errors for foo2.
	 */
	@Test
	public void runWithDisabledTimeLimiterForASpecificInstance() {
		Map<String, Boolean> disableTimeLimiterMap = new HashMap<>();
		disableTimeLimiterMap.put("foo1", false);
		disableTimeLimiterMap.put("foo2", true);
		properties.setDisableTimeLimiter(true);
		final TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
		CircuitBreaker cb = new Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
				timeLimiterRegistry, null, properties)
			.create("foo2");
		assertThat(cb.run(() -> {
			try {
				/* sleep longer than limit allows us to */
				TimeUnit.MILLISECONDS
					.sleep(Math.max(timeLimiterRegistry.getDefaultConfig().getTimeoutDuration().toMillis(), 100L) * 2);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("thread got interrupted", e);
			}
			return "foobar";
		})).isEqualTo("foobar");
	}

	/**
	 * Run the test with grouping and specify thread pool.
	 */
	@Test
	public void runWithCustomGroupThreadPool() {
		Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(
				CircuitBreakerRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(), null);
		String groupName = "groupFoo";

		// configure GroupExecutorService
		factory.configureGroupExecutorService(group -> new ContextThreadPoolExecutor(groupName));

		CircuitBreaker cb = factory.create("foo", groupName);
		assertThat(cb.run(() -> Thread.currentThread().getName())).startsWith(groupName);
	}

	/**
	 * Run tests without grouping and specify thread pool.
	 */
	@Test
	public void runWithCustomNormalThreadPool() {
		Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(
				CircuitBreakerRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(), null);
		String threadPoolName = "demo-";

		// configure ExecutorService
		factory.configureExecutorService(new ContextThreadPoolExecutor(threadPoolName));

		CircuitBreaker cb = factory.create("foo");
		assertThat(cb.run(() -> Thread.currentThread().getName())).startsWith(threadPoolName);
	}

	static class ContextThreadPoolExecutor extends ThreadPoolExecutor {

		/**
		 * example ContextThreadPoolExecutor
		 * @param threadPoolName fixed threadPoolName
		 */
		ContextThreadPoolExecutor(String threadPoolName) {
			super(2, 5, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1024));
			this.setThreadFactory(new NamedThreadFactory(threadPoolName));
		}

	}

}
