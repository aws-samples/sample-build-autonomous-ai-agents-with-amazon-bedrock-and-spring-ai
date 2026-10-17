# Run the samples locally

Five pieces, each in its own folder and each its own deployable:

| Folder | What | Local URL |
|---|---|---|
| `landing-page/` | Static page linking both agents, with live status badges | http://localhost:3000 |
| `autonomous-agent-ui/` | React UI for the travel agent | http://localhost:5173 |
| `idp-agent-ui/` | React UI for the IDP console | http://localhost:5174 |
| `autonomous-agent/` | Spring Boot travel agent backend | http://localhost:8080 |
| `idp-agent/` | Spring Boot IDP backend | http://localhost:8081 |

Each UI calls its own backend through a same-origin `/api` proxy (the Vite dev server
locally, nginx in the container), so no CORS setup is needed.

## Prerequisites

- Java 21, Maven 3.9+, Node.js 20+
- AWS credentials with Amazon Bedrock access to Claude Sonnet 4.5 (the default
  `global.anthropic.claude-sonnet-4-5-20250929-v1:0` inference profile). Check with
  `aws sts get-caller-identity`.
- The `local` Spring profile turns off AgentCore Memory, Browser and Code Interpreter, so
  no AgentCore resources are needed.

## Option 1: Docker Compose

```bash
# from the repository root
mvn -q clean package -DskipTests
cd code
docker compose up --build
```

Open http://localhost:3000. Your `~/.aws` folder is mounted read-only into both backends;
set `AWS_PROFILE` (or the `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` /
`AWS_SESSION_TOKEN` variables) and `AWS_REGION` in your shell before `docker compose up`
to choose credentials. Stop with `docker compose down`.

## Option 2: Maven and npm (no Docker)

Build once from the repository root:

```bash
mvn -q clean install -DskipTests
```

Then start each piece in its own terminal.

**Backends**

```bash
# Terminal 1: travel agent backend on :8080
cd code/autonomous-agent
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2: IDP backend on :8081
cd code/idp-agent
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--server.port=8081
```

In PowerShell, quote the `-D` arguments, for example
`mvn spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--server.port=8081"`.

**UIs and landing page**

```bash
# Terminal 3: travel agent UI on :5173 (proxies /api to :8080)
cd code/autonomous-agent-ui && npm ci && npm run dev

# Terminal 4: IDP UI on :5174 (proxies /api to :8081)
cd code/idp-agent-ui && npm ci && npm run dev

# Terminal 5: landing page on :3000
cd code/landing-page && npm ci && npm run dev
```

## Validate

| Check | Expected |
|---|---|
| http://localhost:3000 | Both cards show **UI: running** and **Backend: running** |
| `curl http://localhost:8080/ping` | `{"status":"Healthy",...}` |
| `curl http://localhost:8081/ping` | `{"status":"Healthy",...}` |
| `curl http://localhost:8080/api/agent/info` | JSON with the model and tool list |
| `curl http://localhost:8081/api/agents` | JSON list of the five specialist agents |
| http://localhost:5173 | Ask "Plan a 3-day trip to Tokyo"; the answer streams in and uses tools |
| http://localhost:5174 | Sign in (demo auth), use **Try a sample**, and check the extraction and rule alerts |

The AgentCore Runtime contract endpoint works locally too:

```bash
curl -N -X POST http://localhost:8080/invocations \
  -H "Content-Type: application/json" \
  -H "X-Amzn-Bedrock-AgentCore-Runtime-Session-Id: local-session-00000000000000000001" \
  -d '{"prompt":"What is the weather in Seattle?"}'
```

If a chat returns the fallback message, the backend could not reach Amazon Bedrock: check
your credentials, Region, and model access in that backend's log.

## Deploying

The backends deploy to Amazon Bedrock AgentCore Runtime with Terraform; see the
[repository README](../README.md#deploy-to-aws-with-terraform). AgentCore Runtime only
forwards `/invocations` and `/ping`, so the UIs and landing page are for local use or for
hosting next to a backend you run yourself (for example the IDP agent on Amazon EKS). Each
image is configured at start-up: `BACKEND_URL` for the UIs, and `AUTONOMOUS_UI_URL`,
`AUTONOMOUS_API_URL`, `IDP_UI_URL`, `IDP_API_URL` for the landing page.
