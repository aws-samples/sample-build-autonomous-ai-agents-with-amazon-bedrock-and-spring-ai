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
package com.amazonaws.samples.agentcore.autonomous.config;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreAsyncClient;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;

import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pins the Bedrock AgentCore data-plane clients to the same region the model uses.
 *
 * <p>
 * The Browser and Code Interpreter starters each fall back to
 * {@code BedrockAgentCoreClient.create()}, which resolves the region from the default AWS
 * chain. Declaring the clients here (both starters back off through
 * {@code @ConditionalOnMissingBean}) makes startup deterministic: the tools always talk to
 * the region configured for the chat model rather than whatever the ambient environment
 * happens to provide. The Evaluations starter reuses the same client too.
 */
@Configuration
public class AgentCoreToolsConfig {

	private final Region region;

	public AgentCoreToolsConfig(@Value("${spring.ai.bedrock.aws.region:us-east-1}") String region) {
		this.region = Region.of(region);
	}

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean
	BedrockAgentCoreClient bedrockAgentCoreClient() {
		return BedrockAgentCoreClient.builder().region(this.region).build();
	}

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean
	BedrockAgentCoreAsyncClient bedrockAgentCoreAsyncClient() {
		return BedrockAgentCoreAsyncClient.builder().region(this.region).build();
	}

	/**
	 * Keep a single failing tool call from aborting the whole turn. When a tool throws (for
	 * example the managed Code Interpreter or Browser service is not provisioned, or the
	 * model emits malformed tool arguments), the error is returned to the model so it can
	 * recover and still answer, instead of surfacing the generic fallback to the user.
	 */
	@Bean
	@ConditionalOnMissingBean
	ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
		return DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build();
	}

}
