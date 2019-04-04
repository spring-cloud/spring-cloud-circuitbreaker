package org.springframework.cloud.circuitbreaker.springretry;

import org.junit.Test;

import org.springframework.cloud.circuitbreaker.commons.CircuitBreaker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 */
public class SpringRetryCircuitBreakerTest {

	@Test
	public void testCreate() {
		CircuitBreaker cb = new SpringRetryCircuitBreakerFactory().create("foo");
		assertThat(cb.run(() -> {
			return "foo";
		})).isEqualTo("foo");
	}
}
