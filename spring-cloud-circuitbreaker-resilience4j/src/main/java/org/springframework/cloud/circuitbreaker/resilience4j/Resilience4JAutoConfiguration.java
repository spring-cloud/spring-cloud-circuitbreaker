/*
 * Copyright 2013-2018 the original author or authors.
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

package org.springframework.cloud.circuitbreaker.resilience4j;

import java.util.ArrayList;
import java.util.List;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ryan Baxter
 * @author Eric Bussieres
 * @author Andrii Bohutskyi
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = { "spring.cloud.circuitbreaker.resilience4j.enabled",
		"spring.cloud.circuitbreaker.resilience4j.blocking.enabled" }, matchIfMissing = true)
public class Resilience4JAutoConfiguration {

	@Autowired(required = false)
	private List<Customizer<Resilience4JCircuitBreakerFactory>> customizers = new ArrayList<>();

	@Bean
	@ConditionalOnMissingBean(CircuitBreakerFactory.class)
	public Resilience4JCircuitBreakerFactory resilience4jCircuitBreakerFactory(CircuitBreakerRegistry circuitBreakerRegistry,
																			   ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
																			   BulkheadRegistry bulkheadRegistry,
																			   TimeLimiterRegistry timeLimiterRegistry) {
		Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(circuitBreakerRegistry,
			threadPoolBulkheadRegistry, bulkheadRegistry, timeLimiterRegistry);
		customizers.forEach(customizer -> customizer.customize(factory));
		return factory;
	}
}
