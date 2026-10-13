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

import java.util.List;

import com.amazonaws.samples.agentcore.idp.dto.DocumentResult;
import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import com.amazonaws.samples.agentcore.idp.service.DocumentOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer test for {@link DocumentController}. The controller is wired up with a
 * standalone MockMvc against a mocked {@link DocumentOrchestrator}, so it exercises the
 * real HTTP surface (routing, multipart binding, JSON serialization) with no AWS access
 * or model calls.
 *
 * <p>
 * Note: Spring Boot 4 moved the {@code @WebMvcTest} slice into the {@code
 * spring-boot-webmvc-test} module, which is not on this module's test classpath (only
 * {@code spring-boot-starter-test} is, and the pom must not be modified). A standalone
 * MockMvc setup provides the same offline controller coverage without that dependency.
 */
class DocumentControllerWebTest {

	private DocumentOrchestrator orchestrator;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.orchestrator = mock(DocumentOrchestrator.class);
		this.mockMvc = MockMvcBuilders.standaloneSetup(new DocumentController(this.orchestrator)).build();
	}

	@Test
	void listAgentsReturnsAllSpecialists() throws Exception {
		this.mockMvc.perform(get("/api/agents"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$", org.hamcrest.Matchers.hasItems("LAWYER", "MORTGAGE", "INSURANCE", "TAX",
					"GENERAL")));
	}

	@Test
	void uploadReturnsStubbedResultThatNeedsReview() throws Exception {
		ProcessedDocument extraction = new ProcessedDocument("LEASE", "A residential lease", "", List.of(), 0.42,
				List.of());
		DocumentResult stub = new DocumentResult("LAWYER", extraction, List.of(), false, true, "review-123",
				List.of("Low extraction confidence"));
		given(this.orchestrator.process(any(), any())).willReturn(stub);

		MockMultipartFile file = new MockMultipartFile("file", "lease.txt", "text/plain",
				"LEASE AGREEMENT between the parties.".getBytes());

		this.mockMvc.perform(multipart("/api/documents").file(file).param("agentType", "LAWYER"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.agentType").value("LAWYER"))
			.andExpect(jsonPath("$.needsReview").value(true))
			.andExpect(jsonPath("$.reviewReasons").isArray())
			.andExpect(jsonPath("$.reviewReasons[0]").value("Low extraction confidence"));
	}

}
