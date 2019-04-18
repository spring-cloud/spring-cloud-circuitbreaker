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

import java.lang.reflect.Method;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.cloud.circuitbreaker.commons.CircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreakerFactory;
import org.springframework.util.ReflectionUtils;

/**
 * .
 *
 * @author Tim Ysewyn
 */
public abstract class AbstractCircuitBreakerInterceptor implements BeanFactoryAware {

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public Object invoke(final CircuitBreakerInvoker invoker, Object target,
			Method method) {
		CircuitBreaker annotation = method.getAnnotation(CircuitBreaker.class);
		String circuitBreakerName = annotation.name();
		Method fallbackMethod = findFallbackMethod(target, annotation.fallbackMethod());

		if (this.isReactiveMethod(method)) {
			if (this.isFlux(method)) {
				return invokeFlux(invoker, circuitBreakerName, target, fallbackMethod);
			}
			else {
				return invokeMono(invoker, circuitBreakerName, target, fallbackMethod);
			}
		}
		else {
			return invoke(invoker, circuitBreakerName, target, fallbackMethod);
		}
	}

	private Method findFallbackMethod(Object target, String fallbackMethodName) {
		if (fallbackMethodName.length() == 0) {
			return null;
		}
		return ReflectionUtils.findMethod(target.getClass(), fallbackMethodName,
				Throwable.class);
	}

	private boolean isReactiveMethod(Method method) {
		return Publisher.class.isAssignableFrom(method.getReturnType());
	}

	private boolean isFlux(Method method) {
		return Flux.class.isAssignableFrom(method.getReturnType());
	}

	private Object invokeFlux(final CircuitBreakerInvoker invoker,
			String circuitBreakerName, Object target, Method fallbackMethod) {
		ReactiveCircuitBreakerFactory cb = this.beanFactory
				.getBean(ReactiveCircuitBreakerFactory.class);
		if (fallbackMethod == null) {
			return cb.create(circuitBreakerName).run((Flux<Object>) invoker.invoke());
		}
		else {
			return cb.create(circuitBreakerName).run((Flux<Object>) invoker.invoke(),
					(t) -> (Flux<Object>) ReflectionUtils.invokeMethod(fallbackMethod,
							target, t));
		}
	}

	private Object invokeMono(final CircuitBreakerInvoker invoker,
			String circuitBreakerName, Object target, Method fallbackMethod) {
		ReactiveCircuitBreakerFactory cb = this.beanFactory
				.getBean(ReactiveCircuitBreakerFactory.class);
		if (fallbackMethod == null) {
			return cb.create(circuitBreakerName).run((Mono<Object>) invoker.invoke());
		}
		else {
			return cb.create(circuitBreakerName).run((Mono<Object>) invoker.invoke(),
					(t) -> (Mono<Object>) ReflectionUtils.invokeMethod(fallbackMethod,
							target, t));
		}
	}

	private Object invoke(final CircuitBreakerInvoker invoker, String circuitBreakerName,
			Object target, Method fallbackMethod) {
		CircuitBreakerFactory cb = this.beanFactory.getBean(CircuitBreakerFactory.class);
		if (fallbackMethod == null) {
			return cb.create(circuitBreakerName).run(invoker::invoke);
		}
		else {
			return cb.create(circuitBreakerName).run(invoker::invoke,
					t -> ReflectionUtils.invokeMethod(fallbackMethod, target, t));
		}
	}

}
