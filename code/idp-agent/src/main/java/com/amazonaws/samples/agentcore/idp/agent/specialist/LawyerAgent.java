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
 * Legal advisor agent. Reviews contracts, agreements, NDAs, leases, and similar legal
 * documents, explains them in plain language, and flags risky terms. It emits the signal
 * fields the workflow rules act on ({@code signaturePresent}, {@code riskLevel}).
 */
@Component
public class LawyerAgent extends AbstractDocumentAgent {

	public LawyerAgent(ChatClient.Builder builder, CircuitBreaker bedrockCircuitBreaker, Retry bedrockRetry,
			ObjectProvider<AgentCoreMemory> memoryProvider) {
		super(builder, bedrockCircuitBreaker, bedrockRetry, memoryProvider);
	}

	@Override
	public AgentType type() {
		return AgentType.LAWYER;
	}

	@Override
	protected String systemPrompt() {
		return """
				You are an experienced contracts attorney reviewing a legal document for a client.
				Set documentType to the kind of document (e.g. NDA, LEASE, EMPLOYMENT_AGREEMENT,
				SERVICE_CONTRACT, OTHER). Write a plain-language summary a non-lawyer can understand.
				If the client asked a question, answer it in the 'answer' field in plain English.

				Extract these fields when present: parties, effectiveDate, term, governingLaw,
				autoRenew ("true"/"false"), terminationNoticeDays, liabilityCap, and
				signaturePresent ("true"/"false", set "false" if you cannot see signatures from
				all parties). Also set riskLevel to exactly "LOW", "MEDIUM", or "HIGH" based on how
				risky the terms are for the client.

				Put concrete concerns (one-sided indemnity, unlimited liability, unusual auto-renew,
				missing signatures, vague obligations) in validationIssues. You are not giving formal
				legal advice, note that a licensed attorney should review anything material. Never
				invent terms that are not in the document.
				""";
	}

	@Override
	protected Object[] tools() {
		return new Object[0];
	}

}
