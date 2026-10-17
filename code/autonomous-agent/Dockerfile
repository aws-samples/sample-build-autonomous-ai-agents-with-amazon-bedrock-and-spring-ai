# Architecture is selected by the buildx --platform flag at build time:
#   AgentCore Runtime  -> linux/arm64 (required; scripts/build-and-push.sh sets it)
#   Local docker compose -> your machine's native architecture
#
# Base image is glibc-based Amazon Linux 2023, not Alpine: the AgentCore Browser
# tool uses Playwright Java, which unpacks and runs a glibc-linked Node driver at
# start-up. On Alpine (musl) that binary cannot execute and the app fails to start.
FROM public.ecr.aws/docker/library/amazoncorretto:21-al2023-headless
WORKDIR /app
COPY --chown=1000:1000 target/autonomous-agent.jar app.jar
# Browser mode "agentcore" drives the remote AgentCore Browser over CDP, so no
# local browser binaries are needed.
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
USER 1000:1000
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/ping || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
