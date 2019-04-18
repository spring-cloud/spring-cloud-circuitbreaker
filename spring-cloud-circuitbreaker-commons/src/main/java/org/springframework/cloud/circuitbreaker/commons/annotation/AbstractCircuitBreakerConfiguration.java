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

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * .
 *
 * @author Tim Ysewyn
 */
@Configuration
public class AbstractCircuitBreakerConfiguration implements ImportAware {

	@Nullable
	protected AnnotationAttributes enableCircuitBreaker;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableCircuitBreaker = AnnotationAttributes.fromMap(
			importMetadata.getAnnotationAttributes(EnableCircuitBreaker.class.getName(), false));
		if (this.enableCircuitBreaker == null) {
			throw new IllegalArgumentException(
				"@EnableCircuitBreaker is not present on importing class " + importMetadata.getClassName());
		}
	}
}
