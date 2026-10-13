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

import com.amazonaws.samples.agentcore.autonomous.metrics.AgentMetrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline unit tests for the domain metrics using an in-memory registry.
 */
class AgentMetricsTests {

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

	private final AgentMetrics metrics = new AgentMetrics(this.registry);

	@Test
	void recordsRequestAndSuccess() {
		Timer.Sample sample = this.metrics.startTimer();
		this.metrics.recordRequest();
		this.metrics.recordSuccess(sample);

		assertThat(this.registry.get("agent.chat.requests").counter().count()).isEqualTo(1.0);
		assertThat(this.registry.get("agent.chat.success").counter().count()).isEqualTo(1.0);
		assertThat(this.registry.get("agent.chat.duration").timer().count()).isEqualTo(1L);
	}

	@Test
	void recordsFallbackOnFailure() {
		Timer.Sample sample = this.metrics.startTimer();
		this.metrics.recordRequest();
		this.metrics.recordFailure(sample);
		this.metrics.recordFallback();

		assertThat(this.registry.get("agent.chat.failure").counter().count()).isEqualTo(1.0);
		assertThat(this.registry.get("agent.chat.fallback").counter().count()).isEqualTo(1.0);
	}

}
