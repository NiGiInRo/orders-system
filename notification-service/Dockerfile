# ── Stage 1: deps ────────────────────────────────────────────────────────────
FROM node:20-slim AS deps
WORKDIR /app

# package*.json primero — Docker cachea esta capa
# si el código cambia pero las dependencias no, no reinstala nada
COPY package*.json ./
RUN npm ci --omit=dev


# ── Stage 2: runtime ─────────────────────────────────────────────────────────
FROM node:20-slim AS runtime
WORKDIR /app

# copiamos solo node_modules ya instalado — sin cache de npm ni devDependencies
COPY --from=deps /app/node_modules ./node_modules
COPY package*.json ./
COPY src/ ./src/

EXPOSE 8083

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
  CMD node -e "require('http').get('http://localhost:8083/health', (res) => process.exit(res.statusCode === 200 ? 0 : 1)).on('error', () => process.exit(1))"

CMD ["node", "src/index.js"]
