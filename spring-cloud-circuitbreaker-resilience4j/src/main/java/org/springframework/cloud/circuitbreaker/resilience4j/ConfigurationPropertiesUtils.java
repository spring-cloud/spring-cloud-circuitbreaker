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

import java.util.Map;

/**
 *
 * A util class related to configuration properties class.
 *
 * @author Santosh Dahal
 */
public final class ConfigurationPropertiesUtils {

	private ConfigurationPropertiesUtils() {
	}

	/**
	 * Determines if the TimeLimiter should be disabled. First priority is operation or
	 * method,then service and if not found returns disable-time-limit.
	 * @param resilience4JConfigurationProperties configuration properties set
	 * @param id operation or method name
	 * @param groupName service group name
	 * @return value set for id, groupName or default value of disable-time-limit and
	 * {@code false} otherwise
	 */
	static boolean isDisableTimeLimiter(Resilience4JConfigurationProperties resilience4JConfigurationProperties,
			String id, String groupName) {

		if (resilience4JConfigurationProperties == null) {
			return false;
		}
		Map<String, Boolean> disableTimeLimiterMap = resilience4JConfigurationProperties.getDisableTimeLimiterMap();
		if (disableTimeLimiterMap.containsKey(id)) {
			return disableTimeLimiterMap.get(id);
		}
		if (disableTimeLimiterMap.containsKey(groupName)) {
			return disableTimeLimiterMap.get(groupName);
		}
		return resilience4JConfigurationProperties.isDisableTimeLimiter();
	}

}
