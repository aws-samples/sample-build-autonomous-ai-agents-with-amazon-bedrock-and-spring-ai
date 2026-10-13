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

/**
 * A workflow rule that fired against a processed document.
 *
 * @param ruleId identifier of the rule
 * @param severity rule severity
 * @param message human-readable explanation surfaced to the caller and alerts
 */
public record RuleResult(String ruleId, RuleSeverity severity, String message) {
}
