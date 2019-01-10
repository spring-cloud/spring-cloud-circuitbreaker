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
package org.springframework.cloud.circuitbreaker.r4j;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import org.springframework.cloud.circuitbreaker.commons.ConfigBuilder;

/**
 * @author Ryan Baxter
 */
public class R4JConfigBuilder implements ConfigBuilder<R4JConfigBuilder.R4JCircuitBreakerConfiguration> {

	private String id;
	private TimeLimiterConfig timeLimiterConfig;
	private CircuitBreakerConfig circuitBreakerConfig;

	public R4JConfigBuilder(String id) {
		this.id = id;
	}

	public R4JConfigBuilder timeLimiterConfig(TimeLimiterConfig config) {
		this.timeLimiterConfig = config;
		return this;
	}

	public R4JConfigBuilder circuitBreakerConfig(CircuitBreakerConfig circuitBreakerConfig) {
		this.circuitBreakerConfig = circuitBreakerConfig;
		return this;
	}

	@Override
	public R4JCircuitBreakerConfiguration build() {
		R4JCircuitBreakerConfiguration config = new R4JCircuitBreakerConfiguration();
		config.setId(id);
		//TODO null checks?
		config.setCircuitBreakerConfig(circuitBreakerConfig);
		config.setTimeLimiterConfig(timeLimiterConfig);
		return config;
	}

	public static class R4JCircuitBreakerConfiguration {
		private String id;
		private TimeLimiterConfig timeLimiterConfig;
		private CircuitBreakerConfig circuitBreakerConfig;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public TimeLimiterConfig getTimeLimiterConfig() {
			return timeLimiterConfig;
		}

		public void setTimeLimiterConfig(TimeLimiterConfig timeLimiterConfig) {
			this.timeLimiterConfig = timeLimiterConfig;
		}

		public CircuitBreakerConfig getCircuitBreakerConfig() {
			return circuitBreakerConfig;
		}

		public void setCircuitBreakerConfig(CircuitBreakerConfig circuitBreakerConfig) {
			this.circuitBreakerConfig = circuitBreakerConfig;
		}
	}
}
