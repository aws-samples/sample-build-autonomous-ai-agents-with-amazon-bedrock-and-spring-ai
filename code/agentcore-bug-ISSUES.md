# Code Review — Bug/Issue Report

Scope: application code under `code/autonomous-agent` and `code/idp-agent`.

Classification note: every issue below is in the **application/sample code**
(`com.amazonaws.samples.agentcore.*`), not in the Spring AI (`org.springframework.ai.*`)
or AgentCore SDK (`org.springaicommunity.agentcore.*`) libraries. AC-3 is an application
design choice interacting with a documented SDK constraint (see that issue).

Severity legend: **Critical** (data loss / outage), **Major** (wrong behavior or resilience defeated), **Minor** (hardening / edge case).

| ID | Module | Severity | Title |
|----|--------|----------|-------|
| AC-1 | autonomous-agent | Major | Retry retries non-transient errors (no exception predicate) |
| AC-2 | autonomous-agent | Major | Retry / circuit-breaker layered opposite to idp-agent |
| AC-3 | autonomous-agent | Major | Artifacts read from shared default session (multi-user leak) |
| IDP-1 | idp-agent | Major | Brittle JSON extraction in `LlmJson.extract` |
| IDP-2 | idp-agent | Major | Retry wraps JSON parse and has no exception predicate |
| IDP-3 | idp-agent | Major | Duplicate rule IDs silently collide |
| IDP-4 | idp-agent | Major | Batch upload aborts on first file failure |
| IDP-5 | idp-agent | Minor | Agent fallback reports wrong type / NPE risk |
| IDP-6 | idp-agent | Minor | SpEL uses `StandardEvaluationContext` (latent RCE) |
| IDP-7 | idp-agent | Minor | Duplicate audit event on re-resolve |
| IDP-8 | idp-agent | Minor | Non-atomic review-queue eviction |
| IDP-9 | idp-agent | Minor | Uploads unauthenticated / unbounded |

---

## AC-1 — Retry retries non-transient errors (no exception predicate)

- **Module:** autonomous-agent
- **Severity:** Major
- **File:** `src/main/java/com/amazonaws/samples/agentcore/autonomous/config/ResilienceConfig.java`
- **Method:** `bedrockRetry()`
- **Classification:** Programming issue (application config)

**Description**
The `RetryConfig` sets `maxAttempts(3)` with exponential backoff but declares no
`retryExceptions`/`retryOnException`/`ignoreExceptions` predicate. The Javadoc says the
retry is for "transient throttling (`ThrottlingException`) and brief network errors," but
because there is no predicate, **every** exception is retried 3x — including
non-transient ones such as `ValidationException` and guardrail rejections.

**Impact**
- Deterministic failures burn 3 attempts plus backoff delay before failing anyway.
- Each retried attempt is also recorded as a circuit-breaker failure, so model/config
  errors can trip the breaker as if Bedrock were unhealthy.

**Current code**
```java
RetryConfig config = RetryConfig.custom()
    .maxAttempts(3)
    .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(500), 2.0))
    .build();
```

**Suggested fix**
Restrict retries to transient failures:
```java
RetryConfig config = RetryConfig.custom()
    .maxAttempts(3)
    .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(500), 2.0))
    .retryExceptions(/* ThrottlingException + transient network types */)
    .ignoreExceptions(/* ValidationException + other permanent types */)
    .build();
```

**Acceptance criteria**
- Non-transient exceptions fail fast (1 attempt).
- Transient throttling/network errors still retry up to 3x.
- A unit test verifies attempt counts for both a transient and a permanent exception.

---

## AC-2 — Retry / circuit-breaker layered opposite to idp-agent

- **Module:** autonomous-agent
- **Severity:** Major
- **File:** `src/main/java/com/amazonaws/samples/agentcore/autonomous/service/AgentService.java`
- **Method:** `streamChat(...)`
- **Classification:** Programming issue (application wiring)

**Description**
The reactive chain applies the circuit breaker as the outermost operator and retry inside
it, which is the reverse of `idp-agent` (retry outermost). The two services behave
inconsistently, and because the whole retry sequence is recorded as a single breaker call,
transient failures that succeed on retry never register with the breaker.

**Current code**
```java
.transformDeferred(RetryOperator.of(this.retry))            // applied first -> inner
.transformDeferred(CircuitBreakerOperator.of(this.circuitBreaker)); // applied last -> outer
```

**Suggested fix**
Pick one ordering and use it in both modules. Document the intended semantics (fast-fail
when open vs. per-attempt breaker accounting) so the choice is explicit.

**Acceptance criteria**
- Both modules use the same retry/breaker ordering.
- The intended semantics are documented in a code comment or the resilience config.

---

## AC-3 — Artifacts read from shared default session (multi-user leak)

