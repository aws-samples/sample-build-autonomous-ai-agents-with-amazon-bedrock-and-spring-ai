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
package com.amazonaws.samples.agentcore.idp.agent;

import java.util.function.Supplier;

import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import org.springaicommunity.agentcore.memory.longterm.AgentCoreMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Template base for specialist agents. Subclasses supply a system prompt and tools; this
 * class handles building the {@link ChatClient}, attaching memory when available, reading
 * text or media, mapping the response to {@link ProcessedDocument}, and wrapping the
 * Amazon Bedrock call in a retry and circuit breaker.
 */
public abstract class AbstractDocumentAgent implements DocumentAgent {

	private final ChatClient chatClient;

	private final BeanOutputConverter<ProcessedDocument> outputConverter = new BeanOutputConverter<>(
			ProcessedDocument.class);

	private final CircuitBreaker circuitBreaker;

	private final Retry retry;

	private final ObjectProvider<AgentCoreMemory> memoryProvider;

	protected AbstractDocumentAgent(ChatClient.Builder builder, CircuitBreaker circuitBreaker, Retry retry,
			ObjectProvider<AgentCoreMemory> memoryProvider) {
		this.circuitBreaker = circuitBreaker;
		this.retry = retry;
		this.memoryProvider = memoryProvider;
		ChatClient.Builder configured = builder.defaultSystem(systemPrompt());
		Object[] tools = tools();
		if (tools.length > 0) {
			configured = configured.defaultTools(tools);
		}
		this.chatClient = configured.build();
	}

	/**
	 * @return the specialist system prompt describing what to extract and validate
	 */
	protected abstract String systemPrompt();

	/**
	 * @return tool instances the model may call (may be empty)
	 */
	protected abstract Object[] tools();

	@Override
	public ProcessedDocument process(DocumentContext context) {
		Supplier<ProcessedDocument> call = () -> callModel(context);
		Supplier<ProcessedDocument> resilient = Retry.decorateSupplier(this.retry,
				CircuitBreaker.decorateSupplier(this.circuitBreaker, call));
		return resilient.get();
	}

	private ProcessedDocument callModel(DocumentContext context) {
		ChatClient.ChatClientRequestSpec spec = this.chatClient.prompt().user(user -> {
			user.text(buildUserText(context));
			if (context.hasMedia()) {
				MimeType mimeType = MimeTypeUtils.parseMimeType(context.mimeType());
				user.media(mimeType, new ByteArrayResource(context.mediaBytes()));
			}
		});

		AgentCoreMemory memory = this.memoryProvider.getIfAvailable();
		if (memory != null && context.conversationId() != null) {
			spec = spec.advisors(memory.advisors)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, context.conversationId()));
		}

		// Use call().content() + manual conversion so we can tolerate models that wrap
		// JSON in markdown code fences (a common cause of parse failures).
		String content = spec.call().content();
		return this.outputConverter.convert(LlmJson.extract(content));
	}

	private String buildUserText(DocumentContext context) {
		StringBuilder sb = new StringBuilder();
		sb.append("Process the following document. Extract the key fields, classify it, ")
			.append("and populate validationIssues with any problems you find. ")
			.append("Set each field's confidence between 0 and 1.\n\n");
		if (context.hasQuestion()) {
			sb.append("Also answer this question about the document in the 'answer' field: ")
				.append(context.question())
				.append("\n\n");
		}
		if (context.hasText()) {
			sb.append("Document text:\n").append(context.text());
		}
		else if (context.hasMedia()) {
			sb.append("The document is attached as an image/PDF.");
		}
		else {
			sb.append("No document content was provided.");
		}
		sb.append("\n\n").append(this.outputConverter.getFormat());
		sb.append("\nReturn ONLY the JSON. Do not wrap it in markdown code fences.");
		return sb.toString();
	}

}
