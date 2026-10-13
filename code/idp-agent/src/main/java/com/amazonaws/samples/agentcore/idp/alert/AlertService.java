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

/**
 * Sends alerts for rules that fired at ALERT or BLOCK severity. The default implementation
 * logs and counts; swap in an Amazon SNS, Slack, or email implementation by providing your
 * own bean (it replaces the default via {@code @ConditionalOnMissingBean}).
 */
public interface AlertService {

	/**
	 * Deliver an alert.
	 * @param result the rule that fired
	 * @param agentType the agent that processed the document
	 * @param document the extraction that triggered the rule
	 */
	void send(RuleResult result, AgentType agentType, ProcessedDocument document);

}
