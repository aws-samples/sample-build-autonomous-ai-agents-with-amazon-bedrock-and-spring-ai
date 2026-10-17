# React UI for the IDP agent, served by unprivileged nginx (uid 101, port 8080).
# The nginx config is a template rendered at start-up, so the same image works
# in any environment:
#   docker run -e BACKEND_URL=http://my-backend:8080 -p 5174:8080 idp-agent-ui

# --- Build stage ---
FROM public.ecr.aws/docker/library/node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# "Return to home" link target, baked into the bundle at build time.
ARG VITE_LANDING_URL=http://localhost:3000/
ENV VITE_LANDING_URL=${VITE_LANDING_URL}
RUN npm run build

# --- Serve stage ---
FROM docker.io/nginxinc/nginx-unprivileged:1.30-alpine
ENV BACKEND_URL=http://idp-agent:8080
# Rendered to /etc/nginx/conf.d/default.conf (replacing the image default).
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
