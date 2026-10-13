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
import com.amazonaws.samples.agentcore.idp.review.ReviewItem;

/**
 * Emits an audit trail for regulated document workflows. The default implementation writes
 * structured log events; provide your own bean to forward the trail to CloudWatch Logs
 * Insights, an audit table (DynamoDB), or a SIEM. Because it is a {@code @ConditionalOnMissingBean}
 * default, your implementation replaces it automatically.
 */
public interface AuditService {

	/**
	 * Record that a document was processed.
	 * @param conversationId session/conversation the document belongs to
	 * @param result the processing result
	 */
	void documentProcessed(String conversationId, DocumentResult result);

	/**
	 * Record a human review decision.
	 * @param item the resolved review item
	 */
	void reviewResolved(ReviewItem item);

}
