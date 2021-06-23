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

package org.springframework.cloud.circuitbreaker.resilience4j.common;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.vavr.collection.Map;

public class Resilience4JTimeLimiterCompareAndGetter
	implements CompareAndGetter<TimeLimiter, TimeLimiterRegistry, TimeLimiterConfig> {

	private static Resilience4JTimeLimiterCompareAndGetter instance;

	public static Resilience4JTimeLimiterCompareAndGetter getInstance() {
		if (instance == null) {
			instance = new Resilience4JTimeLimiterCompareAndGetter();
		}
		return instance;
	}

	@Override
	public TimeLimiter compareAndGet(String id, TimeLimiterRegistry timeLimiterRegistry, TimeLimiterConfig timeLimiterConfig, Map<String, String> tags) {

		TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(id);
		if (timeLimiterConfig == null) {
			return timeLimiter;
		}

		// compare and get
		TimeLimiterConfig realConfig = timeLimiter.getTimeLimiterConfig();
		if (!realConfig.toString().equals(timeLimiterConfig.toString())) {
			timeLimiterRegistry.remove(id);
			timeLimiter = timeLimiterRegistry.timeLimiter(id, timeLimiterConfig, tags);
		}

		return timeLimiter;
	}
}
