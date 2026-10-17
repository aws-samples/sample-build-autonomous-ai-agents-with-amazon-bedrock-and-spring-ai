# Landing page that links both agent UIs, served by unprivileged nginx (uid 101,
# port 8080). Endpoint URLs come from environment variables at start-up (see
# docker-entrypoint.d/40-landing-config.sh):
#   docker run -p 3000:8080 \
#     -e AUTONOMOUS_UI_URL=https://travel.example.com -e IDP_UI_URL=https://idp.example.com \
#     agentcore-samples-landing-page

# --- Build stage ---
FROM public.ecr.aws/docker/library/node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# --- Serve stage ---
FROM docker.io/nginxinc/nginx-unprivileged:1.30-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
# config.js is rewritten at start-up, so the html folder is owned by nginx (101).
COPY --chown=101:101 --from=build /app/dist /usr/share/nginx/html
COPY --chmod=0755 docker-entrypoint.d/40-landing-config.sh /docker-entrypoint.d/40-landing-config.sh
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
