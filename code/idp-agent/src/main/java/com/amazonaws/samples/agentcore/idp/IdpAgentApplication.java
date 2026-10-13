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
package com.amazonaws.samples.agentcore.idp;

import com.amazonaws.samples.agentcore.idp.review.ReviewProperties;
import com.amazonaws.samples.agentcore.idp.rules.IdpProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Intelligent Document Processing (IDP) agent.
 *
 * <p>
 * Given a document (text, or an image/PDF supplied as base64), the agent classifies the
 * document, extracts structured fields, validates them with business-rule tools, and
 * returns a typed result. It runs on Amazon Bedrock AgentCore Runtime via the
 * {@code spring-ai-agentcore-runtime-starter}.
 */
@SpringBootApplication
@EnableConfigurationProperties({ IdpProperties.class, ReviewProperties.class })
public class IdpAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdpAgentApplication.class, args);
	}

}
