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
package com.amazonaws.samples.agentcore.idp.rules;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized workflow rules. Operators add or change rules in {@code application.yml}
 * under {@code idp.rules}, no code change or redeploy of logic required.
 *
 * <p>
 * Each rule's {@code condition} is a Spring Expression Language (SpEL) expression
 * evaluated against a {@link RuleContext}. Because SpEL is powerful, these rules are
 * trusted operator configuration, not end-user input.
 *
 * @param rules the configured rules (never null)
 */
@ConfigurationProperties(prefix = "idp")
public record IdpProperties(List<RuleDefinition> rules) {

	public IdpProperties {
		rules = (rules == null) ? List.of() : List.copyOf(rules);
	}

	/**
	 * A single workflow rule.
	 *
	 * @param id stable identifier
	 * @param description human-readable purpose
	 * @param agentType {@code ALL} or a specific {@link com.amazonaws.samples.agentcore.idp.agent.AgentType}
	 * name
	 * @param condition SpEL predicate over {@link RuleContext}; when true, the rule fires
	 * @param severity {@link RuleSeverity} name
	 * @param message message surfaced to the caller and any alert
	 */
	public record RuleDefinition(String id, String description, String agentType, String condition, String severity,
			String message) {
	}

}
