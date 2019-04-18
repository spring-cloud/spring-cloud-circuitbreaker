/*
 * Copyright 2013-2018 the original author or authors.
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

package org.springframework.cloud.circuitbreaker.commons.annotation;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;

/**
 * .
 *
 * @author Tim Ysewyn
 */
public class CircuitBreakerInterceptor extends AbstractCircuitBreakerInterceptor
		implements MethodInterceptor, Serializable {

	@Override
	@Nullable
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();

		CircuitBreakerInvoker invoker = () -> {
			try {
				return invocation.proceed();
			}
			catch (Throwable throwable) {
				throw new CircuitBreakerInterceptor.ThrowableWrapper(throwable);
			}
		};

		try {
			return this.invoke(invoker, invocation.getThis(), method);
		}
		catch (CircuitBreakerInterceptor.ThrowableWrapper th) {
			throw th.getOriginal();
		}
	}

	class ThrowableWrapper extends RuntimeException {

		private final Throwable original;

		ThrowableWrapper(Throwable original) {
			super(original.getMessage(), original);
			this.original = original;
		}

		Throwable getOriginal() {
			return this.original;
		}

	}

}
