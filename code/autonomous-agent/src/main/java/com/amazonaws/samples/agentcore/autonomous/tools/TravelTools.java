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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Domain tools for a travel-planning agent. In a real system these methods would call
 * downstream APIs (a weather service, a booking system, an internal catalog). Here they
 * return deterministic sample data so the example runs without external dependencies,
 * while still demonstrating how the model plans across multiple tool calls.
 */
public class TravelTools {

	private static final Logger logger = LoggerFactory.getLogger(TravelTools.class);

	private static final Map<String, String> FORECASTS = Map.of("seattle", "12°C, light rain", "tokyo",
			"18°C, clear skies", "london", "9°C, overcast", "new york", "15°C, partly cloudy");

	private static final Map<String, String> POINTS_OF_INTEREST = Map.of("seattle",
			"Pike Place Market, Space Needle, Chihuly Garden", "tokyo",
			"Senso-ji Temple, Shibuya Crossing, teamLab Planets", "london",
			"British Museum, Tower of London, Borough Market", "new york",
			"Central Park, the Met, the High Line");

	@Tool(description = "Get a short weather forecast for a city")
	public String getWeather(@ToolParam(description = "City name, e.g. 'Seattle'") String city) {
		logger.info("Tool call: getWeather(city={})", city);
		String key = normalize(city);
		return FORECASTS.getOrDefault(key, "No forecast available for " + city);
	}

	@Tool(description = "Estimate a flight price in USD between two cities")
	public String estimateFlightPrice(@ToolParam(description = "Origin city") String from,
			@ToolParam(description = "Destination city") String to) {
		logger.info("Tool call: estimateFlightPrice(from={}, to={})", from, to);
		if (from == null || from.isBlank() || to == null || to.isBlank()) {
			return "Both an origin and destination city are required to estimate a fare.";
		}
		// Deterministic pseudo-pricing so the demo is reproducible.
		int base = 250;
		int variable = Math.abs((from + to).hashCode() % 600);
		return "Estimated round-trip fare from " + from + " to " + to + ": $" + (base + variable);
	}

	@Tool(description = "List notable points of interest to visit in a city")
	public String getPointsOfInterest(@ToolParam(description = "City name, e.g. 'Tokyo'") String city) {
		logger.info("Tool call: getPointsOfInterest(city={})", city);
		String key = normalize(city);
		return POINTS_OF_INTEREST.getOrDefault(key, "No curated highlights available for " + city);
	}

	private static String normalize(String city) {
		return city == null ? "" : city.trim().toLowerCase();
	}

}
