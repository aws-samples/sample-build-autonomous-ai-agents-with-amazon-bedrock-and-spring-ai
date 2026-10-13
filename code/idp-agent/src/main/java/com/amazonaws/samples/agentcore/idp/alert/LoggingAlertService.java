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
package com.amazonaws.samples.agentcore.idp.alert;

import com.amazonaws.samples.agentcore.idp.agent.AgentType;
import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import com.amazonaws.samples.agentcore.idp.rules.RuleResult;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link AlertService}: writes a structured log line and increments an
 * {@code idp.alerts.sent} counter tagged by severity and agent type. Registered as the
 * fallback bean in {@link AlertConfig}; provide your own {@link AlertService} bean to fan
 * out to Amazon SNS, Slack, or email and this one steps aside.
 */
public class LoggingAlertService implements AlertService {

	private static final Logger logger = LoggerFactory.getLogger(LoggingAlertService.class);

	private final MeterRegistry registry;

	public LoggingAlertService(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void send(RuleResult result, AgentType agentType, ProcessedDocument document) {
		this.registry.counter("idp.alerts.sent", "severity", result.severity().name(), "type", agentType.name())
			.increment();
		logger.warn("ALERT [{}] rule={} type={} message=\"{}\" documentType={}", result.severity(), result.ruleId(),
				agentType, result.message(), document.documentType());
	}

}
