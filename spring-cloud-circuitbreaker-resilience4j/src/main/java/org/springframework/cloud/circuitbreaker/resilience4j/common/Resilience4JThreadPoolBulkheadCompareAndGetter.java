package org.springframework.cloud.circuitbreaker.resilience4j.common;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.vavr.collection.Map;

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
		if (bulkheadConfig == null) {
			return bulkhead;
		}

		// compare and get
		ThreadPoolBulkheadConfig realConfig = bulkhead.getBulkheadConfig();
		if (!realConfig.toString().equals(bulkheadConfig.toString())) {
			bulkheadRegistry.remove(id);
			bulkhead = bulkheadRegistry.bulkhead(id, bulkheadConfig, tags);
		}

		return bulkhead;
	}
}
