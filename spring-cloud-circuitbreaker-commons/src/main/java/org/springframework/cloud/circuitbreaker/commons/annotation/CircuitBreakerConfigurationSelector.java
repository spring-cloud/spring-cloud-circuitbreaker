/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.circuitbreaker.commons.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author Tim Ysewyn
 */
public class CircuitBreakerConfigurationSelector extends AdviceModeImportSelector<EnableCircuitBreaker> {

	@Override
	protected String[] selectImports(AdviceMode adviceMode) {
		switch (adviceMode) {
		case PROXY:
			return getProxyImports();
		case ASPECTJ:
			return getAspectJImports();
		default:
			return null;
		}
	}

	/**
	 * Return the imports to use if the {@link AdviceMode} is set to {@link AdviceMode#PROXY}.
	 */
	private String[] getProxyImports() {
		List<String> result = new ArrayList<>(2);
		result.add(AutoProxyRegistrar.class.getName());
		result.add(ProxyCircuitBreakerConfiguration.class.getName());
		return StringUtils.toStringArray(result);
	}

	/**
	 * Return the imports to use if the {@link AdviceMode} is set to {@link AdviceMode#ASPECTJ}.
	 */
	private String[] getAspectJImports() {
		List<String> result = Collections.singletonList(AspectJCircuitBreakerConfiguration.class.getName());
		return StringUtils.toStringArray(result);
	}
}
