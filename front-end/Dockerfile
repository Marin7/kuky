# ---- Build stage ----
FROM node:22-alpine AS builder
WORKDIR /workspace

# Install deps first so this layer is reused as long as lockfiles don't change.
COPY package.json package-lock.json ./
RUN npm ci

# Build-time env (baked into the client bundle by Vite/nitro — see repo-root .env.example).
ARG VITE_API_BASE_URL
ARG VITE_SITE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_SITE_URL=$VITE_SITE_URL

# Vite SSR builds peak well above Node's default ~0.5–1 GB heap. Cap the V8
# heap just under a typical 2 GB VM so the build can finish without the host
# OOM-killer, and keep libuv concurrency low to shrink the peak further.
# On a 2 GB host, stop other compose services first (`docker compose stop`)
# so this stage is not competing with postgres/back-end mem_limits.
ENV NODE_OPTIONS="--max-old-space-size=1536"
ENV UV_THREADPOOL_SIZE=2

COPY . .
RUN npm run build

# ---- Runtime stage ----
# nitro's node-server preset bundles its own dependencies, so the runtime
# image only needs the Node runtime plus the built .output directory.
FROM node:22-alpine
RUN addgroup -S kuky && adduser -S kuky -G kuky
WORKDIR /app

COPY --from=builder /workspace/.output ./.output

USER kuky
ENV HOST=0.0.0.0
ENV PORT=8080
# Sized for the ~256m container memory limit set in docker-compose.yml.
# Override by setting NODE_OPTIONS at `docker run`/compose.
ENV NODE_OPTIONS="--max-old-space-size=192"
EXPOSE 8080

CMD ["node", ".output/server/index.mjs"]
