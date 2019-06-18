/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.circuitbreaker.sentinel;

import java.util.Arrays;
import java.util.Collections;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.circuitbreaker.commons.ReactorCircuitBreaker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Eric Zhao
 */
public class ReactorSentinelCircuitBreakerTest {

	@Test
	public void testCreateWithNullRule() {
		String id = "testCreateReactiveCbWithNullRule";
		ReactorSentinelCircuitBreaker cb = new ReactorSentinelCircuitBreaker(id,
				Collections.singletonList(null));
		assertThat(Mono.just("foobar").transform(it -> cb.run(it)).block())
				.isEqualTo("foobar");
		assertThat(DegradeRuleManager.hasConfig(id)).isFalse();
	}

	@Test
	public void runMono() {
		ReactorCircuitBreaker cb = new ReactorSentinelCircuitBreakerFactory()
				.createReactor("foo");
		assertThat(Mono.just("foobar").transform(it -> cb.run(it)).block())
				.isEqualTo("foobar");
	}

	@Test
	public void runMonoWithFallback() {
		ReactorCircuitBreaker cb = new ReactorSentinelCircuitBreakerFactory()
				.createReactor("foo");
		assertThat(Mono.error(new RuntimeException("boom"))
				.transform(it -> cb.run(it, t -> Mono.just("fallback"))).block())
						.isEqualTo("fallback");
	}

	@Test
	public void runFlux() {
		ReactorCircuitBreaker cb = new ReactorSentinelCircuitBreakerFactory()
				.createReactor("foo");
		assertThat(Flux.just("foobar", "hello world").transform(it -> cb.run(it))
				.collectList().block()).isEqualTo(Arrays.asList("foobar", "hello world"));
	}

	@Test
	public void runFluxWithFallback() {
		ReactorCircuitBreaker cb = new ReactorSentinelCircuitBreakerFactory()
				.createReactor("foo");
		assertThat(Flux.error(new RuntimeException("boom"))
				.transform(it -> cb.run(it, t -> Flux.just("fallback"))).collectList()
				.block()).isEqualTo(Arrays.asList("fallback"));
	}

}