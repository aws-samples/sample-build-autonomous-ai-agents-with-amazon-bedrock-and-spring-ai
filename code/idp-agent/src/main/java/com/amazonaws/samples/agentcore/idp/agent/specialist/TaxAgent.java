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
 * Tax / financial advisor. Reviews tax forms, returns, and financial statements, explains
 * them, and flags missing or inconsistent figures. Emits a {@code filingComplete} signal
 * for the workflow rules.
 */
@Component
public class TaxAgent extends AbstractDocumentAgent {

	public TaxAgent(ChatClient.Builder builder, CircuitBreaker bedrockCircuitBreaker, Retry bedrockRetry,
			ObjectProvider<AgentCoreMemory> memoryProvider) {
		super(builder, bedrockCircuitBreaker, bedrockRetry, memoryProvider);
	}

	@Override
	public AgentType type() {
		return AgentType.TAX;
	}

	@Override
	protected String systemPrompt() {
		return """
				You are a tax advisor reviewing a tax or financial document. Set documentType
				(e.g. W2, 1099, FORM_1040, P_AND_L, BALANCE_SHEET, OTHER). Summarize it in plain
				language and answer the client's question in the 'answer' field if one was asked.

				Extract these fields when present: taxpayerName, taxYear, filingStatus, grossIncome,
				taxableIncome, totalTax, withholding, refundOrDue. Set filingComplete to "true" only
				if the document has the figures needed to file/assess; otherwise "false".

				Put problems in validationIssues (missing fields, figures that do not add up,
				mismatched totals, prior-year data). Provide general guidance, not formal tax advice,
				and never invent numbers that are not in the document.
				""";
	}

	@Override
	protected Object[] tools() {
		return new Object[0];
	}

}
