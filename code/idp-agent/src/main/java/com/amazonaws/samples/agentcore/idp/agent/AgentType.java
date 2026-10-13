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
 * The professional advisory agents available in the system. Each value maps to a
 * {@link DocumentAgent} bean. A user picks the expert that fits their document, a lawyer
 * for a contract, a mortgage specialist for a loan packet, an insurance advisor for a
 * policy, or lets the classifier choose.
 */
public enum AgentType {

	LAWYER, MORTGAGE, INSURANCE, TAX, GENERAL;

	/**
	 * Lenient parser used for caller-supplied values. Falls back to {@link #GENERAL} for
	 * null or unknown input.
	 * @param value requested type, case-insensitive
	 * @return the matching agent type, or GENERAL
	 */
	public static AgentType fromOrDefault(String value) {
		if (value == null || value.isBlank()) {
			return GENERAL;
		}
		try {
			return AgentType.valueOf(value.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			return GENERAL;
		}
	}

}
