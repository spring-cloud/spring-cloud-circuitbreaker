/*
 * Copyright 2013-2020 the original author or authors.
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

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Andrii Bohutskyi
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Resilience4JAutoConfigurationPropertyTest.class)
@EnableAutoConfiguration
@ActiveProfiles(profiles = "test-properties")
public class Resilience4JAutoConfigurationPropertyTest {

	@Autowired
	Resilience4JCircuitBreakerFactory factory;

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
	public void testThreadPoolBulkheadPropertiesPopulated() {
		ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = factory.getBulkheadProvider()
				.getThreadPoolBulkheadRegistry();
		assertThat(threadPoolBulkheadRegistry).isNotNull();
		assertThat(threadPoolBulkheadRegistry.find("test_circuit")).isPresent();
		assertThat(threadPoolBulkheadRegistry.find("test_circuit").get().getBulkheadConfig().getMaxThreadPoolSize())
				.isEqualTo(100);
	}

	@Test
	public void testBulkheadPropertiesPopulated() {
		BulkheadRegistry bulkheadRegistry = factory.getBulkheadProvider().getBulkheadRegistry();
		assertThat(bulkheadRegistry).isNotNull();
		assertThat(bulkheadRegistry.find("test_circuit")).isPresent();
		assertThat(bulkheadRegistry.find("test_circuit").get().getBulkheadConfig().getMaxConcurrentCalls())
				.isEqualTo(50);
	}

}
