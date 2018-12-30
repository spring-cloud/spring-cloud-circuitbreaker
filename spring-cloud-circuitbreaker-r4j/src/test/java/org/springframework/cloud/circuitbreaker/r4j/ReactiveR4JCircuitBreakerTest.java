/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.circuitbreaker.r4j;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreaker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Ryan Baxter
 */
public class ReactiveR4JCircuitBreakerTest {

	@Test
	public void runMono() {
		ReactiveCircuitBreaker cb = new ReactiveR4JCircuitBreakerFactory(
				new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults(), R4JCircuitBreakerCustomizer.NO_OP)
						.create("foo");
		assertEquals("foobar", cb.run(Mono.just("foobar")).block());
	}

	@Test
	public void runMonoWithFallback() {
		ReactiveCircuitBreaker cb = new ReactiveR4JCircuitBreakerFactory(
				new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults(), R4JCircuitBreakerCustomizer.NO_OP)
						.create("foo");
		assertEquals("fallback", cb
				.run(Mono.error(new RuntimeException("boom")), t -> Mono.just("fallback"))
				.block());
	}

	@Test
	public void customizeEventPublisherMono() {
		AtomicBoolean isCalled = new AtomicBoolean(false);
		R4JCircuitBreakerCustomizer customizer = circuitBreaker -> circuitBreaker
				.getEventPublisher().onSuccess(event -> isCalled.set(true));
		ReactiveCircuitBreaker cb = new ReactiveR4JCircuitBreakerFactory(
				new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults(), customizer).create("foo");
		assertEquals("foobar", cb.run(Mono.just("foobar")).block());
		assertTrue(isCalled.get());
	}

	@Test
	public void runFlux() {
		ReactiveCircuitBreaker cb = new ReactiveR4JCircuitBreakerFactory(
				new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults(), R4JCircuitBreakerCustomizer.NO_OP)
						.create("foo");
		assertEquals(Arrays.asList("foobar", "hello world"),
				cb.run(Flux.just("foobar", "hello world")).collectList().block());
	}

	@Test
	public void runFluxWithFallback() {
		ReactiveCircuitBreaker cb = new ReactiveR4JCircuitBreakerFactory(
				new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults(), R4JCircuitBreakerCustomizer.NO_OP)
						.create("foo");
		assertEquals(Arrays.asList("fallback"), cb
				.run(Flux.error(new RuntimeException("boom")), t -> Flux.just("fallback"))
				.collectList().block());
	}

	@Test
	public void customizeEventPublisherFlux() {
		AtomicBoolean isCalled = new AtomicBoolean(false);
		R4JCircuitBreakerCustomizer customizer = circuitBreaker -> circuitBreaker
				.getEventPublisher().onSuccess(event -> isCalled.set(true));
		ReactiveCircuitBreaker cb = new ReactiveR4JCircuitBreakerFactory(
				new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults(), customizer).create("foo");
		assertEquals(Arrays.asList("foobar", "hello world"),
				cb.run(Flux.just("foobar", "hello world")).collectList().block());
		assertTrue(isCalled.get());
	}
}