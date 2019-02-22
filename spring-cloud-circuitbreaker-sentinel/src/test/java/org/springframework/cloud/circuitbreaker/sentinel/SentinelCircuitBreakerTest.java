/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.cloud.circuitbreaker.sentinel;

import org.junit.Test;
import org.springframework.cloud.circuitbreaker.commons.CircuitBreaker;

import static org.junit.Assert.*;

/**
 * @author Eric Zhao
 */
public class SentinelCircuitBreakerTest {

    @Test
    public void testRun() {
        CircuitBreaker cb = new SentinelCircuitBreakerFactory().create("testSentinelRun");
        assertEquals("foobar", cb.run(() -> "foobar"));
    }

    @Test
    public void testRunWithFallback() {
        CircuitBreaker cb = new SentinelCircuitBreakerFactory().create("testSentinelRunWithFallback");
        assertEquals("fallback", cb.run(() -> {
            throw new RuntimeException("boom");
        }, t -> "fallback"));
    }
}
