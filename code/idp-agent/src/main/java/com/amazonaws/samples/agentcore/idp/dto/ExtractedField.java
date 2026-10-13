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

/**
 * A single field extracted from a document.
 *
 * @param name field name, e.g. {@code invoiceNumber}
 * @param value extracted value as text
 * @param confidence model confidence in this field, 0.0 to 1.0
 */
public record ExtractedField(String name, String value, double confidence) {
}
