package org.springframework.cloud.circuitbreaker.resilience4j;

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

import java.time.Duration;

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
		assertThat(circuitBreakerRegistry.find("test_circuit").get().getCircuitBreakerConfig().getMinimumNumberOfCalls())
			.isEqualTo(5);
	}

	@Test
	public void testThreadPoolBulkheadPropertiesPopulated() {
		ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = factory.getThreadPoolBulkheadRegistry();
		assertThat(threadPoolBulkheadRegistry).isNotNull();
		assertThat(threadPoolBulkheadRegistry.find("test_circuit")).isPresent();
		assertThat(threadPoolBulkheadRegistry.find("test_circuit").get().getBulkheadConfig().getCoreThreadPoolSize())
			.isEqualTo(20);
	}

	@Test
	public void testBulkheadPropertiesPopulated() {
		BulkheadRegistry bulkheadRegistry = factory.getBulkheadRegistry();
		assertThat(bulkheadRegistry).isNotNull();
		assertThat(bulkheadRegistry.find("test_circuit")).isPresent();
		assertThat(bulkheadRegistry.find("test_circuit").get().getBulkheadConfig().getMaxConcurrentCalls())
			.isEqualTo(12);
	}

	@Test
	public void testTimeLimiterPropertiesPopulated() {
		TimeLimiterRegistry timeLimiterRegistry = factory.getTimeLimiterRegistry();
		assertThat(timeLimiterRegistry).isNotNull();
		assertThat(timeLimiterRegistry.find("test_circuit")).isPresent();
		assertThat(timeLimiterRegistry.find("test_circuit").get().getTimeLimiterConfig().getTimeoutDuration())
			.isEqualTo(Duration.ofSeconds(18));
	}
}
