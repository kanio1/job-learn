# BFF contract responsibility matrix

Scope: configured live `tests-pom` routes. This matrix tests Nuxt/BFF
forwarding or transformation risk; backend controllers retain their status and
domain-contract matrices.

| BFF route family | BFF responsibility | Status/body | Header classification | Evidence |
|---|---|---|---|---|
| Payment create `POST /merchants/{id}/payment-orders` | adds JSON request content type when absent; mints a missing/blank idempotency key; forwards a supplied correlation id | backend `201`/replay `200`/Problem Details `409` are forwarded | `ETag`, `Location`, `Idempotency-Replayed`, response `X-Correlation-ID`: **FORWARDED**; request `Idempotency-Key`: **GENERATED_OR_FORWARDED**; response `Content-Type`: **GENERATED** by Nitro serialization | `payments-create.spec.ts` replay and correlation tests; backend `PaymentOrderHttpContractMvpTest` |
| Payment read, conditional GET and HEAD | forwards `If-None-Match`; returns an empty HEAD body | backend `200` DTO, `304` empty and HEAD `200` empty are preserved | `ETag`, `Cache-Control` (including `no-transform`), `Vary`, `Last-Modified`, response `X-Correlation-ID`: **FORWARDED**; `Content-Type`: **NOT_APPLICABLE** for empty 304/HEAD | `payments-conditional.spec.ts`; backend payment HTTP contract tests |
| Payment PATCH and lifecycle actions | forces merge-patch media type for PATCH; forwards `If-Match` and lifecycle idempotency key unchanged | backend success DTO and 428/412 Problem Details are forwarded | `ETag`, `Cache-Control` (including `no-transform`), `Vary`, response `X-Correlation-ID`: **FORWARDED**; request `If-Match`: **FORWARDED_OPAQUE**; request `Content-Type`: **GENERATED** for PATCH; `Accept-Patch`: **NOT_APPLICABLE** | `payments-conditional.spec.ts`, `payments-illegal-transitions.spec.ts`; backend `PaymentOrderController` OPTIONS contract |
| Merchant, tenant and support mutations | selected route handlers forward `If-Match`; session access token is attached server-side | backend success/Problem Details are forwarded | `ETag`, `Cache-Control`, `Vary`, response `X-Correlation-ID`: **FORWARDED** when supplied by backend; request `If-Match`: **FORWARDED**; `Idempotency-Replayed`: **NOT_APPLICABLE** | `merchants-concurrency.spec.ts`, `tenant-settings.spec.ts`, backend merchant/tenant REST tests |
| Audit/export and evidence downloads | route-specific raw response handling and query mapping | binary/download body is forwarded without JSON parsing | download `Content-Type`/disposition: **FORWARDED_BY_ROUTE**; common safe headers: **FORWARDED** where the route uses `backendApi`; conditional/idempotency headers: **NOT_APPLICABLE** | `audit.spec.ts`, `payments-evidence-export.spec.ts`; backend audit contract tests |
| BFF authentication and error handling | obtains Authorization from server session; converts only unavailable/non-JSON failures to local Problem Details | backend JSON Problem Details are forwarded; missing session/backend failures are BFF-generated Problem Details | `Authorization`: **SERVER_ONLY** (never response); `Content-Type`: **TRANSFORMED** to `application/problem+json` only for local errors; response correlation/cache/vary headers are **FORWARDED** when the backend supplied them | `backendApi.ts`, `session-guest.spec.ts`, `error-lab.spec.ts` |

## Header decisions

- `X-Correlation-ID` is **VERIFIED** for the payment-create route: a caller value
  is forwarded to the backend and the returned value is visible at the BFF. The
  corresponding test also checks that response headers and body contain no
  authorization material.
- `Accept-Patch` is **NOT_APPLICABLE** at the BFF: there is no supported
  `*.options.ts` route under `server/api/merchants/.../payment-orders`. The
  backend OPTIONS contract is intentionally covered at its REST seam; adding a
  synthetic Nuxt OPTIONS endpoint would change product behavior.
- `ETag`/`If-Match` and `Idempotency-Replayed` are reconciled by the payment
  create, conditional and lifecycle POM REST specs above. The backend retains
  validation/status grids; POM proves the BFF forwarding boundary only.

The allowlist in `server/utils/backendApi.ts` is the implementation source for
forwarded response headers. Do not add a BFF test merely to repeat backend
domain status rows.
