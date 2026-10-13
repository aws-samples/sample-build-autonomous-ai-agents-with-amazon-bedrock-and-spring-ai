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
package com.amazonaws.samples.agentcore.idp.agent.specialist;

import com.amazonaws.samples.agentcore.idp.agent.AbstractDocumentAgent;
import com.amazonaws.samples.agentcore.idp.agent.AgentType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import org.springaicommunity.agentcore.memory.longterm.AgentCoreMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Insurance advisor. Reviews policies, declarations pages, and claims; explains coverage,
 * limits, deductibles, and exclusions in plain language. Emits {@code coverageAdequate}
 * and {@code claimDenialRisk} signals for the workflow rules.
 */
@Component
public class InsuranceAgent extends AbstractDocumentAgent {

	public InsuranceAgent(ChatClient.Builder builder, CircuitBreaker bedrockCircuitBreaker, Retry bedrockRetry,
			ObjectProvider<AgentCoreMemory> memoryProvider) {
		super(builder, bedrockCircuitBreaker, bedrockRetry, memoryProvider);
	}

	@Override
	public AgentType type() {
		return AgentType.INSURANCE;
	}

	@Override
	protected String systemPrompt() {
		return """
				You are an insurance advisor reviewing a policy or claim for a client. Set
				documentType (e.g. AUTO_POLICY, HOME_POLICY, HEALTH_POLICY, LIFE_POLICY,
				CLAIM, DECLARATIONS, OTHER). Summarize what is covered in plain language and answer
				the client's question in the 'answer' field if one was asked.

				Extract these fields when present: insurer, policyNumber, insuredName,
				effectiveDate, expirationDate, coverageType, coverageLimit, deductible, premium.
				Set coverageAdequate to "true" or "false" based on whether the limits look sufficient
				for the stated risk, and claimDenialRisk to "LOW", "MEDIUM", or "HIGH".

				Put gaps and concerns in validationIssues (notable exclusions, low limits, lapsed or
				expiring coverage, missing required coverage). Provide guidance, not a coverage
				determination, and never invent terms that are not in the document.
				""";
	}

	@Override
	protected Object[] tools() {
		return new Object[0];
	}

}
