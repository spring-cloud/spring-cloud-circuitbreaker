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

package org.springframework.cloud.circuitbreaker.resilience4j;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 */
public class BulkheadConfigurationTests {

	@Test
	void testConfigurationWithConfigProperties() {
		new WebApplicationContextRunner().withUserConfiguration(Application.class)
			.withPropertyValues("resilience4j.threadPoolBulkHead.configs.testme.queueCapacity=30")
			.run(context -> {
				final String id = "testme";
				Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory = context
					.getBean(Resilience4JCircuitBreakerFactory.class);
				ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = context
					.getBean(ThreadPoolBulkheadRegistry.class);
				final var circuitBreaker = resilience4JCircuitBreakerFactory.create(id);
				circuitBreaker.run(() -> "run-to-populate-registry");
				final var threadPoolBulkheadConfigOptional = threadPoolBulkheadRegistry.find(id)
					.map(ThreadPoolBulkhead::getBulkheadConfig);
				assertThat(threadPoolBulkheadConfigOptional.map(ThreadPoolBulkheadConfig::getQueueCapacity))
					.contains(30);
			});

	}

	@Test
	void testInstanceConfigurationOverridesConfigProperties() {
		new WebApplicationContextRunner().withUserConfiguration(Application.class)
			.withPropertyValues("resilience4j.threadPoolBulkHead.configs.testme.queueCapacity=30",
					"resilience4j.threadPoolBulkHead.instances.testme.queueCapacity=40")
			.run(context -> {
				final String id = "testme";
				Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory = context
					.getBean(Resilience4JCircuitBreakerFactory.class);
				ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = context
					.getBean(ThreadPoolBulkheadRegistry.class);
				final var circuitBreaker = resilience4JCircuitBreakerFactory.create(id);
				circuitBreaker.run(() -> "run-to-populate-registry");
				final var threadPoolBulkheadConfigOptional = threadPoolBulkheadRegistry.find(id)
					.map(ThreadPoolBulkhead::getBulkheadConfig);
				assertThat(threadPoolBulkheadConfigOptional.map(ThreadPoolBulkheadConfig::getQueueCapacity))
					.contains(40);
			});

	}

	@Test
	void testInstanceConfigurationOverridesConfigAndCustomizerProperties() {
		new WebApplicationContextRunner().withUserConfiguration(Application.class)
			.withPropertyValues("resilience4j.threadPoolBulkHead.configs.testme.queueCapacity=30",
					"resilience4j.threadPoolBulkHead.instances.testme.queueCapacity=40")
			.run(context -> {
				final String id = "testme";
				Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory = context
					.getBean(Resilience4JCircuitBreakerFactory.class);
				Resilience4jBulkheadProvider bulkheadProvider = context.getBean(Resilience4jBulkheadProvider.class);
				bulkheadProvider.configure(builder -> {
					ThreadPoolBulkheadConfig threadPoolBulkheadConfig = ThreadPoolBulkheadConfig.custom()
						.queueCapacity(50)
						.build();
					builder.threadPoolBulkheadConfig(threadPoolBulkheadConfig);
				}, id);
				ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = context
					.getBean(ThreadPoolBulkheadRegistry.class);
				final var circuitBreaker = resilience4JCircuitBreakerFactory.create(id);
				circuitBreaker.run(() -> "run-to-populate-registry");
				final var threadPoolBulkheadConfigOptional = threadPoolBulkheadRegistry.find(id)
					.map(ThreadPoolBulkhead::getBulkheadConfig);
				assertThat(threadPoolBulkheadConfigOptional.map(ThreadPoolBulkheadConfig::getQueueCapacity))
					.contains(40);
			});

	}

	@Test
	void testCustomizerConfigurationOverridesConfigProperties() {
		new WebApplicationContextRunner().withUserConfiguration(Application.class)
			.withPropertyValues("resilience4j.threadPoolBulkHead.configs.testme.queueCapacity=30")
			.run(context -> {
				final String id = "testme";
				Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory = context
					.getBean(Resilience4JCircuitBreakerFactory.class);
				Resilience4jBulkheadProvider bulkheadProvider = context.getBean(Resilience4jBulkheadProvider.class);
				bulkheadProvider.configure(builder -> {
					ThreadPoolBulkheadConfig threadPoolBulkheadConfig = ThreadPoolBulkheadConfig.custom()
						.queueCapacity(50)
						.build();
					builder.threadPoolBulkheadConfig(threadPoolBulkheadConfig);
				}, id);
				ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = context
					.getBean(ThreadPoolBulkheadRegistry.class);
				final var circuitBreaker = resilience4JCircuitBreakerFactory.create(id);
				circuitBreaker.run(() -> "run-to-populate-registry");
				final var threadPoolBulkheadConfigOptional = threadPoolBulkheadRegistry.find(id)
					.map(ThreadPoolBulkhead::getBulkheadConfig);
				assertThat(threadPoolBulkheadConfigOptional.map(ThreadPoolBulkheadConfig::getQueueCapacity))
					.contains(50);
			});

	}

