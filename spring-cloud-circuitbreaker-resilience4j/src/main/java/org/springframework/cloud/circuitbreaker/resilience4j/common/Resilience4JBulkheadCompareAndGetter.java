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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.vavr.collection.Map;

public class Resilience4JBulkheadCompareAndGetter
	implements CompareAndGetter<Bulkhead, BulkheadRegistry, BulkheadConfig> {

	private static Resilience4JBulkheadCompareAndGetter instance;

	public static Resilience4JBulkheadCompareAndGetter getInstance() {
		if (instance == null) {
			instance = new Resilience4JBulkheadCompareAndGetter();
		}
		return instance;
	}

	@Override
	public Bulkhead compareAndGet(String id, BulkheadRegistry bulkheadRegistry, BulkheadConfig bulkheadConfig, Map<String, String> tags) {
		Bulkhead bulkhead = bulkheadRegistry.bulkhead(id);
		if (bulkheadConfig == null) {
			return bulkhead;
		}

		// compare and get
		BulkheadConfig realConfig = bulkhead.getBulkheadConfig();
		if (!realConfig.toString().equals(bulkheadConfig.toString())) {
			bulkheadRegistry.remove(id);
			bulkhead = bulkheadRegistry.bulkhead(id, bulkheadConfig, tags);
		}

		return bulkhead;
	}
}
