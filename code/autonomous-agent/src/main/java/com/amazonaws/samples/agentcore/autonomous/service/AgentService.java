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
package com.amazonaws.samples.agentcore.autonomous.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.amazonaws.samples.agentcore.autonomous.config.AgentProperties;
import com.amazonaws.samples.agentcore.autonomous.metrics.AgentMetrics;
import com.amazonaws.samples.agentcore.autonomous.tools.DateTimeTools;
import com.amazonaws.samples.agentcore.autonomous.tools.TravelTools;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agentcore.artifacts.ArtifactStore;
import org.springaicommunity.agentcore.artifacts.GeneratedFile;
import org.springaicommunity.agentcore.artifacts.SessionConstants;
import org.springaicommunity.agentcore.evaluations.AgentCoreEvaluationAdvisor;
import org.springaicommunity.agentcore.memory.longterm.AgentCoreMemory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Core agent logic, separated from the web layer so it can be unit-tested and reused.
 *
 * <p>
 * The service combines the three agent capabilities, <b>planning</b> (the model decides
 * which tools to call), <b>tool use</b> ({@link DateTimeTools}, {@link TravelTools}), and
 * <b>memory</b> (AgentCore short-term history plus long-term strategies scoped per
 * session). The model call is wrapped in a Resilience4j retry and circuit breaker and
 * bounded by a timeout, and every turn is instrumented for observability.
 */
@Service
public class AgentService {

	private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

	private static final String FALLBACK_MESSAGE = "The assistant is temporarily unavailable. Please try again in a moment.";

	private final ChatClient chatClient;

	private final AgentCoreMemory agentCoreMemory;

	private final CircuitBreaker circuitBreaker;

	private final Retry retry;

	private final AgentMetrics metrics;

	private final Duration responseTimeout;

	private final ArtifactStore<GeneratedFile> browserArtifactStore;

	private final ArtifactStore<GeneratedFile> codeInterpreterArtifactStore;

	private final GuardrailService guardrailService;

	public AgentService(ChatClient.Builder chatClientBuilder, ObjectProvider<AgentCoreMemory> agentCoreMemory,
			CircuitBreaker bedrockCircuitBreaker, Retry bedrockRetry, AgentMetrics metrics, AgentProperties properties,
			@Qualifier("browserToolCallbackProvider") ObjectProvider<ToolCallbackProvider> browserTools,
			@Qualifier("codeInterpreterToolCallbackProvider") ObjectProvider<ToolCallbackProvider> codeInterpreterTools,
			@Qualifier("browserArtifactStore") ObjectProvider<ArtifactStore<GeneratedFile>> browserArtifactStore,
			@Qualifier("codeInterpreterArtifactStore") ObjectProvider<ArtifactStore<GeneratedFile>> codeInterpreterArtifactStore,
			ObjectProvider<AgentCoreEvaluationAdvisor> evaluationAdvisor, GuardrailService guardrailService) {
		this.agentCoreMemory = agentCoreMemory.getIfAvailable();
		this.circuitBreaker = bedrockCircuitBreaker;
		this.retry = bedrockRetry;
		this.metrics = metrics;
		this.responseTimeout = properties.getResponseTimeout();
		this.guardrailService = guardrailService;
		this.browserArtifactStore = browserArtifactStore.getIfAvailable();
		this.codeInterpreterArtifactStore = codeInterpreterArtifactStore.getIfAvailable();

		ChatClient.Builder builder = chatClientBuilder.defaultSystem(properties.getSystemPrompt())
			.defaultTools(new DateTimeTools(), new TravelTools());

		// Managed AgentCore tools (browser + code interpreter) are wired in when their
		// starters are on the classpath and the feature is enabled. They require the AWS
		// AgentCore Browser and Code Interpreter managed services, so they are turned off
		// in the local profile (agent.managed-tools-enabled=false).
		List<ToolCallbackProvider> managedTools = new ArrayList<>();
		if (properties.isManagedToolsEnabled()) {
			browserTools.ifAvailable(managedTools::add);
			codeInterpreterTools.ifAvailable(managedTools::add);
		}
		if (!managedTools.isEmpty()) {
			builder = builder.defaultToolCallbacks(managedTools.toArray(new ToolCallbackProvider[0]));
			logger.info("Registered {} managed AgentCore tool provider(s): browser={}, codeInterpreter={}",
					managedTools.size(), this.browserArtifactStore != null, this.codeInterpreterArtifactStore != null);
		}

		// Online response evaluation (AgentCore Evaluate API) is opt-in; the advisor
		// bean only exists when agentcore.evaluation.enabled=true.
		AgentCoreEvaluationAdvisor advisor = evaluationAdvisor.getIfAvailable();
		if (advisor != null) {
			builder = builder.defaultAdvisors(advisor);
			logger.info("AgentCore Evaluations advisor enabled");
		}

		this.chatClient = builder.build();
		if (this.agentCoreMemory == null) {
			logger.warn("AgentCore Memory is not configured; running stateless (no conversation memory). "
					+ "Set a valid AGENTCORE_MEMORY_ID to enable it.");
		}
	}

	/**
	 * Stream the assistant's reply to the caller. The model call (including tool
	 * execution) runs to completion on a worker thread using the blocking {@code call()}
	 * API, which reliably assembles multi-argument tool calls, and the finished reply is
	 * then emitted progressively so the UI renders it as it arrives.
	 * @param prompt the user instruction
	 * @param sessionId session id used to scope conversation memory (a per-call id is
	 * generated when blank)
	 * @return a stream of response chunks; on failure a single fallback message is emitted
	 * rather than an error so callers always receive a usable reply
	 */
	public Flux<String> streamChat(String prompt, String sessionId) {
		return streamChat(prompt, sessionId, true, true);
	}

