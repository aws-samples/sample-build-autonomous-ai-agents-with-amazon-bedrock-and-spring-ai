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
package com.amazonaws.samples.agentcore.idp.alert;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the default {@link AlertService} only when the application has not supplied
 * its own. To send real alerts, define an {@code AlertService} bean (Amazon SNS, Slack,
 * email) in your own configuration and it replaces this fallback.
 */
@Configuration
public class AlertConfig {

	@Bean
	@ConditionalOnMissingBean(AlertService.class)
	public AlertService loggingAlertService(MeterRegistry registry) {
		return new LoggingAlertService(registry);
	}

}
