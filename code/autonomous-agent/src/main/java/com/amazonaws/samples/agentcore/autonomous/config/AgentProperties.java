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

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized agent configuration (twelve-factor: config in the environment).
 *
 * <p>
 * Every value has a safe default and can be overridden via an environment variable or
 * {@code application.properties}, so the same immutable image runs unchanged across
 * environments.
 */
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

	/**
	 * Default system prompt used when {@code agent.system-prompt} is not set.
	 */
	public static final String DEFAULT_SYSTEM_PROMPT = """
			You are an autonomous assistant that plans work in steps and uses the tools that are
			available to you to get things done. Only call a tool that has actually been provided
			to you in this session; never invent or assume a tool exists.
			- If web browsing tools are available, use them to read live web pages and capture what you see.
			- If a code interpreter tool is available, use it to run Python, JavaScript, or TypeScript
			  for calculations, data analysis, and charts, and save charts with a unique filename.
			- If no code or browsing tool is available, do the work directly: write clear, correct code
			  and walk through the result yourself, and answer from your own knowledge.
			Use the built-in helpers for the current time, weather, flight prices, and points of interest
			when they are provided. Remember what the user told you earlier in the conversation.
			Be concise and explain the reasoning behind your recommendations.
			""";

	/**
	 * Maximum time to wait for a complete model response before failing the turn.
	 */
	private Duration responseTimeout = Duration.ofSeconds(60);

	/**
	 * System prompt that defines the agent's role and behavior.
	 */
	private String systemPrompt = DEFAULT_SYSTEM_PROMPT;

	/**
	 * CORS origins allowed to call the REST API (the React UI). Lock down in production.
	 */
	private List<String> corsAllowedOrigins = List.of("http://localhost:5173");

	/**
	 * Whether the managed AgentCore Browser and Code Interpreter tools are registered on
	 * the ChatClient. These require the corresponding AWS AgentCore managed services to be
	 * provisioned, so they are disabled in the local profile and enabled by default in
	 * deployed environments.
	 */
	private boolean managedToolsEnabled = true;

	public Duration getResponseTimeout() {
		return this.responseTimeout;
	}

	public void setResponseTimeout(Duration responseTimeout) {
		this.responseTimeout = responseTimeout;
	}

	public String getSystemPrompt() {
		return this.systemPrompt;
	}

	public void setSystemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	public List<String> getCorsAllowedOrigins() {
		return this.corsAllowedOrigins;
	}

	public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
		this.corsAllowedOrigins = corsAllowedOrigins;
	}

	public boolean isManagedToolsEnabled() {
		return this.managedToolsEnabled;
	}

	public void setManagedToolsEnabled(boolean managedToolsEnabled) {
		this.managedToolsEnabled = managedToolsEnabled;
	}

}
