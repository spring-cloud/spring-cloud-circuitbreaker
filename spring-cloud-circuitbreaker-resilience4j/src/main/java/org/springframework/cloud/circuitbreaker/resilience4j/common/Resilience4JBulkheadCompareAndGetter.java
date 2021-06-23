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