	@Test
	void useThreadPoolWithSemaphorePropertySet() {
		new WebApplicationContextRunner().withUserConfiguration(Application.class)
			.withPropertyValues("resilience4j.threadPoolBulkHead.instances.testme.queueCapacity=30",
					"spring.cloud.circuitbreaker.resilience4j.enableSemaphoreDefaultBulkhead=true")
			.run(context -> {
				final String id = "testme";
				Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory = context
					.getBean(Resilience4JCircuitBreakerFactory.class);
				ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = context
					.getBean(ThreadPoolBulkheadRegistry.class);
				BulkheadRegistry bulkheadRegistry = context.getBean(BulkheadRegistry.class);
				final var circuitBreaker = resilience4JCircuitBreakerFactory.create(id);
				circuitBreaker.run(() -> "run-to-populate-registry");
				final var threadPoolBulkheadConfigOptional = threadPoolBulkheadRegistry.find(id);
				final var semaphoreBulkheadConfig = bulkheadRegistry.find(id);
				assertThat(threadPoolBulkheadConfigOptional.isEmpty()).isFalse();
				assertThat(semaphoreBulkheadConfig.isEmpty()).isTrue();
			});

	}

	@Test
	void useSemaphoreWithoutPropertySet() {
		new WebApplicationContextRunner().withUserConfiguration(Application.class)
			.withPropertyValues("resilience4j.bulkhead.instances.testme.max-concurrent-calls=30")
			.run(context -> {
				final String id = "testme";
				Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory = context
					.getBean(Resilience4JCircuitBreakerFactory.class);
				ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = context
					.getBean(ThreadPoolBulkheadRegistry.class);
				BulkheadRegistry bulkheadRegistry = context.getBean(BulkheadRegistry.class);
				final var circuitBreaker = resilience4JCircuitBreakerFactory.create(id);
				circuitBreaker.run(() -> "run-to-populate-registry");
				final var threadPoolBulkheadConfigOptional = threadPoolBulkheadRegistry.find(id);
				final var semaphoreBulkheadConfig = bulkheadRegistry.find(id);
				assertThat(threadPoolBulkheadConfigOptional.isEmpty()).isTrue();
				assertThat(semaphoreBulkheadConfig.isEmpty()).isFalse();
			});

	}

	@Test
	void configureDefaultOverridesPropertyDefaultForThreadpool() {
		new WebApplicationContextRunner().withUserConfiguration(Application.class)
			.withPropertyValues("resilience4j.bulkhead.config.default.max-concurrent-calls=30",
					"resilience4j.threadpool.config.default.queueCapacity=30"/*
																				 * ,
																				 * "resilience4j.bulkhead.instances.testme.max-wait-duration=30s",
																				 * "resilience4j.threadPoolBulkHead.instances.testme.core-threadpool-size=1"
																				 */)
			.run(context -> {
				final String id = "testme";
				Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory = context
					.getBean(Resilience4JCircuitBreakerFactory.class);
				Resilience4jBulkheadProvider bulkheadProvider = context.getBean(Resilience4jBulkheadProvider.class);
				bulkheadProvider.configureDefault(bulkheadId -> new Resilience4jBulkheadConfigurationBuilder()
					.bulkheadConfig(BulkheadConfig.custom().maxConcurrentCalls(40).build())
					.threadPoolBulkheadConfig(ThreadPoolBulkheadConfig.custom().queueCapacity(40).build())
					.build());
				ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = context
					.getBean(ThreadPoolBulkheadRegistry.class);
				final var circuitBreaker = resilience4JCircuitBreakerFactory.create(id);
				circuitBreaker.run(() -> "run-to-populate-registry");
				final var threadPoolBulkheadConfigOptional = threadPoolBulkheadRegistry.find(id);
				assertThat(threadPoolBulkheadConfigOptional.isEmpty()).isFalse();
				assertThat(threadPoolBulkheadConfigOptional.get().getBulkheadConfig().getQueueCapacity()).isEqualTo(40);
			});

	}

	@Test
	void configureDefaultOverridesPropertyDefaultForSemaphore() {
		new WebApplicationContextRunner().withUserConfiguration(Application.class)
			.withPropertyValues("resilience4j.bulkhead.config.default.max-concurrent-calls=30",
					"spring.cloud.circuitbreaker.resilience4j.enableSemaphoreDefaultBulkhead=true"/*
																									 * ,
																									 * "resilience4j.bulkhead.instances.testme.max-wait-duration=30s",
																									 * "resilience4j.threadPoolBulkHead.instances.testme.core-threadpool-size=1"
																									 */)
			.run(context -> {
				final String id = "testme";
				Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory = context
					.getBean(Resilience4JCircuitBreakerFactory.class);
				Resilience4jBulkheadProvider bulkheadProvider = context.getBean(Resilience4jBulkheadProvider.class);
				bulkheadProvider.configureDefault(bulkheadId -> new Resilience4jBulkheadConfigurationBuilder()
					.bulkheadConfig(BulkheadConfig.custom().maxConcurrentCalls(40).build())
					.threadPoolBulkheadConfig(ThreadPoolBulkheadConfig.custom().queueCapacity(40).build())
					.build());
				BulkheadRegistry bulkheadRegistry = context.getBean(BulkheadRegistry.class);
				final var circuitBreaker = resilience4JCircuitBreakerFactory.create(id);
				circuitBreaker.run(() -> "run-to-populate-registry");
				final var semaphoreBulkheadConfig = bulkheadRegistry.find(id);
				assertThat(semaphoreBulkheadConfig.isEmpty()).isFalse();
				assertThat(semaphoreBulkheadConfig.get().getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(40);
			});

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	protected static class Application {

	}

}
