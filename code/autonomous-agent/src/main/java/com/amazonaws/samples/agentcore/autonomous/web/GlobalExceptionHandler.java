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
package com.amazonaws.samples.agentcore.autonomous.web;

import com.amazonaws.samples.agentcore.autonomous.dto.ErrorResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates exceptions into a consistent {@link ErrorResponse} envelope with the right
 * HTTP status, so clients (and the UI) get predictable error handling.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String detail = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.findFirst()
			.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
			.orElse("Invalid request");
		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Bad Request", detail));
	}

	@ExceptionHandler(CallNotPermittedException.class)
	public ResponseEntity<ErrorResponse> handleCircuitOpen(CallNotPermittedException ex) {
		logger.warn("Circuit breaker open, rejecting call: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable",
					"The assistant is temporarily overloaded. Please retry shortly."));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), "Not Found",
					"No resource at " + ex.getResourcePath() + ". This service exposes /api/** and /actuator/**."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		logger.error("Unhandled error", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
					"An unexpected error occurred."));
	}

}
