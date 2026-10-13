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
 * Utilities for coercing model text into parseable JSON.
 *
 * <p>
 * Some models wrap structured output in markdown code fences (for example
 * <code>```json ... ```</code>) or add a sentence of prose around it. Spring AI's
 * {@code BeanOutputConverter} expects raw JSON, so we extract the outermost JSON object or
 * array before converting. This makes structured output robust across models.
 */
final class LlmJson {

	private LlmJson() {
	}

	/**
	 * Extract the outermost JSON object or array from model output, tolerating code fences
	 * and surrounding prose.
	 * @param text raw model text
	 * @return the JSON substring, or {@code "{}"} when none is found
	 */
	static String extract(String text) {
		if (text == null || text.isBlank()) {
			return "{}";
		}
		String t = text.trim();
		int firstObj = t.indexOf('{');
		int firstArr = t.indexOf('[');
		int start = (firstObj < 0) ? firstArr : (firstArr < 0 ? firstObj : Math.min(firstObj, firstArr));
		if (start < 0) {
			return "{}";
		}
		char close = (t.charAt(start) == '{') ? '}' : ']';
		int end = t.lastIndexOf(close);
		return (end > start) ? t.substring(start, end + 1) : t.substring(start);
	}

}
