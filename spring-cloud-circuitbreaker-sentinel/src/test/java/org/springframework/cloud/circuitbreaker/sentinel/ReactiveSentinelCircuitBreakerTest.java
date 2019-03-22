/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.circuitbreaker.sentinel;

import java.util.Arrays;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreaker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Eric Zhao
 */
public class ReactiveSentinelCircuitBreakerTest {

	@Test
	public void runMono() {
		ReactiveCircuitBreaker cb = new ReactiveSentinelCircuitBreakerFactory()
				.create("foo");
		assertThat(cb.run(Mono.just("foobar")).block()).isEqualTo("foobar");
	}

	@Test
	public void runMonoWithFallback() {
		ReactiveCircuitBreaker cb = new ReactiveSentinelCircuitBreakerFactory()
				.create("foo");
		assertThat(cb
				.run(Mono.error(new RuntimeException("boom")), t -> Mono.just("fallback"))
				.block()).isEqualTo("fallback");
	}

	@Test
	public void runFlux() {
		ReactiveCircuitBreaker cb = new ReactiveSentinelCircuitBreakerFactory()
				.create("foo");
		assertThat(cb.run(Flux.just("foobar", "hello world")).collectList().block())
				.isEqualTo(Arrays.asList("foobar", "hello world"));
	}

	@Test
	public void runFluxWithFallback() {
		ReactiveCircuitBreaker cb = new ReactiveSentinelCircuitBreakerFactory()
				.create("foo");
		assertThat(cb
				.run(Flux.error(new RuntimeException("boom")), t -> Flux.just("fallback"))
				.collectList().block()).isEqualTo(Arrays.asList("fallback"));
	}

}
