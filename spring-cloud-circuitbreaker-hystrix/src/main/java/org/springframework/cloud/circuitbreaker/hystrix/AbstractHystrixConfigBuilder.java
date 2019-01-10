/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.cloud.circuitbreaker.hystrix;

import org.springframework.cloud.circuitbreaker.commons.ConfigBuilder;
import org.springframework.util.StringUtils;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;

/**
 * @author Ryan Baxter
 */
public abstract class AbstractHystrixConfigBuilder<CONFB> implements ConfigBuilder<CONFB> {

	private final String commandName;
	protected String groupName;
	protected HystrixCommandProperties.Setter commandProperties;

	public AbstractHystrixConfigBuilder(String id) {
		this.commandName = id;
	}


	public AbstractHystrixConfigBuilder groupName(String groupName) {
		this.groupName = groupName;
		return this;
	}

	public AbstractHystrixConfigBuilder commandProperties(
			HystrixCommandProperties.Setter commandProperties) {
		this.commandProperties = commandProperties;
		return this;
	}

	protected HystrixCommandGroupKey getGroupKey() {
		String groupNameToUse;
		if (StringUtils.hasText(this.groupName)) {
			groupNameToUse = this.groupName;
		} else {
			groupNameToUse = commandName + "group";
		}
		return HystrixCommandGroupKey.Factory.asKey(groupNameToUse);
	}

	protected HystrixCommandKey getCommandKey() {
		return HystrixCommandKey.Factory.asKey(this.commandName);
	}

	protected HystrixCommandProperties.Setter getCommandPropertiesSetter() {
		return this.commandProperties != null
				? this.commandProperties
				: HystrixCommandProperties.Setter();
	}

}
