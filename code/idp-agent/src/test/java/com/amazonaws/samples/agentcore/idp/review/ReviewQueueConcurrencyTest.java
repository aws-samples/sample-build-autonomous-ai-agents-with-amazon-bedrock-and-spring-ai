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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.samples.agentcore.idp.dto.ProcessedDocument;
import com.amazonaws.samples.agentcore.idp.metrics.IdpMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency / load test for {@link ReviewQueue}. Confirms that the in-memory queue stays
 * consistent under heavy parallel enqueue and resolve traffic, and that resolving the same
 * id twice never drives the pending gauge negative. Runs entirely offline (no AWS).
 */
class ReviewQueueConcurrencyTest {

	private static final int THREADS = 16;

	private static final int ITEMS = 1000;

	private ReviewQueue queue;

	@BeforeEach
	void setUp() {
		this.queue = new ReviewQueue(new ReviewProperties(true, 0.7, 10000), new IdpMetrics(new SimpleMeterRegistry()));
	}

	private ProcessedDocument doc() {
		return new ProcessedDocument("LEASE", "summary", "answer", List.of(), 0.5, List.of());
	}

	@Test
	void concurrentEnqueueAndResolveStaysConsistent() throws InterruptedException {
		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		try {
			// Phase 1: concurrently enqueue ITEMS items and collect their ids.
			List<String> ids = new CopyOnWriteArrayList<>();
			CountDownLatch enqueueStart = new CountDownLatch(1);
			CountDownLatch enqueueDone = new CountDownLatch(ITEMS);
			for (int i = 0; i < ITEMS; i++) {
				final int index = i;
				pool.submit(() -> {
					try {
						enqueueStart.await();
						ReviewItem item = this.queue.enqueue("LAWYER", "conv-" + index, doc(), List.of(),
								List.of("Low confidence"));
						ids.add(item.id());
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
					finally {
						enqueueDone.countDown();
					}
				});
			}
			enqueueStart.countDown();
			assertThat(enqueueDone.await(30, TimeUnit.SECONDS)).as("enqueue phase completed").isTrue();

			assertThat(this.queue.pendingCount()).isEqualTo(ITEMS);
			assertThat(this.queue.pending()).hasSize(ITEMS);
			assertThat(ids).hasSize(ITEMS);

			// Phase 2: concurrently resolve every item (APPROVE), resolving each id twice
			// from different threads to exercise the idempotent no-op path.
			CountDownLatch resolveStart = new CountDownLatch(1);
			CountDownLatch resolveDone = new CountDownLatch(ITEMS * 2);
			for (String id : ids) {
				for (int r = 0; r < 2; r++) {
					final String targetId = id;
					pool.submit(() -> {
						try {
							resolveStart.await();
							this.queue.resolve(targetId, ReviewStatus.APPROVED, "reviewer", "ok");
						}
						catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
						finally {
							resolveDone.countDown();
						}
					});
				}
			}
			resolveStart.countDown();
			assertThat(resolveDone.await(30, TimeUnit.SECONDS)).as("resolve phase completed").isTrue();

			// The queue must be fully drained and never negative.
			assertThat(this.queue.pendingCount()).isZero();
			assertThat(this.queue.pending()).isEmpty();
			assertThat(this.queue.all()).hasSize(ITEMS);

			// Every item ended APPROVED exactly.
			AtomicInteger approved = new AtomicInteger();
			for (ReviewItem item : this.queue.all()) {
				assertThat(item.status()).isEqualTo(ReviewStatus.APPROVED);
				approved.incrementAndGet();
			}
			assertThat(approved.get()).isEqualTo(ITEMS);
		}
		finally {
			pool.shutdown();
			assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).as("pool terminated").isTrue();
		}
	}

}
