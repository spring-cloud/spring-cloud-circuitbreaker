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

import reactor.core.publisher.Mono;

import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.circuitbreaker.commons.Customizer;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = ReactiveHystrixCircuitBreakerIntegrationTest.Application.class)
@DirtiesContext
public class ReactiveHystrixCircuitBreakerIntegrationTest {

	@LocalServerPort
	int port = 0;


	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		@RequestMapping("/slow")
		public Mono<String> slow() {
			return Mono.just("slow").delayElement(Duration.ofSeconds(3));
		}

		@GetMapping("/normal")
		public Mono<String> normal() {
			return Mono.just("normal");
		}

		@Bean
		public Customizer<ReactiveCircuitBreakerFactory<HystrixObservableCommand.Setter,
				ReactiveHystrixCircuitBreakerFactory.ReactiveHystrixConfigBuilder>> customizer() {
			return factory -> factory.configure(builder -> builder.commandProperties(
							HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(2000)), "slow");
		}

		@Bean
		public Customizer<ReactiveCircuitBreakerFactory<HystrixObservableCommand.Setter,
				ReactiveHystrixCircuitBreakerFactory.ReactiveHystrixConfigBuilder>> defaultConfig() {
			return factory -> factory.configureDefault(id -> {
				return HystrixObservableCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(id))
						.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
								.withExecutionTimeoutInMilliseconds(4000));

			});
		}

		@Service
		public static class DemoControllerService {
			private int port = 0;
			private ReactiveCircuitBreakerFactory cbFactory;


			public DemoControllerService(ReactiveCircuitBreakerFactory cbBuilder) {
				this.cbFactory = cbBuilder;
			}

			public Mono<String> slow() {
				return cbFactory.create("slow").run(WebClient.builder().baseUrl("http://localhost:" + port).build()
						.get().uri("/slow").retrieve().bodyToMono(String.class), t -> {
					t.printStackTrace();
					return Mono.just("fallback");
				});
			}

			public Mono<String> normal() {
				return cbFactory.create("normal").run(WebClient.builder().baseUrl("http://localhost:" + port).build()
						.get().uri("/normal").retrieve().bodyToMono(String.class), t -> {
					t.printStackTrace();
					return Mono.just("fallback");
				});
			}

			public void setPort(int port) {
				this.port = port;
			}
		}
	}

	@Autowired
	ReactiveHystrixCircuitBreakerIntegrationTest.Application.DemoControllerService service;

	@Before
	public void setup() {
		service.setPort(port);
	}

	@Test
	public void testSlow() {
		assertEquals("fallback", service.slow().block());
	}

	@Test
	public void testNormal() {
		assertEquals("normal", service.normal().block());
	}
}
