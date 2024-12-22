/*
 * Copyright 2013-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 * @author Thomas Vitale
 * @author Yavor Chamov
 */
public class ReactiveResilience4JCircuitBreakerTest {

	@Test
	public void runMono() {
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(), new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults()), new Resilience4JConfigurationProperties())
			.create("foo");
		assertThat(Mono.just("foobar").transform(cb::run).block()).isEqualTo("foobar");
	}

	@Test
	public void runMonoWithFallback() {
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(), new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults()), new Resilience4JConfigurationProperties())
			.create("foo");
		assertThat(Mono.error(new RuntimeException("boom"))
			.transform(it -> cb.run(it, t -> Mono.just("fallback")))
			.block()).isEqualTo("fallback");
	}

	@Test
	public void runFlux() {
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(), new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults()), new Resilience4JConfigurationProperties())
			.create("foo");
		assertThat(Flux.just("foobar", "hello world").transform(cb::run).collectList().block())
			.isEqualTo(Arrays.asList("foobar", "hello world"));
	}

	@Test
	public void runFluxWithFallback() {
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(), new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults()), new Resilience4JConfigurationProperties())
			.create("foo");
		assertThat(Flux.error(new RuntimeException("boom"))
			.transform(it -> cb.run(it, t -> Flux.just("fallback")))
			.collectList()
			.block()).isEqualTo(Collections.singletonList("fallback"));
	}

	/**
	 * Run circuit breaker with default time limiter and expects everything to run without
	 * errors.
	 */
	@Test
	public void runWithDefaultTimeLimiter() {
		final TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
			timeLimiterRegistry, new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults()), new Resilience4JConfigurationProperties())
			.create("foo");

		assertThat(Mono.fromCallable(() -> {
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
		}).subscribeOn(Schedulers.single()).transform(cb::run).block()).isEqualTo("foobar");
	}

	/**
	 * Run circuit breaker with default time limiter and expects the time limit to get
	 * exceeded.
	 */
	@Test(expected = NoFallbackAvailableException.class)
	public void runWithDefaultTimeLimiterTooSlow() {
		final TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
			timeLimiterRegistry, new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults()), new Resilience4JConfigurationProperties())
			.create("foo");

		Mono.fromCallable(() -> {
			try {
				/* sleep longer than time limit allows us to */
				TimeUnit.MILLISECONDS
					.sleep(Math.max(timeLimiterRegistry.getDefaultConfig().getTimeoutDuration().toMillis(), 100L) * 2);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("thread got interrupted", e);
			}
			return "foobar";
		}).subscribeOn(Schedulers.single()).transform(cb::run).doOnSuccess(s -> {
			throw new AssertionError("timeout did not occur");
		}).block();

		Assert.fail("execution did not cause exception");
	}

	/**
	 * Run circuit breaker with default time limiter and exceed time limit. Due to the
	 * disabled time limiter execution, everything should finish without errors.
	 */
	@Test
	public void runWithDisabledTimeLimiter() {
		final TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
		final Resilience4JConfigurationProperties resilience4JConfigurationProperties = new Resilience4JConfigurationProperties();
		resilience4JConfigurationProperties.setDisableTimeLimiter(true);
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
			timeLimiterRegistry, new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults()), resilience4JConfigurationProperties)
			.create("foo");

		assertThat(Mono.fromCallable(() -> {
			try {
				/* sleep longer than timit limit allows us to */
				TimeUnit.MILLISECONDS
					.sleep(Math.max(timeLimiterRegistry.getDefaultConfig().getTimeoutDuration().toMillis(), 100L) * 2);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("thread got interrupted", e);
			}
			return "foobar";
		}).subscribeOn(Schedulers.single()).transform(cb::run).block()).isEqualTo("foobar");
	}

	@Test
	public void runMonoWithBulkheadProvider() {
		ReactiveResilience4jBulkheadProvider bulkheadProvider = new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults());
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(
			CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(),
			bulkheadProvider,
			new Resilience4JConfigurationProperties()
		).create("foo");

		assertThat(Mono.just("bulkheadMono")
			.transform(cb::run)
			.block())
			.isEqualTo("bulkheadMono");
	}

	@Test
	public void runMonoWithBulkheadProviderAndFallback() {
		ReactiveResilience4jBulkheadProvider bulkheadProvider = new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults());
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(
			CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(),
			bulkheadProvider,
			new Resilience4JConfigurationProperties()
		).create("foo");

		assertThat(Mono.error(new RuntimeException("exception"))
			.transform(it -> cb.run(it, t -> Mono.just("bulkheadFallback")))
			.block())
			.isEqualTo("bulkheadFallback");
	}

	@Test
	public void runFluxWithBulkheadProvider() {
		ReactiveResilience4jBulkheadProvider bulkheadProvider = new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults());
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(
			CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(),
			bulkheadProvider,
			new Resilience4JConfigurationProperties()
		).create("foo");

		assertThat(Flux.just("bulkheadFlux1", "bulkheadFlux2")
			.transform(cb::run)
			.collectList()
			.block())
			.isEqualTo(Arrays.asList("bulkheadFlux1", "bulkheadFlux2"));
	}

	@Test
	public void runFluxWithBulkheadProviderAndFallback() {
		ReactiveResilience4jBulkheadProvider bulkheadProvider = new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults());
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(
			CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(),
			bulkheadProvider,
			new Resilience4JConfigurationProperties()
		).create("foo");

		assertThat(Flux.error(new RuntimeException("exception"))
			.transform(it -> cb.run(it, t -> Flux.just("bulkheadFallbackFlux")))
			.collectList()
			.block())
			.isEqualTo(Collections.singletonList("bulkheadFallbackFlux"));
	}

	@Test
	public void runMonoWithoutBulkheadProvider() {
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(
			CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(),
			null,
			new Resilience4JConfigurationProperties()
		).create("foo");

		assertThat(Mono.just("noBulkheadMono")
			.transform(cb::run)
			.block())
			.isEqualTo("noBulkheadMono");
	}

	@Test
	public void runMonoWithoutBulkheadProviderWithFallback() {
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(
			CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(),
			null,
			new Resilience4JConfigurationProperties()
		).create("foo");

		assertThat(Mono.error(new RuntimeException("exception"))
			.transform(it -> cb.run(it, t -> Mono.just("noBulkheadFallback")))
			.block())
			.isEqualTo("noBulkheadFallback");
	}

	@Test
	public void runFluxWithoutBulkheadProvider() {
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(
			CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(),
			null,
			new Resilience4JConfigurationProperties()
		).create("foo");

		assertThat(Flux.just("noBulkheadFlux1", "noBulkheadFlux2")
			.transform(cb::run)
			.collectList()
			.block())
			.isEqualTo(Arrays.asList("noBulkheadFlux1", "noBulkheadFlux2"));
	}

	@Test
	public void runFluxWithoutBulkheadProviderWithFallback() {
		ReactiveCircuitBreaker cb = new ReactiveResilience4JCircuitBreakerFactory(
			CircuitBreakerRegistry.ofDefaults(),
			TimeLimiterRegistry.ofDefaults(),
			null,
			new Resilience4JConfigurationProperties()
		).create("foo");

		assertThat(Flux.error(new RuntimeException("boom"))
			.transform(it -> cb.run(it, t -> Flux.just("noBulkheadFallbackFlux")))
			.collectList()
			.block())
			.isEqualTo(Collections.singletonList("noBulkheadFallbackFlux"));
	}
}
