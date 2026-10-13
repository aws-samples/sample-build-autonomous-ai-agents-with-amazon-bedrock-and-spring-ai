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
package com.amazonaws.samples.agentcore.idp.dto;

/**
 * A document to process.
 *
 * <p>
 * Provide either {@code content} (plain text, for example OCR output) or
 * {@code base64Content} with its {@code mimeType} (an image or PDF the multimodal model
 * reads directly). {@code agentType} is optional, when omitted, the classifier agent
 * picks the specialist. {@code question} is optional, when present, the agent answers it
 * about the document in addition to extracting fields.
 *
 * @param agentType requested specialist (LAWYER, MORTGAGE, INSURANCE, TAX, GENERAL) or
 * null to auto-classify
 * @param question optional natural-language question for the agent to answer
 * @param content plain-text document content (optional if base64Content is set)
 * @param mimeType MIME type of base64Content, e.g. {@code image/png} or
 * {@code application/pdf}
 * @param base64Content base64-encoded document bytes (optional)
 */
public record DocumentRequest(String agentType, String question, String content, String mimeType,
		String base64Content) {
}
