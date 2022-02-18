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

package org.springframework.cloud.circuitbreaker.springretry;

import org.springframework.classify.Classifier;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;

/**
 * @author Ryan Baxter
 */
public class SpringRetryConfig {

	private String id;

	private BackOffPolicy backOffPolicy;

	private RetryPolicy retryPolicy;

	private boolean forceRefreshState;

	private Classifier<Throwable, Boolean> stateClassifier;

	boolean isForceRefreshState() {
		return forceRefreshState;
	}

	void setForceRefreshState(boolean forceRefreshState) {
		this.forceRefreshState = forceRefreshState;
	}

	Classifier<Throwable, Boolean> getStateClassifier() {
		return stateClassifier;
	}

	void setStateClassifier(Classifier<Throwable, Boolean> stateClassifier) {
		this.stateClassifier = stateClassifier;
	}

	RetryPolicy getRetryPolicy() {
		return retryPolicy;
	}

	void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	String getId() {
		return id;
	}

	void setId(String id) {
		this.id = id;
	}

	BackOffPolicy getBackOffPolicy() {
		return backOffPolicy;
	}

	void setBackOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
	}

}
