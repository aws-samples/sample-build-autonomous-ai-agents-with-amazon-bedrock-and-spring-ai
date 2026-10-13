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
package com.amazonaws.samples.agentcore.autonomous;

import com.amazonaws.samples.agentcore.autonomous.config.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline unit tests for the resilience beans. These do not require AWS credentials or a
 * Spring context, so they run in any environment.
 */
class ResilienceConfigTests {

	private final ResilienceConfig config = new ResilienceConfig();

	@Test
	void circuitBreakerStartsClosed() {
		CircuitBreaker circuitBreaker = this.config.bedrockCircuitBreaker();
		assertThat(circuitBreaker.getName()).isEqualTo("bedrock");
		assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
	}

	@Test
	void retryAllowsThreeAttempts() {
		Retry retry = this.config.bedrockRetry();
		assertThat(retry.getName()).isEqualTo("bedrock");
		assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
	}

}
