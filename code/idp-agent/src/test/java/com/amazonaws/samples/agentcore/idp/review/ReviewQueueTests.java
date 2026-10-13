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
package com.amazonaws.samples.agentcore.idp.review;

import java.util.List;
import java.util.Optional;

import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import com.amazonaws.samples.agentcore.idp.metrics.IdpMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewQueueTests {

	private ReviewQueue queue;

	@BeforeEach
	void setUp() {
		this.queue = new ReviewQueue(new ReviewProperties(true, 0.7, 500), new IdpMetrics(new SimpleMeterRegistry()));
	}

	private ProcessedDocument doc() {
		return new ProcessedDocument("LEASE", "summary", "answer", List.of(), 0.5, List.of());
	}

	@Test
	void enqueueAddsPendingItem() {
		ReviewItem item = this.queue.enqueue("LAWYER", "conv-1", doc(), List.of(), List.of("Low confidence"));

		assertThat(item.status()).isEqualTo(ReviewStatus.PENDING);
		assertThat(this.queue.pendingCount()).isEqualTo(1);
		assertThat(this.queue.pending()).hasSize(1);
		assertThat(this.queue.find(item.id())).isPresent();
	}

	@Test
	void resolveApprovesAndClearsFromPending() {
		ReviewItem item = this.queue.enqueue("MORTGAGE", "conv-2", doc(), List.of(), List.of("Blocked"));

		Optional<ReviewItem> resolved = this.queue.resolve(item.id(), ReviewStatus.APPROVED, "alice", "looks good");

		assertThat(resolved).isPresent();
		assertThat(resolved.get().status()).isEqualTo(ReviewStatus.APPROVED);
		assertThat(resolved.get().reviewer()).isEqualTo("alice");
		assertThat(this.queue.pendingCount()).isZero();
		assertThat(this.queue.pending()).isEmpty();
		assertThat(this.queue.all()).hasSize(1);
	}

	@Test
	void resolveUnknownIdReturnsEmpty() {
		assertThat(this.queue.resolve("does-not-exist", ReviewStatus.APPROVED, "bob", null)).isEmpty();
	}

	@Test
	void resolveTwiceDoesNotDoubleCount() {
		ReviewItem item = this.queue.enqueue("TAX", "conv-3", doc(), List.of(), List.of("Low confidence"));

		this.queue.resolve(item.id(), ReviewStatus.APPROVED, "alice", null);
		Optional<ReviewItem> second = this.queue.resolve(item.id(), ReviewStatus.REJECTED, "bob", null);

		// Second resolve is a no-op: the item stays APPROVED and the gauge stays at zero.
		assertThat(second).isPresent();
		assertThat(second.get().status()).isEqualTo(ReviewStatus.APPROVED);
		assertThat(this.queue.pendingCount()).isZero();
	}

	@Test
	void invalidDecisionIsRejected() {
		ReviewItem item = this.queue.enqueue("GENERAL", "conv-4", doc(), List.of(), List.of("Low confidence"));

		try {
			this.queue.resolve(item.id(), ReviewStatus.PENDING, "alice", null);
			assertThat(false).as("expected IllegalArgumentException").isTrue();
		}
		catch (IllegalArgumentException expected) {
			assertThat(expected).hasMessageContaining("APPROVED or REJECTED");
		}
	}

}