	public Flux<String> streamChat(String prompt, String sessionId, boolean guardrails, boolean memory) {
		if (guardrails) {
			var blocked = this.guardrailService.screen(prompt);
			if (blocked.isPresent()) {
				logger.info("Guardrail blocked a prompt");
				this.metrics.recordRequest();
				return Flux.just("[Guardrail] " + blocked.get());
			}
		}
		String conversationId = "user:" + normalizeSession(sessionId);
		logger.info("Chat (streamed) for conversationId={} guardrails={} memory={}", conversationId, guardrails, memory);

		Timer.Sample sample = this.metrics.startTimer();
		this.metrics.recordRequest();

		return Mono.fromCallable(() -> invokeModel(prompt, conversationId, memory))
			.subscribeOn(Schedulers.boundedElastic())
			.timeout(this.responseTimeout)
			.transformDeferred(RetryOperator.of(this.retry))
			.transformDeferred(CircuitBreakerOperator.of(this.circuitBreaker))
			.flatMapMany(AgentService::toChunks)
			.doOnComplete(() -> this.metrics.recordSuccess(sample))
			.onErrorResume(ex -> {
				logger.error("Model call failed for conversationId={}, returning fallback", conversationId, ex);
				this.metrics.recordFailure(sample);
				this.metrics.recordFallback();
				return Flux.just(FALLBACK_MESSAGE);
			});
	}

	/**
	 * Synchronous entry point that returns the complete assistant reply. Annotated with
	 * {@link Observed} so it produces a metric today and a trace span once a tracing bridge
	 * is added.
	 * @param prompt the user instruction
	 * @param sessionId session id used to scope conversation memory
	 * @return the complete assistant reply
	 */
	@Observed(name = "agent.chat.sync", contextualName = "agent-chat-sync")
	public String chat(String prompt, String sessionId) {
		return chat(prompt, sessionId, true, true);
	}

	@Observed(name = "agent.chat.sync", contextualName = "agent-chat-sync")
	public String chat(String prompt, String sessionId, boolean guardrails, boolean memory) {
		if (guardrails) {
			var blocked = this.guardrailService.screen(prompt);
			if (blocked.isPresent()) {
				logger.info("Guardrail blocked a prompt");
				return "[Guardrail] " + blocked.get();
			}
		}
		String conversationId = "user:" + normalizeSession(sessionId);
		logger.info("Chat (sync) for conversationId={} guardrails={} memory={}", conversationId, guardrails, memory);

		Timer.Sample sample = this.metrics.startTimer();
		this.metrics.recordRequest();
		try {
			String content = invokeModel(prompt, conversationId, memory);
			this.metrics.recordSuccess(sample);
			return content;
		}
		catch (Exception ex) {
			logger.error("Model call failed for conversationId={}, returning fallback", conversationId, ex);
			this.metrics.recordFailure(sample);
			this.metrics.recordFallback();
			return FALLBACK_MESSAGE;
		}
	}

	/**
	 * Single blocking model call with tool execution and (optional) conversation memory.
	 * Using {@code call()} rather than {@code stream()} ensures multi-argument tool calls
	 * are fully assembled before the tool runs.
	 */
	private String invokeModel(String prompt, String conversationId, boolean memory) {
		ChatClient.ChatClientRequestSpec request = this.chatClient.prompt().user(prompt);
		if (memory && this.agentCoreMemory != null) {
			request = request.advisors(this.agentCoreMemory.advisors)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
		}
		String content = request.call().content();
		return (content == null ? "" : content) + collectGeneratedArtifacts();
	}

	/**
	 * Drain any artifacts the browser and code interpreter tools produced during this turn
	 * (screenshots, charts, CSVs) and render them as markdown so the UI can display or
	 * download them inline. The blocking {@code call()} path has no Reactor context, so the
	 * tools store artifacts under the single-user default session; we read from the same
	 * key. Retrieval is a destructive read, so each artifact is surfaced exactly once.
	 * @return markdown for the generated artifacts, or an empty string when none were produced
	 */
	private String collectGeneratedArtifacts() {
		StringBuilder sb = new StringBuilder();
		appendArtifacts(sb, this.browserArtifactStore);
		appendArtifacts(sb, this.codeInterpreterArtifactStore);
		return sb.toString();
	}

	private static void appendArtifacts(StringBuilder sb, ArtifactStore<GeneratedFile> store) {
		if (store == null) {
			return;
		}
		List<GeneratedFile> files = store.retrieve(SessionConstants.DEFAULT_SESSION_ID);
		if (files == null || files.isEmpty()) {
			return;
		}
		for (GeneratedFile file : files) {
			if (file.isImage()) {
				sb.append("\n\n![").append(file.name()).append("](").append(file.toDataUrl()).append(")");
			}
			else {
				sb.append("\n\n[Download ").append(file.name()).append("](").append(file.toDataUrl()).append(")");
			}
		}
	}

	/**
	 * Split a finished reply into word-sized chunks so the UI can render it progressively.
	 * The trailing whitespace is preserved on each chunk so the text reassembles exactly.
	 */
	private static Flux<String> toChunks(String text) {
		if (text == null || text.isEmpty()) {
			return Flux.empty();
		}
		return Flux.fromArray(text.split("(?<=\\s)"));
	}

	private static String normalizeSession(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return "anon-" + UUID.randomUUID();
		}
		return sessionId;
	}

}
