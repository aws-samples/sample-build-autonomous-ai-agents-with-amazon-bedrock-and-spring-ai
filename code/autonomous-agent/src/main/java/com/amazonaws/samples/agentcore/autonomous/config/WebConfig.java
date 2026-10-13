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

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for the browser-based React UI. Origins are externalized via
 * {@code agent.cors-allowed-origins} and should be restricted to real UI origins in
 * production.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final AgentProperties properties;

	public WebConfig(AgentProperties properties) {
		this.properties = properties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			.allowedOrigins(this.properties.getCorsAllowedOrigins().toArray(String[]::new))
			.allowedMethods("GET", "POST")
			.allowedHeaders("*")
			.maxAge(3600);
	}

}
