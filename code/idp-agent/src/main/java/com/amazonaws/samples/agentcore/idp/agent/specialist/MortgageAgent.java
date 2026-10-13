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
 * Mortgage / loan specialist. Reviews loan applications, pre-approval letters, closing
 * disclosures, pay stubs, and similar documents, and checks completeness for underwriting.
 * Emits {@code incomeDocumented} and {@code dtiRatio} signals for the workflow rules.
 */
@Component
public class MortgageAgent extends AbstractDocumentAgent {

	public MortgageAgent(ChatClient.Builder builder, CircuitBreaker bedrockCircuitBreaker, Retry bedrockRetry,
			ObjectProvider<AgentCoreMemory> memoryProvider) {
		super(builder, bedrockCircuitBreaker, bedrockRetry, memoryProvider);
	}

	@Override
	public AgentType type() {
		return AgentType.MORTGAGE;
	}

	@Override
	protected String systemPrompt() {
		return """
				You are a mortgage loan officer reviewing a borrower's document for completeness and
				risk. Set documentType (e.g. LOAN_APPLICATION, PRE_APPROVAL, CLOSING_DISCLOSURE,
				PAY_STUB, BANK_STATEMENT, OTHER). Write a clear summary, and answer the client's
				question in the 'answer' field if one was asked.

				Extract these fields when present: borrowerName, loanAmount, propertyValue,
				interestRate, loanTermYears, monthlyIncome, monthlyDebt, creditScore, loanType.
				Compute and set dtiRatio (monthly debt / monthly income, as a 0–1 decimal string)
				when income and debt are available. Set incomeDocumented to "true" only if income is
				supported by pay stubs, tax returns, or bank statements in the document; otherwise
				"false".

				Put underwriting concerns in validationIssues (high DTI, missing income proof, LTV
				over 80%, low credit score). Provide guidance, not a lending decision, and never
				invent figures that are not in the document.
				""";
	}

	@Override
	protected Object[] tools() {
		return new Object[0];
	}

}
