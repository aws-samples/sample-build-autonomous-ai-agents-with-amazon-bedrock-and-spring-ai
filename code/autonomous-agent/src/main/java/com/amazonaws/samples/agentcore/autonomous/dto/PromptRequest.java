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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload accepted by the agent's invocation and REST endpoints.
 *
 * @param prompt the natural-language instruction from the caller
 * @param sessionId optional client-supplied session id used to scope conversation memory;
 * when absent on the REST path a per-call session is used, and on the AgentCore Runtime
 * path the session id is taken from the request headers instead
 * @param guardrails when true (default), input guardrails screen the prompt before it
 * reaches the model
 * @param memory when true (default), conversation memory is applied for the session
 */
public record PromptRequest(@NotBlank(message = "prompt must not be blank") @Size(max = 8000,
		message = "prompt must be 8000 characters or fewer") String prompt, String sessionId, Boolean guardrails,
		Boolean memory) {

	/** Backward-compatible constructor: guardrails and memory default to on. */
	public PromptRequest(String prompt, String sessionId) {
		this(prompt, sessionId, Boolean.TRUE, Boolean.TRUE);
	}

	/** Whether guardrails are enabled (default true when unset). */
	public boolean guardrailsEnabled() {
		return this.guardrails == null || this.guardrails;
	}

	/** Whether conversation memory is enabled (default true when unset). */
	public boolean memoryEnabled() {
		return this.memory == null || this.memory;
	}
}
