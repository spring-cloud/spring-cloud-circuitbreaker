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

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ryan Baxter
 */
class SpringRetryCircuitBreakerTest {

	@Test
	void testCreate() {
		CircuitBreaker cb = new SpringRetryCircuitBreakerFactory().create("foo");
		assertThat(cb.run(() -> "foo")).isEqualTo("foo");
	}

	@Test
	void testFallback() {
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
		assertThat(cb.run(spyedSup, t -> "fallback")).isEqualTo("fallback");
		// This will only be called 3 times because the SimpleRetryPolicy will trip the
		// circuit after the 3rd attempt.
		verify(spyedSup, times(3)).get();
	}

	@Test
	public void testRetryCustomizer() {
		SpringRetryCircuitBreakerFactory factory = new SpringRetryCircuitBreakerFactory();
		CustomListener listener = new CustomListener();
		factory.addRetryTemplateCustomizers(rt -> rt.setListeners(new RetryListener[] { listener }), "with-customizer");

		String result = factory.create("with-customizer").run(() -> "foo");
		assertThat(result).isEqualTo("foo");
		assertThat(listener.toCheck[0]).isEqualTo("check-me-please");
	}

	private static class CustomListener implements RetryListener {

		private final String[] toCheck = new String[1];

		@Override
		public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
			return true;
		}

		@Override
		public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
				Throwable throwable) {
			toCheck[0] = "check-me-please";
		}

		@Override
		public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
				Throwable throwable) {

		}

	}

}
