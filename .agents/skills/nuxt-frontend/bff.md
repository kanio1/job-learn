# Nitro BFF

Browser → Nuxt `server/api/**` → Spring. `backendApi` reads `session.secure.accessToken` and sets `Authorization` on the **outbound** Spring call only.

## Handler shape

Copy an existing file. Use a **method suffix** (`.get.ts`, `.post.ts`, …) so the wrong verb is 405 from Nitro, not from ad-hoc checks. That is separate from body parsing: `readBody` on a GET throws 405 in h3 even without a suffix. Do **not** add `readValidatedBody` / Zod on the proxy — Spring owns the HTTP contract; the client Zod schema is the dashboard oracle.

```ts
export default defineEventHandler(async (event) => {
  const merchantId = getRouterParam(event, 'merchantId')
  const idempotencyKey = getHeader(event, 'idempotency-key')
  const correlationId = getHeader(event, 'x-correlation-id')
  return backendApi(event, `/api/merchants/${merchantId}/payment-orders`, {
    method: 'POST',
    body: await readBody(event),
    idempotencyKey,
    correlationId: correlationId || undefined,
  })
})
```

Forward `If-Match` / `If-None-Match` / `Idempotency-Key` / `X-Correlation-ID` through `backendApi` opts — do not reimplement fetch.

Use `#server/utils/backendApi` only if a deep relative import is painful (Nuxt 4.3+ `#server` alias). Relative import of `backendApi` is the current house style and is fine.

## Headers

`forwardBackendHeaders` allowlist (extend this list in one place if a spec adds a header):

`ETag`, `Cache-Control`, `Vary`, `X-Correlation-ID`, `Location`, `Accept-Patch`, `Allow`, `Retry-After`, `WWW-Authenticate`, `Idempotency-Replayed`, `Last-Modified`, `Lab-Signature`, `Lab-Event-Id`.

Never add `Authorization` or `Set-Cookie` from Spring to the browser response.

Client: `useApiClient` + `$fetch.raw` so those headers survive. Plain `$fetch` is wrong for any screen that shows ETag / Location / correlation id.

## Auth

- `requireUserSession` inside `backendApi`. Missing token → 401 `missing_access_token`.
- Session user (username, roles, tenant) may be client-visible. Access/refresh tokens must not.
- Do not use Node `Buffer` to decode JWTs; follow `server/routes/auth/keycloak.get.ts`.
- Do not add `fromNodeMiddleware` / Express-style `req, res, next` handlers.

## Errors

If Spring returns `application/problem+json`, preserve status + body (see `backendApi` catch). Client maps that through `problemDetailsSchema` (`.passthrough()` for extensions).

Backend down → user-visible backend-unavailable, not a fake 403.

## Client contract

`useApiClient().request(path, schema, opts)` returns `{ data, status, headers, problem, raw }`. Domain composables call **proxy paths** (`/api/merchants/...`). `runtimeConfig.public.apiBaseUrl` is for `backendApi` on the server only — never from Vue. New dashboard calls go through `useApiClient`, not a second `$fetch.raw` in the page.
