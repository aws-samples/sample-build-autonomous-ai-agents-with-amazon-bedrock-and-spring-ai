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
package com.amazonaws.samples.agentcore.autonomous.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * Lightweight, dependency-free input guardrail that screens a prompt before it reaches the
 * model. This is a demonstration of the guardrail pattern that can be toggled per request
 * from the UI. In production you would front this with Amazon Bedrock Guardrails (denied
 * topics, content filters, PII redaction) and keep this as a fast local pre-check.
 */
@Service
public class GuardrailService {

	// Categories of clearly disallowed intent. Kept intentionally small and readable.
	private static final List<Pattern> BLOCKED = List.of(
			Pattern.compile("\\b(make|build|create)\\b.{0,30}\\b(bomb|explosive|bioweapon|nerve agent)\\b",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\b(how to)\\b.{0,40}\\b(hack|ddos|steal|launder)\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\b(kill|harm)\\b.{0,20}\\b(myself|yourself)\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("ignore (all )?(previous|prior) instructions", Pattern.CASE_INSENSITIVE));

	/**
	 * Screen a prompt. Returns a human-readable block reason when the prompt should be
	 * refused, or an empty optional when it is allowed.
	 * @param prompt the user prompt
	 * @return the block reason, or empty if allowed
	 */
	public Optional<String> screen(String prompt) {
		if (prompt == null || prompt.isBlank()) {
			return Optional.empty();
		}
		for (Pattern p : BLOCKED) {
			if (p.matcher(prompt).find()) {
				return Optional.of("This request was blocked by the input guardrail because it appears to ask for "
						+ "harmful, illegal, or unsafe content, or attempts a prompt injection.");
			}
		}
		return Optional.empty();
	}

}
