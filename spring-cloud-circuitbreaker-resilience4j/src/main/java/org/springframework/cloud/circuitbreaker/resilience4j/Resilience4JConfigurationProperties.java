/*
 * Copyright 2013-2022 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ryan Baxter
 */
@ConfigurationProperties("spring.cloud.circuitbreaker.resilience4j")
public class Resilience4JConfigurationProperties {

	private boolean enableGroupMeterFilter = true;

	private String defaultGroupTag = "none";

	private boolean enableSemaphoreDefaultBulkhead = false;

	private boolean disableThreadPool = false;

	public boolean isEnableGroupMeterFilter() {
		return enableGroupMeterFilter;
	}

	public void setEnableGroupMeterFilter(boolean enableGroupMeterFilter) {
		this.enableGroupMeterFilter = enableGroupMeterFilter;
	}

	public String getDefaultGroupTag() {
		return defaultGroupTag;
	}

	public void setDefaultGroupTag(String defaultGroupTag) {
		this.defaultGroupTag = defaultGroupTag;
	}

	public boolean isEnableSemaphoreDefaultBulkhead() {
		return enableSemaphoreDefaultBulkhead;
	}

	public void setEnableSemaphoreDefaultBulkhead(boolean enableSemaphoreDefaultBulkhead) {
		this.enableSemaphoreDefaultBulkhead = enableSemaphoreDefaultBulkhead;
	}

	public boolean isDisableThreadPool() {
		return disableThreadPool;
	}

	public void setDisableThreadPool(boolean disableThreadPool) {
		this.disableThreadPool = disableThreadPool;
	}

}
