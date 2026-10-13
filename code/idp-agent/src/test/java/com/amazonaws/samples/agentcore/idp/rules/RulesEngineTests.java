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

import java.util.List;

import com.amazonaws.samples.agentcore.idp.agent.AgentType;
import com.amazonaws.samples.agentcore.idp.dto.ExtractedField;
import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import com.amazonaws.samples.agentcore.idp.rules.IdpProperties.RuleDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline tests for the SpEL-based rules engine. No AWS or Spring context required.
 */
class RulesEngineTests {

	private static final RuleDefinition NO_SIGNATURE = new RuleDefinition("legal-unsigned",
			"A legal document should be signed", "LAWYER",
			"field('signaturePresent') == 'false' || missing('signaturePresent')", "ALERT",
			"Document appears unsigned.");

	private static final RuleDefinition LOW_CONFIDENCE = new RuleDefinition("low-confidence", "Low confidence", "ALL",
			"confidence < 0.6", "WARN", "Low extraction confidence.");

	private final RulesEngine engine = new RulesEngine(new IdpProperties(List.of(NO_SIGNATURE, LOW_CONFIDENCE)));

	@Test
	void firesAlertWhenDocumentHasNoSignature() {
		ProcessedDocument doc = new ProcessedDocument("LEASE", "A lease", "",
				List.of(new ExtractedField("signaturePresent", "false", 0.95)), 0.95, List.of());

		List<RuleResult> results = this.engine.evaluate(AgentType.LAWYER, doc);

		assertThat(results).extracting(RuleResult::ruleId).contains("legal-unsigned");
		assertThat(results).extracting(RuleResult::severity).contains(RuleSeverity.ALERT);
	}

	@Test
	void doesNotFireWhenDocumentIsSigned() {
		ProcessedDocument doc = new ProcessedDocument("LEASE", "A lease", "",
				List.of(new ExtractedField("signaturePresent", "true", 0.95)), 0.95, List.of());

		List<RuleResult> results = this.engine.evaluate(AgentType.LAWYER, doc);

		assertThat(results).extracting(RuleResult::ruleId).doesNotContain("legal-unsigned");
	}

	@Test
	void allScopedRuleAppliesToEveryAgent() {
		ProcessedDocument doc = new ProcessedDocument("LOAN_APPLICATION", "An application", "", List.of(), 0.4,
				List.of());

		List<RuleResult> results = this.engine.evaluate(AgentType.MORTGAGE, doc);

		assertThat(results).extracting(RuleResult::ruleId).contains("low-confidence");
	}

	@Test
	void invalidConditionIsSkippedNotFatal() {
		RuleDefinition broken = new RuleDefinition("broken", "bad", "ALL", "this is not (valid spel", "INFO", "x");
		RulesEngine engineWithBadRule = new RulesEngine(new IdpProperties(List.of(broken, LOW_CONFIDENCE)));

		ProcessedDocument doc = new ProcessedDocument("OTHER", "doc", "", List.of(), 0.3, List.of());

		List<RuleResult> results = engineWithBadRule.evaluate(AgentType.GENERAL, doc);

		assertThat(results).extracting(RuleResult::ruleId).containsExactly("low-confidence");
	}

}
