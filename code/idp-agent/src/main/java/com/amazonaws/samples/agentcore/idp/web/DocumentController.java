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
package com.amazonaws.samples.agentcore.idp.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.amazonaws.samples.agentcore.idp.agent.AgentType;
import com.amazonaws.samples.agentcore.idp.dto.DocumentRequest;
import com.amazonaws.samples.agentcore.idp.dto.DocumentResult;
import com.amazonaws.samples.agentcore.idp.service.DocumentOrchestrator;
import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.context.AgentCoreContext;
import org.springaicommunity.agentcore.context.AgentCoreHeaders;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * HTTP surface for the IDP system.
 *
 * <ul>
 * <li>{@code POST /invocations}, the AgentCore Runtime contract entry point (JSON).</li>
 * <li>{@code POST /api/documents}, interactive multipart upload: choose an agent, attach
 * a file, optionally ask a question.</li>
 * <li>{@code GET /api/agents}, list available specialist agents.</li>
 * </ul>
 */
@RestController
public class DocumentController {

	private final DocumentOrchestrator orchestrator;

	public DocumentController(DocumentOrchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}

	@AgentCoreInvocation
	public DocumentResult process(DocumentRequest request, AgentCoreContext context) {
		String sessionId = context.getHeader(AgentCoreHeaders.SESSION_ID);
		return this.orchestrator.process(request, conversationId(sessionId));
	}

	@PostMapping(path = "/api/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public DocumentResult upload(@RequestPart("file") MultipartFile file,
			@RequestParam(value = "agentType", required = false) String agentType,
			@RequestParam(value = "question", required = false) String question,
			@RequestParam(value = "sessionId", required = false) String sessionId) throws IOException {

		String session = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;
		return processOne(file, agentType, question, session);
	}

	private DocumentResult processOne(MultipartFile file, String agentType, String question, String session)
			throws IOException {
		String mimeType = file.getContentType();
		byte[] bytes = file.getBytes();
		DocumentRequest request;
		if (isTextLike(mimeType)) {
			request = new DocumentRequest(agentType, question, new String(bytes, StandardCharsets.UTF_8), null, null);
		}
		else {
			request = new DocumentRequest(agentType, question, null, mimeType,
					Base64.getEncoder().encodeToString(bytes));
		}
		return this.orchestrator.process(request, conversationId(session));
	}

	/**
	 * Batch upload: process several documents in one request with the same specialist
	 * (or auto-classification). Each file is handled independently and returns its own
	 * result, so one failure does not sink the batch.
	 */
	@PostMapping(path = "/api/documents/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public List<DocumentResult> uploadBatch(@RequestPart("files") MultipartFile[] files,
			@RequestParam(value = "agentType", required = false) String agentType,
			@RequestParam(value = "sessionId", required = false) String sessionId) throws IOException {

		String session = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;
		List<DocumentResult> results = new ArrayList<>();
		for (MultipartFile file : files) {
			results.add(processOne(file, agentType, null, session));
		}
		return results;
	}

	@GetMapping("/api/agents")
	public List<String> agents() {
		return Arrays.stream(AgentType.values()).map(Enum::name).toList();
	}

	private String conversationId(String sessionId) {
		return "doc:" + ((sessionId == null || sessionId.isBlank()) ? "default" : sessionId);
	}

	private boolean isTextLike(String mimeType) {
		if (mimeType == null || mimeType.isBlank()) {
			return true;
		}
		return mimeType.startsWith("text/") || mimeType.equals(MediaType.APPLICATION_JSON_VALUE);
	}

}
