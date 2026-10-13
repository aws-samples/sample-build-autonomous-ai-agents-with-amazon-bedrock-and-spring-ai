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

/**
 * Everything an agent needs to process one document.
 *
 * @param type the resolved agent type
 * @param question optional question for the agent to answer (may be null)
 * @param text plain-text document content (may be null when media is supplied)
 * @param mimeType MIME type of the media bytes (may be null)
 * @param mediaBytes raw image/PDF bytes for multimodal reading (may be null)
 * @param conversationId memory conversation id, {@code actor:session}
 */
public record DocumentContext(AgentType type, String question, String text, String mimeType, byte[] mediaBytes,
		String conversationId) {

	public boolean hasMedia() {
		return this.mediaBytes != null && this.mediaBytes.length > 0 && this.mimeType != null;
	}

	public boolean hasText() {
		return this.text != null && !this.text.isBlank();
	}

	public boolean hasQuestion() {
		return this.question != null && !this.question.isBlank();
	}

}
