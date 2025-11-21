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

package org.springframework.cloud.circuitbreaker.springretry;

import org.jspecify.annotations.Nullable;

import org.springframework.classify.Classifier;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;

/**
 * @author Ryan Baxter
 */
public class SpringRetryConfig {

	private @Nullable String id;

	private @Nullable BackOffPolicy backOffPolicy;

	private @Nullable RetryPolicy retryPolicy;

	private boolean forceRefreshState;

	private @Nullable Classifier<Throwable, Boolean> stateClassifier;

	boolean isForceRefreshState() {
		return forceRefreshState;
	}

	SpringRetryConfig setForceRefreshState(boolean forceRefreshState) {
		this.forceRefreshState = forceRefreshState;
		return this;
	}

	public @Nullable Classifier<Throwable, Boolean> getStateClassifier() {
		return stateClassifier;
	}

	SpringRetryConfig setStateClassifier(Classifier<Throwable, Boolean> stateClassifier) {
		this.stateClassifier = stateClassifier;
		return this;
	}

	public @Nullable RetryPolicy getRetryPolicy() {
		return retryPolicy;
	}

	SpringRetryConfig setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
		return this;
	}

	public @Nullable String getId() {
		return id;
	}

	SpringRetryConfig setId(String id) {
		this.id = id;
		return this;
	}

	public @Nullable BackOffPolicy getBackOffPolicy() {
		return backOffPolicy;
	}

	SpringRetryConfig setBackOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
		return this;
	}

}
