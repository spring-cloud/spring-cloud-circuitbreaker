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
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import org.jspecify.annotations.Nullable;

/**
 * @author Andrii Bohutskyi
 */
public class Resilience4jBulkheadConfigurationBuilder {

	private BulkheadConfig bulkheadConfig = BulkheadConfig.ofDefaults();

	private ThreadPoolBulkheadConfig threadPoolBulkheadConfig = ThreadPoolBulkheadConfig.ofDefaults();

	public Resilience4jBulkheadConfigurationBuilder bulkheadConfig(@Nullable BulkheadConfig bulkheadConfig) {
		if (bulkheadConfig != null) {
			this.bulkheadConfig = bulkheadConfig;
		}
		return this;
	}

	public Resilience4jBulkheadConfigurationBuilder threadPoolBulkheadConfig(
			@Nullable ThreadPoolBulkheadConfig threadPoolBulkheadConfig) {
		if (threadPoolBulkheadConfig != null) {
			this.threadPoolBulkheadConfig = threadPoolBulkheadConfig;
		}
		return this;
	}

	public BulkheadConfiguration build() {
		BulkheadConfiguration configuration = new BulkheadConfiguration();
		configuration.setBulkheadConfig(this.bulkheadConfig);
		configuration.setThreadPoolBulkheadConfig(this.threadPoolBulkheadConfig);
		return configuration;
	}

	public static class BulkheadConfiguration {

		private @Nullable BulkheadConfig bulkheadConfig;

		private @Nullable ThreadPoolBulkheadConfig threadPoolBulkheadConfig;

		public void setBulkheadConfig(BulkheadConfig bulkheadConfig) {
			this.bulkheadConfig = bulkheadConfig;
		}

		public @Nullable ThreadPoolBulkheadConfig getThreadPoolBulkheadConfig() {
			return threadPoolBulkheadConfig;
		}

		public void setThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig threadPoolBulkheadConfig) {
			this.threadPoolBulkheadConfig = threadPoolBulkheadConfig;
		}

		public @Nullable BulkheadConfig getBulkheadConfig() {
			return bulkheadConfig;
		}

	}

}
