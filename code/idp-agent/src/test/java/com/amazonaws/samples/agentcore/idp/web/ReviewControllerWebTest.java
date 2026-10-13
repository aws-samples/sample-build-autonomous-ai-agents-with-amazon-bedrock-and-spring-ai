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
import java.util.Optional;

import com.amazonaws.samples.agentcore.idp.audit.AuditService;
import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import com.amazonaws.samples.agentcore.idp.review.ReviewItem;
import com.amazonaws.samples.agentcore.idp.review.ReviewQueue;
import com.amazonaws.samples.agentcore.idp.review.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer test for {@link ReviewController}. The controller is wired up with a standalone
 * MockMvc against mocked {@link ReviewQueue} and {@link AuditService} collaborators, so it
 * runs offline with no AWS access.
 *
 * <p>
 * Note: Spring Boot 4 moved the {@code @WebMvcTest} slice into the {@code
 * spring-boot-webmvc-test} module, which is not on this module's test classpath (only
 * {@code spring-boot-starter-test} is, and the pom must not be modified). A standalone
 * MockMvc setup, with an explicit Bean Validation validator, provides the same offline
 * controller coverage (including the {@code @Valid} 400 paths) without that dependency.
 */
class ReviewControllerWebTest {

	private ReviewQueue reviewQueue;

	private AuditService auditService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.reviewQueue = mock(ReviewQueue.class);
		this.auditService = mock(AuditService.class);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		this.mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(this.reviewQueue, this.auditService))
			.setValidator(validator)
			.build();
	}

	private ProcessedDocument doc() {
		return new ProcessedDocument("LEASE", "summary", "answer", List.of(), 0.5, List.of());
	}

	@Test
	void pendingReturnsQueuedItems() throws Exception {
		ReviewItem item = ReviewItem.pending("item-1", "LAWYER", "conv-1", doc(), List.of(),
				List.of("Low confidence"));
		given(this.reviewQueue.pending()).willReturn(List.of(item));

		this.mockMvc.perform(get("/api/review"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].id").value("item-1"))
			.andExpect(jsonPath("$[0].agentType").value("LAWYER"))
			.andExpect(jsonPath("$[0].status").value("PENDING"));
	}

	@Test
	void getReturns404WhenItemMissing() throws Exception {
		given(this.reviewQueue.find("nope")).willReturn(Optional.empty());

		this.mockMvc.perform(get("/api/review/{id}", "nope")).andExpect(status().isNotFound());
	}

	@Test
	void resolveApprovesAndRecordsAudit() throws Exception {
		ReviewItem resolved = ReviewItem
			.pending("item-2", "MORTGAGE", "conv-2", doc(), List.of(), List.of("Blocked"))
			.resolve(ReviewStatus.APPROVED, "alice", null);
		given(this.reviewQueue.resolve(eq("item-2"), eq(ReviewStatus.APPROVED), eq("alice"), any()))
			.willReturn(Optional.of(resolved));

		this.mockMvc
			.perform(post("/api/review/{id}/resolve", "item-2").contentType(MediaType.APPLICATION_JSON)
				.content("{\"decision\":\"APPROVE\",\"reviewer\":\"alice\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value("item-2"))
			.andExpect(jsonPath("$.status").value("APPROVED"))
			.andExpect(jsonPath("$.reviewer").value("alice"));

		verify(this.auditService).reviewResolved(resolved);
	}

	@Test
	void resolveWithInvalidDecisionReturns400() throws Exception {
		this.mockMvc
			.perform(post("/api/review/{id}/resolve", "item-3").contentType(MediaType.APPLICATION_JSON)
				.content("{\"decision\":\"MAYBE\",\"reviewer\":\"alice\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void resolveWithBlankReviewerReturns400() throws Exception {
		this.mockMvc
			.perform(post("/api/review/{id}/resolve", "item-4").contentType(MediaType.APPLICATION_JSON)
				.content("{\"decision\":\"APPROVE\",\"reviewer\":\"\"}"))
			.andExpect(status().isBadRequest());
	}

}
