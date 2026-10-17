# Architecture is selected by the buildx --platform flag at build time:
#   AgentCore Runtime  -> linux/arm64 (required)
#   EKS / ECS          -> match your node architecture (linux/amd64 or linux/arm64)
#
# Same glibc-based Amazon Linux 2023 base as the autonomous agent, so both
# images behave the same way (see that Dockerfile for why Alpine is avoided).
FROM public.ecr.aws/docker/library/amazoncorretto:21-al2023-headless
WORKDIR /app
COPY --chown=1000:1000 target/idp-agent.jar app.jar
USER 1000:1000
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/ping || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
