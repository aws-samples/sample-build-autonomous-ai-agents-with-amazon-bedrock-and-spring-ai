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
package com.amazonaws.samples.agentcore.idp.review;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import com.amazonaws.samples.agentcore.idp.metrics.IdpMetrics;
import com.amazonaws.samples.agentcore.idp.rules.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/**
 * In-memory, thread-safe store of documents awaiting human review.
 *
 * <p>
 * This is a reference implementation suitable for a single AgentCore Runtime instance or
 * for local development. For a horizontally scaled or durable deployment, back this with a
 * shared store (for example Amazon DynamoDB or an SQS-fed worker) behind the same API. The
 * queue is bounded: once it reaches {@code maxQueueSize}, the oldest already-resolved items
 * are evicted first so pending work is never silently dropped.
 */
@Service
public class ReviewQueue {

	private static final Logger logger = LoggerFactory.getLogger(ReviewQueue.class);

	private final ConcurrentMap<String, ReviewItem> items = new ConcurrentHashMap<>();

	private final AtomicInteger pendingGauge = new AtomicInteger();

	private final int maxQueueSize;

	private final IdpMetrics metrics;

	public ReviewQueue(ReviewProperties properties, IdpMetrics metrics) {
		this.maxQueueSize = properties.maxQueueSize();
		this.metrics = metrics;
		this.metrics.bindReviewPendingGauge(this.pendingGauge);
	}

	/**
	 * Enqueue a document for human review.
	 * @return the created item
	 */
	public ReviewItem enqueue(String agentType, String conversationId, ProcessedDocument extraction,
			List<RuleResult> alerts, List<String> reasons) {
		evictIfNeeded();
		String id = UUID.randomUUID().toString();
		ReviewItem item = ReviewItem.pending(id, agentType, conversationId, extraction, alerts, reasons);
		this.items.put(id, item);
		this.pendingGauge.incrementAndGet();
		this.metrics.recordReviewQueued(agentType);
		logger.info("Queued document for review: id={} agent={} reasons={}", id, agentType, reasons);
		return item;
	}

	/**
	 * Resolve a pending item.
	 * @return the updated item, or empty if the id is unknown or already resolved
	 */
	public Optional<ReviewItem> resolve(String id, ReviewStatus decision, String reviewer, String note) {
		if (decision != ReviewStatus.APPROVED && decision != ReviewStatus.REJECTED) {
			throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
		}
		boolean[] transitioned = { false };
		ReviewItem updated = this.items.computeIfPresent(id, (key, existing) -> {
			if (existing.status() != ReviewStatus.PENDING) {
				return existing;
			}
			transitioned[0] = true;
			return existing.resolve(decision, reviewer, note);
		});
		if (updated == null) {
			return Optional.empty();
		}
		if (transitioned[0]) {
			this.pendingGauge.updateAndGet(v -> Math.max(0, v - 1));
			this.metrics.recordReviewResolved(updated.agentType(), decision.name());
			logger.info("Resolved review item id={} decision={} reviewer={}", id, decision, reviewer);
		}
		return Optional.of(updated);
	}

	/** All pending items, newest first. */
	public List<ReviewItem> pending() {
		return this.items.values()
			.stream()
			.filter(i -> i.status() == ReviewStatus.PENDING)
			.sorted(Comparator.comparing(ReviewItem::createdAt).reversed())
			.toList();
	}

	/** Every item regardless of status, newest first. */
	public List<ReviewItem> all() {
		return this.items.values()
			.stream()
			.sorted(Comparator.comparing(ReviewItem::createdAt).reversed())
			.toList();
	}

	/** Look up a single item by id. */
	public Optional<ReviewItem> find(String id) {
		return Optional.ofNullable(this.items.get(id));
	}

	/** Number of items still awaiting review. */
	public int pendingCount() {
		return this.pendingGauge.get();
	}

	private void evictIfNeeded() {
		if (this.items.size() < this.maxQueueSize) {
			return;
		}
		this.items.values()
			.stream()
			.filter(i -> i.status() != ReviewStatus.PENDING)
			.min(Comparator.comparing(ReviewItem::createdAt))
			.map(ReviewItem::id)
			.ifPresent(this.items::remove);
	}

}
