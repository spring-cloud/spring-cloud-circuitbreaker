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

package org.springframework.cloud.circuitbreaker.springretry;

import java.util.function.Supplier;

import org.junit.Test;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ryan Baxter
 */
public class SpringRetryCircuitBreakerTest {

	@Test
	public void testCreate() {
		CircuitBreaker cb = new SpringRetryCircuitBreakerFactory().create("foo");
		assertThat(cb.run(() -> "foo")).isEqualTo("foo");
	}

	@Test
	public void testFallback() {
		SpringRetryCircuitBreakerFactory factory = new SpringRetryCircuitBreakerFactory();
		Supplier<String> spyedSup = spy(new Supplier<String>() {
			@Override
			public String get() {
				throw new RuntimeException("boom");
			}
		});
		CircuitBreaker cb = factory.create("foo");
		for (int i = 0; i < 10; i++) {
			cb.run(spyedSup, t -> "fallback");
		}
		assertThat((String) cb.run(spyedSup, t -> "fallback")).isEqualTo("fallback");
		// This will only be called 3 times because the SimpleRetryPolicy will trip the
		// circuit after the
		// 3rd attempt.
		verify(spyedSup, times(3)).get();
	}

}
