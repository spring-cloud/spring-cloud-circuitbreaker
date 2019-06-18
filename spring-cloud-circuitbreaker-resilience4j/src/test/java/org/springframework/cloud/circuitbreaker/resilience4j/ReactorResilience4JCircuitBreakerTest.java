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

import java.util.Arrays;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreaker;
import org.springframework.cloud.circuitbreaker.commons.ReactorCircuitBreaker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 */
public class ReactorResilience4JCircuitBreakerTest {

	@Test
	public void runMono() {
		ReactorCircuitBreaker cb = new ReactorResilience4JCircuitBreakerFactory()
				.createReactor("foo");
		assertThat(Mono.just("foobar").transform(it -> cb.run(it)).block())
				.isEqualTo("foobar");
	}

	@Test
	public void runMonoWithFallback() {
		ReactorCircuitBreaker cb = new ReactorResilience4JCircuitBreakerFactory()
				.createReactor("foo");
		assertThat(Mono.error(new RuntimeException("boom"))
				.transform(it -> cb.run(it, t -> Mono.just("fallback"))).block())
						.isEqualTo("fallback");
	}

	@Test
	public void runFlux() {
		ReactorCircuitBreaker cb = new ReactorResilience4JCircuitBreakerFactory()
				.createReactor("foo");
		assertThat(Flux.just("foobar", "hello world").transform(it -> cb.run(it))
				.collectList().block()).isEqualTo(Arrays.asList("foobar", "hello world"));
	}

	@Test
	public void runFluxWithFallback() {
		ReactorCircuitBreaker cb = new ReactorResilience4JCircuitBreakerFactory()
				.createReactor("foo");
		assertThat(Flux.error(new RuntimeException("boom"))
				.transform(it -> cb.run(it, t -> Flux.just("fallback"))).collectList()
				.block()).isEqualTo(Arrays.asList("fallback"));
	}

	@Test
	public void runPublisher() {
		ReactiveCircuitBreaker cb = new ReactorResilience4JCircuitBreakerFactory()
				.createReactive("foo");
		assertThat(Flux.just("foobar", "hello world").transform(it -> cb.run(it))
				.collectList().block()).isEqualTo(Arrays.asList("foobar", "hello world"));
	}

	@Test
	public void runPublisherWithFallback() {
		ReactiveCircuitBreaker cb = new ReactorResilience4JCircuitBreakerFactory()
				.createReactive("foo");
		assertThat(Flux.error(new RuntimeException("boom"))
				.transform(it -> cb.run(it, t -> Flux.just("fallback"))).collectList()
				.block()).isEqualTo(Arrays.asList("fallback"));
	}

}
