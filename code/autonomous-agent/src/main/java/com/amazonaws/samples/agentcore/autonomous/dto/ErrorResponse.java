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

import java.time.Instant;

/**
 * Consistent error envelope returned by {@code GlobalExceptionHandler}.
 *
 * @param status HTTP status code
 * @param error short error reason
 * @param message human-readable detail (safe to surface to clients)
 * @param timestamp when the error occurred
 */
public record ErrorResponse(int status, String error, String message, Instant timestamp) {

	public static ErrorResponse of(int status, String error, String message) {
		return new ErrorResponse(status, error, message, Instant.now());
	}

}
