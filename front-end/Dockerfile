# ---- Build stage ----
FROM node:22-alpine AS builder
WORKDIR /workspace

# Install deps first so this layer is reused as long as lockfiles don't change.
COPY package.json package-lock.json ./
RUN npm ci

# Build-time env (baked into the client bundle by Vite/nitro — see .env.example).
ARG VITE_API_BASE_URL
ARG VITE_SITE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_SITE_URL=$VITE_SITE_URL

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
EXPOSE 8080

CMD ["node", ".output/server/index.mjs"]
