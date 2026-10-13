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
package com.amazonaws.samples.agentcore.autonomous.dto;

import java.util.List;

/**
 * Self-description the UI fetches on load to render the agent's capabilities and the tools
 * it can call.
 *
 * @param name display name
 * @param description short summary of what the agent does
 * @param capabilities the core agent capabilities exercised (planning, tool use, memory)
 * @param tools the tools the model may call
 * @param model the Bedrock model id backing the agent
 */
public record AgentInfo(String name, String description, List<String> capabilities, List<String> tools,
		String model) {
}
