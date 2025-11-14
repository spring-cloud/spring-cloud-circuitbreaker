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

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.core.retry.RetryPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link FrameworkRetryCircuitBreakerFactory}.
 *
 * @author Ryan Baxter
 */
class FrameworkRetryCircuitBreakerFactoryTest {

	@Test
	void testCreateCircuitBreaker() {
		FrameworkRetryCircuitBreakerFactory factory = new FrameworkRetryCircuitBreakerFactory();
		CircuitBreaker circuitBreaker = factory.create("test");

		assertThat(circuitBreaker).isNotNull();
		assertThat(circuitBreaker).isInstanceOf(FrameworkRetryCircuitBreaker.class);
		assertThat(((FrameworkRetryCircuitBreaker) circuitBreaker).getId()).isEqualTo("test");
	}

	@Test
	void testCreateCircuitBreakerRequiresId() {
		FrameworkRetryCircuitBreakerFactory factory = new FrameworkRetryCircuitBreakerFactory();

		assertThatThrownBy(() -> factory.create(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("circuit breaker must have an id");

		assertThatThrownBy(() -> factory.create("")).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("circuit breaker must have an id");
	}

	@Test
	void testConfigureDefault() {
		FrameworkRetryCircuitBreakerFactory factory = new FrameworkRetryCircuitBreakerFactory();

		factory.configureDefault(id -> new FrameworkRetryConfigBuilder(id).openTimeout(Duration.ofSeconds(30))
			.resetTimeout(Duration.ofSeconds(10))
			.build());

		CircuitBreaker circuitBreaker = factory.create("test");
		FrameworkRetryCircuitBreaker frameworkRetryCircuitBreaker = (FrameworkRetryCircuitBreaker) circuitBreaker;

		assertThat(frameworkRetryCircuitBreaker.getCircuitBreakerPolicy()).isNotNull();
	}

	@Test
	void testConfigureSpecificCircuitBreaker() {
		FrameworkRetryCircuitBreakerFactory factory = new FrameworkRetryCircuitBreakerFactory();

		factory.configure(
				builder -> builder.openTimeout(Duration.ofSeconds(20)).resetTimeout(Duration.ofSeconds(5)).build(),
				"test");

		CircuitBreaker circuitBreaker = factory.create("test");
		assertThat(circuitBreaker).isNotNull();

		// Create another circuit breaker with default config
		CircuitBreaker defaultCircuitBreaker = factory.create("default");
		assertThat(defaultCircuitBreaker).isNotNull();
	}

	@Test
	void testConfigBuilder() {
		FrameworkRetryCircuitBreakerFactory factory = new FrameworkRetryCircuitBreakerFactory();
		FrameworkRetryConfigBuilder builder = factory.configBuilder("test");

		assertThat(builder).isNotNull();

		FrameworkRetryConfig config = builder.retryPolicy(RetryPolicy.withMaxRetries(5))
			.openTimeout(Duration.ofSeconds(15))
			.resetTimeout(Duration.ofSeconds(3))
			.build();

		assertThat(config).isNotNull();
		assertThat(config.getRetryPolicy()).isNotNull();
		assertThat(config.getCircuitBreakerRetryPolicy()).isNotNull();
	}

}
