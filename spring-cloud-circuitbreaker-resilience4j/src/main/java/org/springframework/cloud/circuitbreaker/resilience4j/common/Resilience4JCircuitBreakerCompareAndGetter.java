package org.springframework.cloud.circuitbreaker.resilience4j.common;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

public class Resilience4JCircuitBreakerCompareAndGetter
	implements CompareAndGetter<CircuitBreaker, CircuitBreakerRegistry, CircuitBreakerConfig> {

	private static Resilience4JCircuitBreakerCompareAndGetter instance;

	public static Resilience4JCircuitBreakerCompareAndGetter getInstance() {
		if (instance == null) {
			instance = new Resilience4JCircuitBreakerCompareAndGetter();
		}
		return instance;
	}

	@Override
	public CircuitBreaker compareAndGet(String id, CircuitBreakerRegistry circuitBreakerRegistry
		, CircuitBreakerConfig circuitBreakerConfig, io.vavr.collection.Map<String, String> tags) {

		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(id);
		if (circuitBreakerConfig == null) {
			return circuitBreaker;
		}

		// compare and get
		CircuitBreakerConfig realConfig = circuitBreaker.getCircuitBreakerConfig();
		if (!realConfig.toString().equals(circuitBreakerConfig.toString())) {
			circuitBreakerRegistry.remove(id);
			circuitBreaker = circuitBreakerRegistry.circuitBreaker(id, circuitBreakerConfig, tags);
		}

		return circuitBreaker;
	}
}
