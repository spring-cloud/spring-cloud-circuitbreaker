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

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.lang.Nullable;

/**
 * Auto-configuration for Framework Retry circuit breaker.
 *
 * @author Ryan Baxter
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RetryTemplate.class)
public class FrameworkRetryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(CircuitBreakerFactory.class)
	public CircuitBreakerFactory<?, ?> frameworkRetryCircuitBreakerFactory(
			@Nullable List<Customizer<FrameworkRetryCircuitBreakerFactory>> customizers) {

		FrameworkRetryCircuitBreakerFactory factory = new FrameworkRetryCircuitBreakerFactory();
		if (customizers != null) {
			customizers.forEach(customizer -> customizer.customize(factory));
		}
		return factory;
	}

}
