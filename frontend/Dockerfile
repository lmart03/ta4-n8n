# syntax=docker/dockerfile:1.7
# ---------------------------------------------------------------------------
# TA4 Frontend: imagen multi-stage (build con Node, runtime con nginx).
# nginx sirve la SPA y reenvía /api al backend del compose.
# ---------------------------------------------------------------------------

ARG NODE_VERSION=24
ARG NGINX_VERSION=1.27-alpine

# --------------------------- Etapa 1: build --------------------------------
FROM node:${NODE_VERSION}-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build -- --configuration production

# -------------------------- Etapa 2: runtime -------------------------------
FROM nginx:${NGINX_VERSION} AS runtime

COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ta4-front/browser/ /usr/share/nginx/html/

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
