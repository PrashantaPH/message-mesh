# syntax=docker/dockerfile:1

# ---- Build stage: install deps and produce the static bundle ----
FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies from the lockfile for reproducible builds.
COPY package.json package-lock.json ./
RUN npm ci

# Build the production bundle into /app/dist.
COPY . .
RUN npm run build

# ---- Runtime stage: serve static files with nginx ----
FROM nginx:1.27-alpine AS runtime

# nginx expands ${BACKEND_URL} into the config at container start (envsubst).
ENV BACKEND_URL=http://backend:8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD ["sh", "-c", "wget --quiet --tries=1 --spider http://localhost/ || exit 1"]
