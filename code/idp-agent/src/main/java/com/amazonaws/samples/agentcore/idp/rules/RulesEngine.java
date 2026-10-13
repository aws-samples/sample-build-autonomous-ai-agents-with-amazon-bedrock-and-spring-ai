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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.amazonaws.samples.agentcore.idp.agent.AgentType;
import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import com.amazonaws.samples.agentcore.idp.rules.IdpProperties.RuleDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Evaluates the configured workflow rules against a processed document. Rules whose
 * {@code agentType} is {@code ALL} or matches the handling agent are evaluated; those
 * whose SpEL condition is true become {@link RuleResult}s.
 *
 * <p>
 * Compiled expressions are cached. A rule whose expression fails to parse or evaluate is
 * logged and skipped so one bad rule cannot break processing.
 */
@Component
public class RulesEngine {

	private static final Logger logger = LoggerFactory.getLogger(RulesEngine.class);

	private static final String MATCH_ALL = "ALL";

	private final List<RuleDefinition> rules;

	private final SpelExpressionParser parser = new SpelExpressionParser();

	private final Map<String, Expression> compiled = new ConcurrentHashMap<>();

	public RulesEngine(IdpProperties properties) {
		this.rules = properties.rules();
		// Pre-compile and validate at startup; surface bad expressions early.
		for (RuleDefinition rule : this.rules) {
			try {
				this.compiled.put(rule.id(), this.parser.parseExpression(rule.condition()));
			}
			catch (RuntimeException ex) {
				logger.error("Rule '{}' has an invalid SpEL condition and will be skipped: {}", rule.id(),
						rule.condition(), ex);
			}
		}
		logger.info("Loaded {} workflow rule(s)", this.compiled.size());
	}

	/**
	 * Evaluate all applicable rules.
	 * @param agentType the agent that processed the document
	 * @param document the extraction to test
	 * @return triggered rules, in declaration order
	 */
	public List<RuleResult> evaluate(AgentType agentType, ProcessedDocument document) {
		List<RuleResult> results = new ArrayList<>();
		RuleContext root = new RuleContext(document);
		StandardEvaluationContext evalContext = new StandardEvaluationContext(root);

		for (RuleDefinition rule : this.rules) {
			if (!appliesTo(rule, agentType)) {
				continue;
			}
			Expression expression = this.compiled.get(rule.id());
			if (expression == null) {
				continue;
			}
			try {
				Boolean fired = expression.getValue(evalContext, Boolean.class);
				if (Boolean.TRUE.equals(fired)) {
					results.add(new RuleResult(rule.id(), parseSeverity(rule.severity()), rule.message()));
				}
			}
			catch (RuntimeException ex) {
				logger.warn("Rule '{}' failed to evaluate and was skipped", rule.id(), ex);
			}
		}
		return results;
	}

	private boolean appliesTo(RuleDefinition rule, AgentType agentType) {
		String target = rule.agentType();
		return target == null || target.isBlank() || MATCH_ALL.equalsIgnoreCase(target)
				|| target.equalsIgnoreCase(agentType.name());
	}

	private RuleSeverity parseSeverity(String value) {
		try {
			return RuleSeverity.valueOf(value.trim().toUpperCase());
		}
		catch (RuntimeException ex) {
			return RuleSeverity.INFO;
		}
	}

}
