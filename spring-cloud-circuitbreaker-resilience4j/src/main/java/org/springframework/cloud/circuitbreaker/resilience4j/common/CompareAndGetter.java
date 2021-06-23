package org.springframework.cloud.circuitbreaker.resilience4j.common;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.Registry;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;

/**
 * when using the config files to configure components like
 * 		{@link CircuitBreaker} etc. and then use the class
 * 		{@link Resilience4JCircuitBreakerFactory} to configure
 * 		these components will not take effect, because of the
 * 		logic to get components from registry depends on
 * 		[computeIfAbsent]
 *
 * this interface provide method compareAndGet:
 * 		1] get the component from register as A
 * 	    2] compare A's config with config that be configured by
 * 	    		{@link Resilience4JCircuitBreakerFactory}
 * 	    3] if not match, register new one and return
 *
 * @param <E> the element like {@link CircuitBreaker}, etc.
 * @param <R> the Registry for E
 * @param <C> the Config for E
 */
public interface CompareAndGetter<E, R extends Registry<E, C>, C> {

	E compareAndGet(String id, R register, C config, io.vavr.collection.Map<String, String> tags);
}
