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

import java.time.Instant;
import java.util.List;

import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import com.amazonaws.samples.agentcore.idp.rules.RuleResult;

/**
 * An item queued for human review.
 *
 * @param id stable identifier used to resolve the item
 * @param agentType specialist that processed the document
 * @param conversationId session/conversation the document belongs to
 * @param extraction the structured extraction under review
 * @param alerts workflow rules that fired
 * @param reasons why the item was routed to review (blocked, alert, low confidence)
 * @param status current lifecycle state
 * @param reviewer who resolved it (null while pending)
 * @param reviewerNote optional note left by the reviewer
 * @param createdAt when the item entered the queue
 * @param resolvedAt when it was resolved (null while pending)
 */
public record ReviewItem(String id, String agentType, String conversationId, ProcessedDocument extraction,
		List<RuleResult> alerts, List<String> reasons, ReviewStatus status, String reviewer, String reviewerNote,
		Instant createdAt, Instant resolvedAt) {

	/**
	 * Create a fresh PENDING item.
	 */
	public static ReviewItem pending(String id, String agentType, String conversationId, ProcessedDocument extraction,
			List<RuleResult> alerts, List<String> reasons) {
		return new ReviewItem(id, agentType, conversationId, extraction, List.copyOf(alerts), List.copyOf(reasons),
				ReviewStatus.PENDING, null, null, Instant.now(), null);
	}

	/**
	 * Return a resolved copy of this item, preserving all other fields.
	 */
	public ReviewItem resolve(ReviewStatus decision, String reviewer, String note) {
		return new ReviewItem(this.id, this.agentType, this.conversationId, this.extraction, this.alerts, this.reasons,
				decision, reviewer, note, this.createdAt, Instant.now());
	}

}
