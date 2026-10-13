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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * A lightweight router agent. When the caller does not specify an agent type, it reads
 * the document and decides which specialist should handle it. This is the first hop in
 * the multi-agent flow: classify, then dispatch.
 */
@Component
public class ClassifierAgent {

	private static final Logger logger = LoggerFactory.getLogger(ClassifierAgent.class);

	private static final int MAX_TEXT_CHARS = 4000;

	private final ChatClient chatClient;

	private final CircuitBreaker circuitBreaker;

	private final Retry retry;

	private final BeanOutputConverter<Classification> outputConverter = new BeanOutputConverter<>(Classification.class);

	public ClassifierAgent(ChatClient.Builder builder, CircuitBreaker bedrockCircuitBreaker, Retry bedrockRetry) {
		this.circuitBreaker = bedrockCircuitBreaker;
		this.retry = bedrockRetry;
		this.chatClient = builder.defaultSystem("""
				You route a document to the right professional advisor. Choose the single best
				category from this list based on the document's subject matter:
				LAWYER (contracts, agreements, leases, legal letters),
				MORTGAGE (loan applications, pre-approvals, closing disclosures, pay stubs),
				INSURANCE (policies, declarations, claims),
				TAX (tax forms, returns, financial statements),
				GENERAL (anything else).
				Reply with only the category in the 'type' field.
				""").build();
	}

	public AgentType classify(DocumentContext context) {
		try {
			Classification classification = Retry
				.decorateSupplier(this.retry,
						CircuitBreaker.decorateSupplier(this.circuitBreaker, () -> callModel(context)))
				.get();
			AgentType type = AgentType.fromOrDefault(classification == null ? null : classification.type());
			logger.info("Classifier selected {}", type);
			return type;
		}
		catch (RuntimeException ex) {
			logger.warn("Classification failed, defaulting to GENERAL", ex);
			return AgentType.GENERAL;
		}
	}

	private Classification callModel(DocumentContext context) {
		String content = this.chatClient.prompt().user(user -> {
			user.text(buildText(context));
			if (context.hasMedia()) {
				MimeType mimeType = MimeTypeUtils.parseMimeType(context.mimeType());
				user.media(mimeType, new ByteArrayResource(context.mediaBytes()));
			}
		}).call().content();
		return this.outputConverter.convert(LlmJson.extract(content));
	}

	private String buildText(DocumentContext context) {
		String body;
		if (context.hasText()) {
			String text = context.text();
			String trimmed = text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text;
			body = "Classify this document:\n\n" + trimmed;
		}
		else {
			body = "Classify the attached document image/PDF.";
		}
		return body + "\n\n" + this.outputConverter.getFormat()
				+ "\nReturn ONLY the JSON. Do not wrap it in markdown code fences.";
	}

	/**
	 * Structured classification result.
	 *
	 * @param type one of the AgentType names
	 */
	public record Classification(String type) {
	}

}
