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
package com.amazonaws.samples.agentcore.idp.dto;

import java.util.List;

import com.amazonaws.samples.agentcore.idp.rules.RuleResult;

/**
 * Final response returned to the caller: the specialist that handled the document, the
 * structured extraction, any workflow rules that fired, and whether the document is
 * blocked for manual review.
 *
 * @param agentType specialist that processed the document
 * @param extraction structured extraction from the agent
 * @param alerts workflow rules that triggered
 * @param blocked true if a BLOCK-severity rule fired
 * @param needsReview true if the document was routed to the human review queue
 * @param reviewId id of the queued review item (null when no review is needed)
 * @param reviewReasons why the document was routed to review (empty when none)
 */
public record DocumentResult(String agentType, ProcessedDocument extraction, List<RuleResult> alerts, boolean blocked,
		boolean needsReview, String reviewId, List<String> reviewReasons) {
}
