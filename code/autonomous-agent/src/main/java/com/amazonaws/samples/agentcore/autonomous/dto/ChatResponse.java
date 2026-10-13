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
package com.amazonaws.samples.agentcore.autonomous.dto;

import java.time.Instant;

/**
 * Aggregated (non-streaming) chat response returned by the synchronous REST endpoint.
 *
 * @param sessionId the session the turn was scoped to
 * @param content the full assistant reply
 * @param timestamp when the response was produced
 */
public record ChatResponse(String sessionId, String content, Instant timestamp) {

	public static ChatResponse of(String sessionId, String content) {
		return new ChatResponse(sessionId, content, Instant.now());
	}

}
