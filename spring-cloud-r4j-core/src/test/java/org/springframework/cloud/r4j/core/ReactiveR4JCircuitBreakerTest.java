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
package org.springframework.cloud.r4j.core;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import org.junit.Test;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;

import static org.junit.Assert.assertEquals;


/**
 * @author Ryan Baxter
 */
public class ReactiveR4JCircuitBreakerTest {

	@Test
	public void runMono() {
		ReactiveCircuitBreaker cb = new ReactiveR4JCircuitBreakerFactory(new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults()).create("foo");
		assertEquals("foobar", cb.run(Mono.just("foobar")).block());
	}

	@Test
	public void runMonoWithFallback() {
		ReactiveCircuitBreaker cb = new ReactiveR4JCircuitBreakerFactory(new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults()).create("foo");
		assertEquals("fallback", cb.run(Mono.error(new RuntimeException("boom")), t -> Mono.just("fallback")).block());
	}

	@Test
	public void runFlux() {
		ReactiveCircuitBreaker cb = new ReactiveR4JCircuitBreakerFactory(new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults()).create("foo");
		assertEquals(Arrays.asList("foobar", "hello world"), cb.run(Flux.just("foobar", "hello world")).collectList().block());
	}

	@Test
	public void runFluxWithFallback() {
		ReactiveCircuitBreaker cb = new ReactiveR4JCircuitBreakerFactory(new R4JConfigFactory.DefaultR4JConfigFactory(),
				CircuitBreakerRegistry.ofDefaults()).create("foo");
		assertEquals(Arrays.asList("fallback"), cb.run(Flux.error(new RuntimeException("boom")), t -> Flux.just("fallback")).collectList().block());
	}

}