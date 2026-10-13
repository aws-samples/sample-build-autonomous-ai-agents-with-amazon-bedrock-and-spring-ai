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

import com.amazonaws.samples.agentcore.autonomous.service.AgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link AgentController}. Uses a standalone {@link MockMvc} setup so
 * the controller, its request validation, and {@link GlobalExceptionHandler} are exercised
 * end to end with a mocked {@link AgentService} and no AWS access.
 *
 * <p>
 * Note: Spring Boot 4.1 moved the {@code @WebMvcTest} slice into the separate
 * {@code spring-boot-webmvc-test} module, which is not on this module's test classpath
 * (only {@code spring-boot-starter-test} is). A standalone MockMvc build from
 * {@code spring-test} gives equivalent coverage of the controller and advice without
 * adding dependencies.
 */
class AgentControllerWebTest {

	private AgentService agentService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.agentService = mock(AgentService.class);
		AgentController controller = new AgentController(this.agentService, "test-model");
		this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void agentInfoReturnsNameAndNonEmptyTools() throws Exception {
		this.mockMvc.perform(get("/api/agent/info"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.name").value("Autonomous Agent"))
			.andExpect(jsonPath("$.tools").isArray())
			.andExpect(jsonPath("$.tools[0]").exists())
			.andExpect(jsonPath("$.tools.length()").value(org.hamcrest.Matchers.greaterThan(0)));
	}

	@Test
	void chatSyncWithValidPromptReturnsStubbedContent() throws Exception {
		when(this.agentService.chat(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn("hello");

		this.mockMvc.perform(post("/api/chat/sync").contentType(MediaType.APPLICATION_JSON)
				.content("{\"prompt\":\"hi there\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").value("hello"));
	}

	@Test
	void chatSyncWithBlankPromptReturnsBadRequest() throws Exception {
		this.mockMvc.perform(post("/api/chat/sync").contentType(MediaType.APPLICATION_JSON)
				.content("{\"prompt\":\"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void chatSyncWithTooLongPromptReturnsBadRequest() throws Exception {
		String longPrompt = "a".repeat(8001);
		String body = "{\"prompt\":\"" + longPrompt + "\"}";

		this.mockMvc.perform(post("/api/chat/sync").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void chatSyncPassesGuardrailAndMemoryFlagsThrough() throws Exception {
		when(this.agentService.chat(anyString(), any(), anyBoolean(), anyBoolean())).thenReturn("ok");

		this.mockMvc.perform(post("/api/chat/sync").contentType(MediaType.APPLICATION_JSON)
				.content("{\"prompt\":\"hi\",\"guardrails\":false,\"memory\":false}"))
			.andExpect(status().isOk());

		verify(this.agentService).chat(eq("hi"), any(), eq(false), eq(false));
	}

}
