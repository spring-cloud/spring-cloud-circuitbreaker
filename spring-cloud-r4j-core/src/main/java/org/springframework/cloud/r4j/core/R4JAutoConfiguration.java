/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.r4j.core;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ryan Baxter
 */
@Configuration
public class R4JAutoConfiguration {
	@Bean
	@ConditionalOnMissingBean(CircuitBreakerRegistry.class)
	public CircuitBreakerRegistry circuitBreakerRegistry(R4JConfigFactory configFactory) {
		return CircuitBreakerRegistry.of(configFactory.getDefaultCircuitBreakerConfig());
	}

	@Bean
	@ConditionalOnMissingBean(R4JConfigFactory.class)
	public R4JConfigFactory r4jCircuitBreakerConfigFactory() {
			return new R4JConfigFactory.DefaultR4JConfigFactory();
	}

	@Bean
	@ConditionalOnMissingBean(ExecutorService.class)
	public ExecutorService r4jExecutorService() {
		return Executors.newSingleThreadExecutor();
	}

	@Bean
	@ConditionalOnMissingBean(CircuitBreakerFactory.class)
	public CircuitBreakerFactory r4jCircuitBreakerFactory(R4JConfigFactory r4JConfigFactory,
														  CircuitBreakerRegistry circuitBreakerRegistry,
														  ExecutorService executorService) {
		return new R4JCircuitBreakerFactory(r4JConfigFactory, circuitBreakerRegistry, executorService);
	}

	@Bean
	@ConditionalOnMissingBean(ReactiveCircuitBreakerFactory.class)
	@ConditionalOnClass(name = {"reactor.core.publisher.Mono", "reactor.core.publisher.Flux"})
	public ReactiveCircuitBreakerFactory reactiveR4JCircuitBreakerFactory(R4JConfigFactory r4JConfigFactory,
																		  CircuitBreakerRegistry circuitBreakerRegistry) {
		return new ReactiveR4JCircuitBreakerFactory(r4JConfigFactory, circuitBreakerRegistry);
	}

}
