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
package com.amazonaws.samples.agentcore.idp.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.samples.agentcore.idp.agent.AgentType;
import com.amazonaws.samples.agentcore.idp.agent.ClassifierAgent;
import com.amazonaws.samples.agentcore.idp.agent.DocumentAgent;
import com.amazonaws.samples.agentcore.idp.agent.DocumentContext;
import com.amazonaws.samples.agentcore.idp.audit.AuditService;
import com.amazonaws.samples.agentcore.idp.dto.DocumentRequest;
import com.amazonaws.samples.agentcore.idp.dto.DocumentResult;
import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import com.amazonaws.samples.agentcore.idp.metrics.IdpMetrics;
import com.amazonaws.samples.agentcore.idp.alert.AlertService;
import com.amazonaws.samples.agentcore.idp.review.ReviewItem;
import com.amazonaws.samples.agentcore.idp.review.ReviewProperties;
import com.amazonaws.samples.agentcore.idp.review.ReviewQueue;
import com.amazonaws.samples.agentcore.idp.rules.RuleResult;
import com.amazonaws.samples.agentcore.idp.rules.RuleSeverity;
import com.amazonaws.samples.agentcore.idp.rules.RulesEngine;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/**
 * Coordinates the multi-agent flow for one document:
 * <ol>
 * <li>resolve the specialist (caller-specified or via the {@link ClassifierAgent}),</li>
 * <li>run the specialist {@link DocumentAgent} to extract and answer,</li>
 * <li>evaluate workflow {@link RulesEngine rules} and dispatch alerts,</li>
 * <li>record domain metrics and emit an observation/trace span.</li>
 * </ol>
 */
@Service
public class DocumentOrchestrator {

	private static final Logger logger = LoggerFactory.getLogger(DocumentOrchestrator.class);

	private final Map<AgentType, DocumentAgent> agents = new EnumMap<>(AgentType.class);

	private final ClassifierAgent classifier;

	private final RulesEngine rulesEngine;

	private final AlertService alertService;

	private final IdpMetrics metrics;

	private final ReviewQueue reviewQueue;

	private final ReviewProperties reviewProperties;

	private final AuditService auditService;

	public DocumentOrchestrator(List<DocumentAgent> agentBeans, ClassifierAgent classifier, RulesEngine rulesEngine,
			AlertService alertService, IdpMetrics metrics, ReviewQueue reviewQueue, ReviewProperties reviewProperties,
			AuditService auditService) {
		for (DocumentAgent agent : agentBeans) {
			this.agents.put(agent.type(), agent);
		}
		this.classifier = classifier;
		this.rulesEngine = rulesEngine;
		this.alertService = alertService;
		this.metrics = metrics;
		this.reviewQueue = reviewQueue;
		this.reviewProperties = reviewProperties;
		this.auditService = auditService;
	}

	@Observed(name = "idp.process", contextualName = "process-document")
	public DocumentResult process(DocumentRequest request, String conversationId) {
		long startNanos = System.nanoTime();
		byte[] media = decode(request.base64Content());

		AgentType type = resolveType(request, media, conversationId);
		DocumentContext context = new DocumentContext(type, request.question(), request.content(), request.mimeType(),
				media, conversationId);
		DocumentAgent agent = this.agents.getOrDefault(type, this.agents.get(AgentType.GENERAL));

		String status = "success";
		ProcessedDocument extraction = null;
		try {
			extraction = agent.process(context);
			List<RuleResult> alerts = this.rulesEngine.evaluate(type, extraction);
			boolean blocked = dispatchAlerts(alerts, type, extraction);

			// Route flagged or low-confidence extractions to human review.
			List<String> reviewReasons = reviewReasons(blocked, alerts, extraction);
			boolean needsReview = this.reviewProperties.enabled() && !reviewReasons.isEmpty();
			String reviewId = null;
			if (needsReview) {
				ReviewItem item = this.reviewQueue.enqueue(type.name(), conversationId, extraction, alerts,
						reviewReasons);
				reviewId = item.id();
			}

			DocumentResult result = new DocumentResult(type.name(), extraction, alerts, blocked, needsReview, reviewId,
					reviewReasons);
			this.auditService.documentProcessed(conversationId, result);
			return result;
		}
		catch (RuntimeException ex) {
			status = "failure";
			logger.error("Processing failed for agent {}", type, ex);
			throw ex;
		}
		finally {
			double confidence = (extraction != null) ? extraction.confidence() : 0.0;
			this.metrics.recordProcessed(type.name(), status, confidence,
					Duration.ofNanos(System.nanoTime() - startNanos));
		}
	}

	private AgentType resolveType(DocumentRequest request, byte[] media, String conversationId) {
		if (request.agentType() != null && !request.agentType().isBlank()) {
			return AgentType.fromOrDefault(request.agentType());
		}
		DocumentContext classifyContext = new DocumentContext(AgentType.GENERAL, null, request.content(),
				request.mimeType(), media, conversationId);
		return this.classifier.classify(classifyContext);
	}

	/**
	 * Decide why (if at all) a document should go to a human. A document is routed to review
	 * when a BLOCK rule fired, when any actionable alert was raised, or when the model's
	 * overall confidence falls below the configured threshold.
	 */
	private List<String> reviewReasons(boolean blocked, List<RuleResult> alerts, ProcessedDocument extraction) {
		List<String> reasons = new ArrayList<>();
		if (blocked) {
			reasons.add("Blocked by a workflow rule");
		}
		boolean hasAlert = alerts.stream()
			.anyMatch(a -> a.severity() == RuleSeverity.ALERT || a.severity() == RuleSeverity.BLOCK);
		if (hasAlert && !blocked) {
			reasons.add("Actionable alert raised");
		}
		double threshold = this.reviewProperties.confidenceThreshold();
		double confidence = (extraction != null) ? extraction.confidence() : 0.0;
		if (confidence < threshold) {
			reasons.add(String.format("Low extraction confidence (%.0f%% below %.0f%% threshold)", confidence * 100,
					threshold * 100));
		}
		return reasons;
	}

	private boolean dispatchAlerts(List<RuleResult> alerts, AgentType type, ProcessedDocument extraction) {
		boolean blocked = false;
		for (RuleResult alert : alerts) {
			if (alert.severity() == RuleSeverity.ALERT || alert.severity() == RuleSeverity.BLOCK) {
				this.alertService.send(alert, type, extraction);
			}
			if (alert.severity() == RuleSeverity.BLOCK) {
				blocked = true;
			}
		}
		return blocked;
	}

	private byte[] decode(String base64) {
		if (base64 == null || base64.isBlank()) {
			return null;
		}
		try {
			return Base64.getDecoder().decode(base64);
		}
		catch (IllegalArgumentException ex) {
			logger.warn("Ignoring malformed base64 content");
			return null;
		}
	}

}
