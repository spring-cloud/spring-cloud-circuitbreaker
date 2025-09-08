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

package org.springframework.cloud.circuitbreaker.resilience4j;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to ensure that
 * {@code TimeLimiterMetricsAutoConfiguration} goes after
 * {@code CompositeMeterRegistryAutoConfiguration} so that the {@link MeterRegistry} bean
 * is available.
 *
 * @author Andy Wilkinson
 */
@AutoConfiguration(afterName = "org.springframework.boot.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
		beforeName = "io.github.resilience4j.springboot3.timelimiter.autoconfigure.TimeLimiterMetricsAutoConfiguration")
public class Resilence4JMetricsOrderingAutoConfiguration {

}
