/*
 * Copyright 2013-2021 the original author or authors.
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

import java.time.Duration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Thomas Vitale
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ReactiveResilience4JAutoConfigurationPropertyTest.class)
@EnableAutoConfiguration
@ActiveProfiles(profiles = "test-properties")
public class ReactiveResilience4JAutoConfigurationPropertyTest {

	@Autowired
	ReactiveResilience4JCircuitBreakerFactory factory;

	@Test
	public void testCircuitBreakerPropertiesPopulated() {
		CircuitBreakerRegistry circuitBreakerRegistry = factory.getCircuitBreakerRegistry();
		assertThat(circuitBreakerRegistry).isNotNull();
		assertThat(circuitBreakerRegistry.find("test_circuit")).isPresent();
		assertThat(
				circuitBreakerRegistry.find("test_circuit").get().getCircuitBreakerConfig().getMinimumNumberOfCalls())
						.isEqualTo(5);
	}

	@Test
	public void testTimeLimiterPropertiesPopulated() {
		TimeLimiterRegistry timeLimiterRegistry = factory.getTimeLimiterRegistry();
		assertThat(timeLimiterRegistry).isNotNull();
		assertThat(timeLimiterRegistry.find("test_circuit")).isPresent();
		assertThat(timeLimiterRegistry.find("test_circuit").get().getTimeLimiterConfig().getTimeoutDuration())
				.isEqualTo(Duration.ofSeconds(18));
	}

	@Test
	public void testDefaultCircuitBreakerPropertiesPopulated() {
		factory.create("default_circuitBreaker").run(Mono.just("result"));
		CircuitBreakerRegistry circuitBreakerRegistry = factory.getCircuitBreakerRegistry();
		assertThat(circuitBreakerRegistry).isNotNull();
		assertThat(circuitBreakerRegistry.find("default_circuitBreaker")).isPresent();
		assertThat(circuitBreakerRegistry.find("default_circuitBreaker").get().getCircuitBreakerConfig()
				.getMinimumNumberOfCalls()).isEqualTo(20);
	}

	@Test
	public void testDefaultTimeLimiterPropertiesPopulated() {
		factory.create("default_circuitBreaker").run(Mono.just("result"));
		TimeLimiterRegistry timeLimiterRegistry = factory.getTimeLimiterRegistry();
		assertThat(timeLimiterRegistry).isNotNull();
		assertThat(timeLimiterRegistry.find("default_circuitBreaker")).isPresent();
		assertThat(timeLimiterRegistry.find("default_circuitBreaker").get().getTimeLimiterConfig().getTimeoutDuration())
				.isEqualTo(Duration.ofMillis(150));
	}

}
