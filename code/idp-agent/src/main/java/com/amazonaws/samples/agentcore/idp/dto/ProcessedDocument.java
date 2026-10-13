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

import java.util.List;

/**
 * Structured extraction produced by a specialist agent. Spring AI maps the model's
 * response directly onto this record via {@code .entity(ProcessedDocument.class)}.
 *
 * @param documentType the kind of document the agent identified, e.g. LEASE, NDA,
 * LOAN_APPLICATION, AUTO_POLICY, W2, OTHER
 * @param summary one or two sentence summary of the document
 * @param answer the agent's answer to the caller's question (empty if none was asked)
 * @param fields extracted key fields
 * @param confidence overall confidence in the extraction, 0.0 to 1.0
 * @param validationIssues human-readable validation problems found by the agent (empty if
 * none)
 */
public record ProcessedDocument(String documentType, String summary, String answer, List<ExtractedField> fields,
		double confidence, List<String> validationIssues) {
}
