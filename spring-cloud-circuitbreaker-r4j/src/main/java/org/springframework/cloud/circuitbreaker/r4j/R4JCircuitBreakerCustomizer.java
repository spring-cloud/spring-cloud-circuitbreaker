package org.springframework.cloud.circuitbreaker.r4j;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/**
 * Callback interface that can be used to customize a R4j {@link CircuitBreaker}.
 *
 * @author Toshiaki Maki
 */
@FunctionalInterface
public interface R4JCircuitBreakerCustomizer {

	/**
	 * Customize the circuitBreaker.
	 *
	 * @param circuitBreaker the circuit breaker to customize
	 */
	void customize(CircuitBreaker circuitBreaker);

	/**
	 * Default customize that does nothing.
	 */
	R4JCircuitBreakerCustomizer NO_OP = circuitBreaker -> {
	};
}
