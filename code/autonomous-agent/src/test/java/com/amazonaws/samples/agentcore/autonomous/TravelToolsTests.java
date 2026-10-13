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

import com.amazonaws.samples.agentcore.autonomous.tools.TravelTools;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline unit tests for the deterministic travel tools.
 */
class TravelToolsTests {

	private final TravelTools tools = new TravelTools();

	@Test
	void weatherIsCaseInsensitive() {
		assertThat(this.tools.getWeather("Seattle")).isEqualTo(this.tools.getWeather("  seattle "));
	}

	@Test
	void unknownCityReturnsGracefulMessage() {
		assertThat(this.tools.getWeather("Atlantis")).contains("No forecast available");
		assertThat(this.tools.getPointsOfInterest("Atlantis")).contains("No curated highlights");
	}

	@Test
	void flightPriceIsDeterministic() {
		String first = this.tools.estimateFlightPrice("Seattle", "Tokyo");
		String second = this.tools.estimateFlightPrice("Seattle", "Tokyo");
		assertThat(first).isEqualTo(second).contains("Estimated round-trip fare");
	}

	@Test
	void flightPriceRequiresBothCities() {
		assertThat(this.tools.estimateFlightPrice("Seattle", "")).contains("required");
	}

}