- **Module:** autonomous-agent
- **Severity:** Major
- **File:** `src/main/java/com/amazonaws/samples/agentcore/autonomous/service/AgentService.java`
- **Method:** `collectGeneratedArtifacts()` / `appendArtifacts(...)`
- **Classification:** Application design choice interacting with an SDK constraint (not an SDK bug)

**Description**
Both artifact stores are drained from `SessionConstants.DEFAULT_SESSION_ID` regardless of
the caller's session. The blocking `call()` API has no Reactor context, so the AgentCore
tools store artifacts under the SDK's default session key and the service reads from the
same key. Under concurrent users, one user's screenshots/CSVs can surface to another user
or be lost. The code comment acknowledges the "single-user default session."

**Impact**
- Cross-user artifact leakage in a multi-tenant deployment.
- Lost or misattributed artifacts under concurrency.

**Suggested fix**
Propagate the real session id to the artifact store on the blocking path (e.g. scope the
store retrieval by `conversationId`), or document that the sample is single-user only and
guard against concurrent use.

**Acceptance criteria**
- Artifacts are retrieved using the caller's session id, not the shared default.
- A concurrency test confirms artifacts are not shared across sessions.

---

## IDP-1 — Brittle JSON extraction in `LlmJson.extract`

- **Module:** idp-agent
- **Severity:** Major
- **File:** `src/main/java/com/amazonaws/samples/agentcore/idp/agent/LlmJson.java`
- **Method:** `extract(String text)`
- **Classification:** Programming issue

**Description**
The method takes the first `{`/`[` and the **last** matching close char across the entire
string with no brace balancing. Trailing prose containing `}`/`]`, a stray leading brace,
or sibling JSON blocks yield invalid JSON, so `BeanOutputConverter.convert()` throws — the
opposite of what the utility is meant to guarantee.

**Current code**
```java
char close = (t.charAt(start) == '{') ? '}' : ']';
int end = t.lastIndexOf(close);
return (end > start) ? t.substring(start, end + 1) : t.substring(start);
```

**Suggested fix**
Balance braces/brackets from `start` (tracking string literals and escapes) and return the
substring at the matching close, or strip known markdown code fences first and parse the
fenced block.

**Acceptance criteria**
- JSON followed by trailing prose is extracted correctly.
- Fenced ```` ```json ... ``` ```` blocks are handled.
- Unit tests cover trailing prose, code fences, and sibling blocks.

---

## IDP-2 — Retry wraps JSON parse and has no exception predicate

- **Module:** idp-agent
- **Severity:** Major
- **File:** `src/main/java/com/amazonaws/samples/agentcore/idp/agent/AbstractDocumentAgent.java`
- **Method:** `process(DocumentContext)`
- **Classification:** Programming issue

**Description**
The resilience wrapper places retry outside the circuit breaker, and the retried supplier
includes the JSON parse step. Combined with the missing retry predicate (see AC-1, same
config style in `idp/config/ResilienceConfig.java`):
1. An open breaker's `CallNotPermittedException` is retried 3x (retrying a fast-fail).
2. Deterministic JSON parse failures are retried 3x and recorded as breaker failures,
   conflating model-formatting errors with Bedrock health.

**Current code**
```java
Supplier<ProcessedDocument> resilient = Retry.decorateSupplier(this.retry,
    CircuitBreaker.decorateSupplier(this.circuitBreaker, call));
```

**Suggested fix**
- Add a retry predicate (as in AC-1) so parse/validation errors are not retried.
- Consider moving the JSON parse outside the retried call so only the model invocation is
  retried.

**Acceptance criteria**
- Parse failures are not retried and do not count as breaker failures.
- `CallNotPermittedException` is not retried.

---

## IDP-3 — Duplicate rule IDs silently collide

- **Module:** idp-agent
- **Severity:** Major
- **File:** `src/main/java/com/amazonaws/samples/agentcore/idp/rules/RulesEngine.java`
- **Method:** constructor
- **Classification:** Programming issue

**Description**
`compiled` is a map keyed by `rule.id()`. Duplicate IDs (never validated) overwrite each
other, so multiple rules with the same id all evaluate the last-compiled condition and the
earlier condition is silently lost. The startup "Loaded N rule(s)" count also under-reports.

**Current code**
```java
this.compiled.put(rule.id(), this.parser.parseExpression(rule.condition()));
```

**Suggested fix**
Validate uniqueness at startup (fail fast or log an error) and/or key compiled expressions
by list index rather than id.

**Acceptance criteria**
- Duplicate rule ids are detected at startup.
- Every declared rule is evaluated with its own condition.

---

## IDP-4 — Batch upload aborts on first file failure

- **Module:** idp-agent
- **Severity:** Major
- **File:** `src/main/java/com/amazonaws/samples/agentcore/idp/web/DocumentController.java`
- **Method:** `uploadBatch(...)`
- **Classification:** Programming issue

