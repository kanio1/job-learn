FROM docker.io/library/node:22-bookworm-slim AS build
WORKDIR /src
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0
ENV NODE_OPTIONS=--max-old-space-size=6144
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN corepack pnpm install --frozen-lockfile --config.dangerouslyAllowAllBuilds=true
COPY . .
RUN corepack pnpm exec nuxi prepare
RUN corepack pnpm typecheck
RUN corepack pnpm build

FROM docker.io/library/node:22-bookworm-slim
WORKDIR /app
ENV NODE_ENV=production \
    HOST=0.0.0.0 \
    PORT=3000 \
    NITRO_HOST=0.0.0.0 \
    NITRO_PORT=3000
COPY --from=build /src/.output /app/.output
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
