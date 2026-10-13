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
package com.amazonaws.samples.agentcore.autonomous.tools;

import java.time.LocalDateTime;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * A tool the model can call to ground its answers in the current time. Tool calling is
 * one of the three core agent capabilities: the model decides when to invoke this method,
 * Spring AI executes it, and the result is fed back into the conversation.
 */
public class DateTimeTools {

	@Tool(description = "Get the current date and time in the user's timezone")
	public String getCurrentDateTime() {
		return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
	}

}
