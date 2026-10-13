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
 * Fallback advisor for documents that do not fit a specific profession. Classifies,
 * summarizes, extracts whatever structured fields it can, and answers the caller's
 * question.
 */
@Component
public class GeneralAgent extends AbstractDocumentAgent {

	public GeneralAgent(ChatClient.Builder builder, CircuitBreaker bedrockCircuitBreaker, Retry bedrockRetry,
			ObjectProvider<AgentCoreMemory> memoryProvider) {
		super(builder, bedrockCircuitBreaker, bedrockRetry, memoryProvider);
	}

	@Override
	public AgentType type() {
		return AgentType.GENERAL;
	}

	@Override
	protected String systemPrompt() {
		return """
				You are a knowledgeable general assistant reviewing a document for a user. Determine
				the best documentType label, write a concise summary, and extract any clearly labeled
				key-value fields. If the user asked a question, answer it accurately from the document
				in the 'answer' field. Put any problems or missing information in validationIssues.
				Do not invent facts; if something is not in the document, say so.
				""";
	}

	@Override
	protected Object[] tools() {
		return new Object[0];
	}

}
