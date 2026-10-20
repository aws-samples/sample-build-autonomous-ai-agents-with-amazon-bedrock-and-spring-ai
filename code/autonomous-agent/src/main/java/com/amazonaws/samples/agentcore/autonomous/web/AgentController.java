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
package com.amazonaws.samples.agentcore.autonomous.web;

import java.util.List;

import com.amazonaws.samples.agentcore.autonomous.dto.AgentInfo;
import com.amazonaws.samples.agentcore.autonomous.dto.ChatResponse;
import com.amazonaws.samples.agentcore.autonomous.dto.PromptRequest;
import com.amazonaws.samples.agentcore.autonomous.service.AgentService;
import jakarta.validation.Valid;
import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.context.AgentCoreContext;
import org.springaicommunity.agentcore.context.AgentCoreHeaders;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web entry points for the agent.
 *
 * <ul>
 * <li>{@code @AgentCoreInvocation}, the single method AgentCore Runtime calls at
 * {@code POST /invocations}; the session id comes from the runtime headers.</li>
 * <li>{@code POST /api/chat}, streaming endpoint for the React UI (token-by-token).</li>
 * <li>{@code POST /api/chat/sync}, aggregated reply for simple clients and tests.</li>
 * <li>{@code GET /api/agent/info}, self-description rendered by the UI on load.</li>
 * </ul>
 *
 * All business logic lives in {@link AgentService}; the controller only adapts transport.
 */
@RestController
public class AgentController {

	private final AgentService agentService;

	private final String model;

	public AgentController(AgentService agentService,
			@Value("${spring.ai.bedrock.converse.chat.options.model:unknown}") String model) {
		this.agentService = agentService;
		this.model = model;
	}

	@AgentCoreInvocation
	public ChatResponse invoke(PromptRequest request, AgentCoreContext context) {
		String sessionId = context.getHeader(AgentCoreHeaders.SESSION_ID);
		String content = this.agentService.chat(request.prompt(), sessionId, request.guardrailsEnabled(),
				request.memoryEnabled());
		return ChatResponse.of(sessionId, content);
	}

	@PostMapping(value = "/api/chat", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
	public Flux<String> chatStream(@Valid @RequestBody PromptRequest request) {
		return this.agentService.streamChat(request.prompt(), request.sessionId(), request.guardrailsEnabled(),
				request.memoryEnabled());
	}

	@PostMapping("/api/chat/sync")
	public ChatResponse chatSync(@Valid @RequestBody PromptRequest request) {
		String content = this.agentService.chat(request.prompt(), request.sessionId(), request.guardrailsEnabled(),
				request.memoryEnabled());
		return ChatResponse.of(request.sessionId(), content);
	}

	@GetMapping("/api/agent/info")
	public AgentInfo info() {
		return new AgentInfo("Autonomous Agent",
				"Plans work by reasoning over your request, browsing the web, running code, calling tools for facts, and remembering the conversation.",
				List.of("Planning", "Tool use", "Web browsing", "Code execution", "Memory", "Evaluation"),
				List.of("getCurrentDateTime", "getWeather", "estimateFlightPrice", "getPointsOfInterest", "browseUrl",
						"takeScreenshot", "clickElement", "fillForm", "evaluateScript", "executeCode"),
				this.model);
	}

}