**Description**
The batch loop has no per-file try/catch, so one file's `IOException`/`RuntimeException`
aborts the entire batch — contradicting the Javadoc: "one failure does not sink the batch."

**Current code**
```java
for (MultipartFile file : files) {
    results.add(processOne(file, agentType, null, session));
}
```

**Suggested fix**
Wrap each file in try/catch and return a per-file success/error result so the batch
completes.

**Acceptance criteria**
- A batch with one failing file still returns results for the others.
- The failing entry carries an error indicator.

---

## IDP-5 — Agent fallback reports wrong type / NPE risk

- **Module:** idp-agent
- **Severity:** Minor
- **File:** `src/main/java/com/amazonaws/samples/agentcore/idp/service/DocumentOrchestrator.java`
- **Method:** `process(...)`
- **Classification:** Programming issue

**Description**
`this.agents.getOrDefault(type, this.agents.get(AgentType.GENERAL))` falls back to the
GENERAL agent, but the returned `DocumentResult` still reports the original `type.name()`
(mismatch between the agent that ran and the reported type). It also NPEs if no GENERAL
agent bean is registered (unguarded null default).

**Suggested fix**
Guard the GENERAL fallback for null, and report the agent type that actually handled the
document.

**Acceptance criteria**
- Result reports the agent type that actually ran.
- Missing GENERAL bean produces a clear error, not an NPE.

---

## IDP-6 — SpEL uses `StandardEvaluationContext` (latent RCE)

- **Module:** idp-agent
- **Severity:** Minor
- **File:** `src/main/java/com/amazonaws/samples/agentcore/idp/rules/RulesEngine.java`
- **Method:** `evaluate(...)`
- **Classification:** Programming issue (hardening)

**Description**
Rule conditions are evaluated with `StandardEvaluationContext`, which allows full SpEL
including `T(...)` type references and arbitrary method invocation. If rule configuration
is ever attacker-influenced, this is a remote code execution vector.

**Suggested fix**
Use `SimpleEvaluationContext` (property/read-only access) unless full SpEL is explicitly
required, and document that rule config is trusted.

**Acceptance criteria**
- Rule evaluation uses a restricted evaluation context, or the trust boundary is documented.

---

## IDP-7 — Duplicate audit event on re-resolve

- **Module:** idp-agent
- **Severity:** Minor
- **Files:** `src/main/java/com/amazonaws/samples/agentcore/idp/web/ReviewController.java`,
  `src/main/java/com/amazonaws/samples/agentcore/idp/review/ReviewQueue.java`
- **Method:** `resolve(...)`
- **Classification:** Programming issue

**Description**
Re-resolving an already-resolved review item returns the existing item with
`transitioned=false`, but the controller calls `auditService.reviewResolved()`
unconditionally. This emits a duplicate `review_resolved` audit event and returns 200 OK
with no state change. (The metric is guarded; the audit is not.)

**Suggested fix**
Only emit the audit event when the state actually transitioned.

**Acceptance criteria**
- Re-resolving an already-resolved item emits no additional audit event.

---

## IDP-8 — Non-atomic review-queue eviction

- **Module:** idp-agent
- **Severity:** Minor
- **File:** `src/main/java/com/amazonaws/samples/agentcore/idp/review/ReviewQueue.java`
- **Method:** `evictIfNeeded()` / `enqueue(...)`
- **Classification:** Programming issue

**Description**
Eviction is check-then-act and not atomic, so under concurrency the queue can exceed
`maxQueueSize`. Additionally, when all items are PENDING nothing is evictable, allowing
unbounded growth.

**Suggested fix**
Make eviction atomic (e.g. bounded structure or a lock around check+evict+insert) and
define behavior when nothing is evictable (reject or evict oldest PENDING).

**Acceptance criteria**
- Queue never exceeds `maxQueueSize` under concurrent enqueues.
- Behavior when all items are PENDING is defined and tested.

---

## IDP-9 — Uploads unauthenticated / unbounded

- **Module:** idp-agent
- **Severity:** Minor
- **File:** `src/main/java/com/amazonaws/samples/agentcore/idp/web/DocumentController.java`
- **Methods:** `upload(...)`, `uploadBatch(...)`, `processOne(...)`, `isTextLike(...)`
- **Classification:** Programming issue (hardening)

**Description**
- Upload endpoints have no authentication.
- No explicit file size or type limits; `file.getBytes()` loads the whole file into memory.
- `isTextLike()` treats blank/unknown content types as UTF-8 text, mangling binary uploads.

**Suggested fix**
Add authentication/authorization, enforce max file size and allowed content types, and
default unknown content types to binary handling.

**Acceptance criteria**
- Endpoints require auth (or the demo scope is documented).
- Oversized/unsupported uploads are rejected with a clear error.
- Binary uploads with unknown content types are not corrupted.
