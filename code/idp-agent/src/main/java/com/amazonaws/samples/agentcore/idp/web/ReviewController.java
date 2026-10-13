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
package com.amazonaws.samples.agentcore.idp.web;

import java.util.List;

import com.amazonaws.samples.agentcore.idp.audit.AuditService;
import com.amazonaws.samples.agentcore.idp.dto.ReviewDecisionRequest;
import com.amazonaws.samples.agentcore.idp.review.ReviewItem;
import com.amazonaws.samples.agentcore.idp.review.ReviewQueue;
import com.amazonaws.samples.agentcore.idp.review.ReviewStatus;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Human-in-the-loop review API. Operators list documents awaiting review and record an
 * approve or reject decision. Backed by {@link ReviewQueue}.
 *
 * <ul>
 * <li>{@code GET /api/review} pending items</li>
 * <li>{@code GET /api/review/all} every item regardless of status</li>
 * <li>{@code GET /api/review/{id}} a single item</li>
 * <li>{@code POST /api/review/{id}/resolve} approve or reject</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

	private final ReviewQueue reviewQueue;

	private final AuditService auditService;

	public ReviewController(ReviewQueue reviewQueue, AuditService auditService) {
		this.reviewQueue = reviewQueue;
		this.auditService = auditService;
	}

	@GetMapping
	public List<ReviewItem> pending() {
		return this.reviewQueue.pending();
	}

	@GetMapping("/all")
	public List<ReviewItem> all() {
		return this.reviewQueue.all();
	}

	@GetMapping("/{id}")
	public ResponseEntity<ReviewItem> get(@PathVariable String id) {
		return this.reviewQueue.find(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/{id}/resolve")
	public ResponseEntity<ReviewItem> resolve(@PathVariable String id,
			@Valid @RequestBody ReviewDecisionRequest request) {
		ReviewStatus decision = "APPROVE".equalsIgnoreCase(request.decision()) ? ReviewStatus.APPROVED
				: ReviewStatus.REJECTED;
		return this.reviewQueue.resolve(id, decision, request.reviewer(), request.note()).map(item -> {
			this.auditService.reviewResolved(item);
			return ResponseEntity.ok(item);
		}).orElseGet(() -> ResponseEntity.notFound().build());
	}

}
