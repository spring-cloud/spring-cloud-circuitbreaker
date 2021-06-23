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
