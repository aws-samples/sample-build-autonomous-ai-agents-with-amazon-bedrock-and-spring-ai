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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the human-in-the-loop review workflow.
 *
 * <p>
 * A production IDP pipeline rarely auto-approves everything. Documents that are blocked by
 * a rule, carry an actionable alert, or come back with low model confidence are routed to
 * a review queue for a human to approve or reject. These properties tune that routing.
 *
 * @param enabled whether low-confidence / alerted documents are routed to the review queue
 * @param confidenceThreshold extractions below this confidence (0.0 to 1.0) are flagged for
 * review
 * @param maxQueueSize maximum number of items retained in the in-memory queue before the
 * oldest resolved items are evicted
 */
@ConfigurationProperties(prefix = "idp.review")
public record ReviewProperties(Boolean enabled, Double confidenceThreshold, Integer maxQueueSize) {

	public ReviewProperties {
		if (enabled == null) {
			enabled = Boolean.TRUE;
		}
		if (confidenceThreshold == null || confidenceThreshold < 0.0 || confidenceThreshold > 1.0) {
			confidenceThreshold = 0.7;
		}
		if (maxQueueSize == null || maxQueueSize < 1) {
			maxQueueSize = 500;
		}
	}

}
