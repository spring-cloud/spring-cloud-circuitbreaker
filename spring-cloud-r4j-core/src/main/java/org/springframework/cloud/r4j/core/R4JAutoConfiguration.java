package org.springframework.cloud.r4j.core;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerBuilder;
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
	@ConditionalOnMissingBean(CircuitBreakerBuilder.class)
	public CircuitBreakerBuilder r4jCircuitBreakerBuilder(R4JConfigFactory r4JConfigFactory,
														  CircuitBreakerRegistry circuitBreakerRegistry,
														  ExecutorService executorService) {
		return new R4JCircuitBreakerBuilder(r4JConfigFactory, circuitBreakerRegistry, executorService);
	}
}
