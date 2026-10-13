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
package com.amazonaws.samples.agentcore.idp.rules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.samples.agentcore.idp.dto.ExtractedField;
import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;

/**
 * The root object SpEL rule conditions evaluate against. Exposes extraction data as
 * properties ({@code documentType}, {@code confidence}, {@code validationIssues},
 * {@code fields}) and convenience methods ({@code field('name')}, {@code missing('name')})
 * so rules read naturally, for example:
 *
 * <pre>{@code field('signaturePresent') == 'false' || missing('signaturePresent')}</pre>
 */
public class RuleContext {

	private final ProcessedDocument doc;

	private final Map<String, String> fields;

	private final Map<String, String> lowerKeyed;

	public RuleContext(ProcessedDocument doc) {
		this.doc = doc;
		this.fields = new LinkedHashMap<>();
		this.lowerKeyed = new LinkedHashMap<>();
		if (doc.fields() != null) {
			for (ExtractedField f : doc.fields()) {
				if (f != null && f.name() != null) {
					this.fields.put(f.name(), f.value());
					this.lowerKeyed.put(f.name().toLowerCase(), f.value() == null ? "" : f.value());
				}
			}
		}
	}

	public String getDocumentType() {
		return this.doc.documentType();
	}

	public double getConfidence() {
		return this.doc.confidence();
	}

	public String getAnswer() {
		return this.doc.answer();
	}

	public List<String> getValidationIssues() {
		return this.doc.validationIssues() == null ? List.of() : this.doc.validationIssues();
	}

	public Map<String, String> getFields() {
		return this.fields;
	}

	/**
	 * Case-insensitive field lookup. Returns an empty string when the field is absent.
	 * @param name field name
	 * @return field value, or empty string
	 */
	public String field(String name) {
		return this.lowerKeyed.getOrDefault(name == null ? "" : name.toLowerCase(), "");
	}

	/**
	 * @param name field name
	 * @return true when the field is missing or blank
	 */
	public boolean missing(String name) {
		return field(name).isBlank();
	}

}
