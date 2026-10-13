# AgentCore Runtime requires linux/arm64 container images.
FROM --platform=linux/arm64 public.ecr.aws/docker/library/amazoncorretto:21-alpine

RUN addgroup -S appuser && adduser -S appuser -G appuser
WORKDIR /app
COPY target/autonomous-agent.jar app.jar
RUN chown appuser:appuser /app/app.jar
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ping || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
