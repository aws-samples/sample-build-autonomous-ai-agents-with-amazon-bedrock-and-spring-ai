/*
 * Copyright 2025-2026 the original author or authors.
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
package com.amazonaws.samples.agentcore.autonomous.config;

import java.time.Duration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience policies applied to the model call. A circuit breaker stops hammering
 * Amazon Bedrock when it is failing, and a retry policy with exponential backoff smooths
 * over transient throttling ({@code ThrottlingException}) and brief network errors.
 */
@Configuration
public class ResilienceConfig {

	@Bean
	public CircuitBreaker bedrockCircuitBreaker() {
		CircuitBreakerConfig config = CircuitBreakerConfig.custom()
			.failureRateThreshold(50.0f)
			.slidingWindowSize(10)
			.waitDurationInOpenState(Duration.ofSeconds(15))
			.permittedNumberOfCallsInHalfOpenState(3)
			.build();
		return CircuitBreaker.of("bedrock", config);
	}

	@Bean
	public Retry bedrockRetry() {
		RetryConfig config = RetryConfig.custom()
			.maxAttempts(3)
			.intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(Duration.ofMillis(500),
					2.0))
			.build();
		return Retry.of("bedrock", config);
	}

}
