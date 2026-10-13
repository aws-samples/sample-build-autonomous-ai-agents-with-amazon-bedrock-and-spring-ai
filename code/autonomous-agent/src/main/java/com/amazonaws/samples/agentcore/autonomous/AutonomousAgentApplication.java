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
package com.amazonaws.samples.agentcore.autonomous;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the autonomous agent.
 *
 * <p>
 * The {@code spring-ai-agentcore-runtime-starter} dependency auto-configures the
 * AgentCore Runtime contract on startup: a {@code POST /invocations} endpoint that
 * dispatches to the method annotated with {@code @AgentCoreInvocation}, and a
 * {@code GET /ping} health endpoint backed by Spring Boot Actuator.
 *
 * <p>
 * {@code @ConfigurationPropertiesScan} binds {@code AgentProperties} so agent behavior is
 * configured from the environment (twelve-factor).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AutonomousAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutonomousAgentApplication.class, args);
	}

}
