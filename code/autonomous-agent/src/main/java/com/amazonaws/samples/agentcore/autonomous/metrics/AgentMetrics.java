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
package com.amazonaws.samples.agentcore.autonomous.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Component;

/**
 * Domain metrics for the agent. These describe whether the agent is doing useful work , 
 * not just that the process is up. All meters are exposed on {@code /actuator/prometheus}.
 *
 * <ul>
 * <li>{@code agent.chat.requests}, invocations received</li>
 * <li>{@code agent.chat.success} / {@code agent.chat.failure}, terminal outcomes</li>
 * <li>{@code agent.chat.fallback}, times the resilience fallback message was returned</li>
 * <li>{@code agent.chat.duration}, end-to-end turn latency (with percentile histogram)</li>
 * </ul>
 */
@Component
public class AgentMetrics {

	private final Counter requests;

	private final Counter successes;

	private final Counter failures;

	private final Counter fallbacks;

	private final Timer duration;

	public AgentMetrics(MeterRegistry registry) {
		this.requests = Counter.builder("agent.chat.requests")
			.description("Total chat invocations received")
			.register(registry);
		this.successes = Counter.builder("agent.chat.success")
			.description("Chat invocations that completed successfully")
			.register(registry);
		this.failures = Counter.builder("agent.chat.failure")
			.description("Chat invocations that ended in error")
			.register(registry);
		this.fallbacks = Counter.builder("agent.chat.fallback")
			.description("Chat invocations served by the resilience fallback")
			.register(registry);
		this.duration = Timer.builder("agent.chat.duration")
			.description("End-to-end chat turn latency")
			.publishPercentileHistogram()
			.register(registry);
	}

	public Timer.Sample startTimer() {
		return Timer.start();
	}

	public void recordRequest() {
		this.requests.increment();
	}

	public void recordSuccess(Timer.Sample sample) {
		this.successes.increment();
		sample.stop(this.duration);
	}

	public void recordFailure(Timer.Sample sample) {
		this.failures.increment();
		sample.stop(this.duration);
	}

	public void recordFallback() {
		this.fallbacks.increment();
	}

}
