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
package com.amazonaws.samples.agentcore.idp.metrics;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

/**
 * Domain metrics for document processing. Generic request timers tell you the service is
 * up; these business metrics tell you whether it is doing useful work, how many
 * documents of each type were processed, how confident the extractions are, and how long
 * extraction takes. All are exported on {@code /actuator/prometheus}.
 */
@Component
public class IdpMetrics {

	private final MeterRegistry registry;

	public IdpMetrics(MeterRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Record a processed document.
	 * @param documentType classified type (or {@code unknown})
	 * @param status {@code success} or {@code failure}
	 * @param confidence overall extraction confidence, 0.0 to 1.0
	 * @param duration wall-clock processing time
	 */
	public void recordProcessed(String documentType, String status, double confidence, Duration duration) {
		String type = (documentType == null || documentType.isBlank()) ? "unknown" : documentType;

		this.registry.counter("idp.documents.processed", "type", type, "status", status).increment();

		this.registry.timer("idp.processing.duration", "type", type).record(duration);

		if ("success".equals(status)) {
			DistributionSummary.builder("idp.extraction.confidence")
				.description("Overall extraction confidence per document")
				.tag("type", type)
				.scale(100.0)
				.register(this.registry)
				.record(confidence);
		}
	}

	/**
	 * Count a document routed to the human review queue.
	 * @param documentType classified/agent type
	 */
	public void recordReviewQueued(String documentType) {
		this.registry.counter("idp.review.queued", "type", safe(documentType)).increment();
	}

	/**
	 * Count a review item resolved by a human.
	 * @param documentType classified/agent type
	 * @param decision APPROVED or REJECTED
	 */
	public void recordReviewResolved(String documentType, String decision) {
		this.registry.counter("idp.review.resolved", "type", safe(documentType), "decision", decision).increment();
	}

	/**
	 * Publish the current review-queue depth as a gauge so dashboards can alert on backlog.
	 * @param pending live counter of pending items owned by the review queue
	 */
	public void bindReviewPendingGauge(AtomicInteger pending) {
		Gauge.builder("idp.review.pending", pending, AtomicInteger::get)
			.description("Documents currently awaiting human review")
			.register(this.registry);
	}

	private static String safe(String type) {
		return (type == null || type.isBlank()) ? "unknown" : type;
	}

}
