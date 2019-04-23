/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.circuitbreaker.failsafe;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeExecutor;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.event.ExecutionAttemptedEvent;
import net.jodah.failsafe.function.CheckedFunction;

import org.springframework.cloud.circuitbreaker.commons.CircuitBreaker;
import org.springframework.cloud.circuitbreaker.commons.Customizer;

/**
 * @author Jakub Marchwicki
 */
public class FailsafeCircuitBreaker implements CircuitBreaker {

	private final String id;

	private final FailsafeConfigBuilder.FailsafeConfig config;

	private final Optional<Customizer<FailsafeExecutor>> failsafeCustomizer;

	public FailsafeCircuitBreaker(String id, FailsafeConfigBuilder.FailsafeConfig config,
			Optional<Customizer<FailsafeExecutor>> failsafeCustomizer) {
		this.id = id;
		this.config = config;
		this.failsafeCustomizer = failsafeCustomizer;
	}

	@Override
	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {

		Fallback<T> failsafeFallback = Fallback.of(extract(fallback));
		FailsafeExecutor<T> failsafeExecutor = Failsafe.with(failsafeFallback,
				config.getRetryPolicy(), config.getCircuitBreaker());

		failsafeCustomizer
				.ifPresent(customizer -> customizer.customize(failsafeExecutor));

		return failsafeExecutor.get(ex -> toRun.get());
	}

	private <T> CheckedFunction<ExecutionAttemptedEvent<? extends T>, ? extends T> extract(
			Function<Throwable, T> fallback) {
		return execution -> fallback.apply(execution.getLastFailure());
	}

}
