package org.springframework.cloud.circuitbreaker.resilience4j;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/**
 * @author Ryan Baxter
 */
public interface CircuitBreakerCustomizer {
	void customize(CircuitBreaker circuitBreaker);
}
