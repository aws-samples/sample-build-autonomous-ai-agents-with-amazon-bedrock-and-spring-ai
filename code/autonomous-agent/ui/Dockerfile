# --- Build stage ---
FROM public.ecr.aws/docker/library/node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci || npm install
COPY . .
RUN npm run build

# --- Serve stage (runs as the non-root nginx user on port 8080) ---
FROM public.ecr.aws/docker/library/nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
RUN touch /run/nginx.pid \
  && chown -R nginx:nginx /run/nginx.pid /var/cache/nginx /usr/share/nginx/html
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ || exit 1
CMD ["nginx", "-g", "daemon off;"]
