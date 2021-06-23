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

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.vavr.collection.Map;

/**
 * implement for ThreadPoolBulkhead
 * @author dangzhicairang
 */
public class Resilience4JThreadPoolBulkheadCompareAndGetter
	implements CompareAndGetter<ThreadPoolBulkhead, ThreadPoolBulkheadRegistry, ThreadPoolBulkheadConfig> {

	private static Resilience4JThreadPoolBulkheadCompareAndGetter instance;

	public static Resilience4JThreadPoolBulkheadCompareAndGetter getInstance() {
		if (instance == null) {
			instance = new Resilience4JThreadPoolBulkheadCompareAndGetter();
		}
		return instance;
	}

	@Override
	public ThreadPoolBulkhead compareAndGet(String id, ThreadPoolBulkheadRegistry bulkheadRegistry
			, ThreadPoolBulkheadConfig bulkheadConfig, Map<String, String> tags) {

		ThreadPoolBulkhead bulkhead = bulkheadRegistry.bulkhead(id);

		// compare and get
		ThreadPoolBulkheadConfig realConfig = bulkhead.getBulkheadConfig();
		if (!realConfig.toString().equals(bulkheadConfig.toString())) {
			bulkheadRegistry.remove(id);
			bulkhead = bulkheadRegistry.bulkhead(id, bulkheadConfig, tags);
		}

		return bulkhead;
	}
}
