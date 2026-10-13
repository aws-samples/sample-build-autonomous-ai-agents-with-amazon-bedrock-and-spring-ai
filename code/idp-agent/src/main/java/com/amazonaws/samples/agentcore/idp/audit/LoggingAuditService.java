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
package com.amazonaws.samples.agentcore.idp.audit;

import com.amazonaws.samples.agentcore.idp.dto.DocumentResult;
import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import com.amazonaws.samples.agentcore.idp.review.ReviewItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link AuditService} that writes structured audit events to a dedicated logger
 * ({@code idp.audit}). With {@code LOG_FORMAT=ecs} these become structured JSON records
 * that flow to CloudWatch, queryable in Logs Insights. Registered as the fallback bean in
 * {@link AuditConfig}; provide your own {@link AuditService} bean and this one steps aside.
 */
public class LoggingAuditService implements AuditService {

	private static final Logger audit = LoggerFactory.getLogger("idp.audit");

	@Override
	public void documentProcessed(String conversationId, DocumentResult result) {
		ProcessedDocument doc = result.extraction();
		audit.info(
				"event=document_processed conversationId={} agentType={} documentType={} confidence={} alerts={} blocked={} needsReview={}",
				conversationId, result.agentType(), (doc != null) ? doc.documentType() : "unknown",
				(doc != null) ? String.format("%.2f", doc.confidence()) : "0.00", result.alerts().size(),
				result.blocked(), result.needsReview());
	}

	@Override
	public void reviewResolved(ReviewItem item) {
		audit.info("event=review_resolved id={} agentType={} decision={} reviewer={} note={}", item.id(),
				item.agentType(), item.status(), item.reviewer(), item.reviewerNote());
	}

}
