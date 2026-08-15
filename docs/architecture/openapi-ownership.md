# OpenAPI ownership (Wave 4 decision)

**Status:** `DECISION_RECORDED` (2026-08-14); Wave 5 tooling **IMPLEMENTED** (2026-08-15)  
**Gate:** `REST-OPENAPI-DRIFT-01`  
**Catalog:** [docs/testing/wave-5-openapi-springdoc/README.md](../testing/wave-5-openapi-springdoc/README.md)  
**Tooling:** runtime `/v3/api-docs` + path filter + authenticated exposure. Snapshot + CI = **Wave 6**.

This record closes the stop-gate in `status/index.md`: choose spec-first versus code-first **before** adding a generator, `/v3/api-docs`, or CI drift checks. Wave 5 adds springdoc (`starter-webmvc-api`) and filtered `/v3/api-docs`. It does **not** add Swagger UI, a committed `openapi.yaml`, or a second generator.

## Decision

| Field | Choice |
|---|---|
| Mode | **Code-first.** Controllers and Java request/response records are the source of implemented HTTP behaviour. |
| Owner | Backend Spring module (`apps/backend`). springdoc lives there. |
| Consumer | `apps/api-tests` asserts live HTTP. It must **not** generate OpenAPI. |
| Human contracts | Markdown under `specs/*/contracts` remains documentation and learning material, not a second machine source of truth. |
| Canonical artifact (Wave 5) | Runtime `/v3/api-docs` with path filtering. A committed snapshot is allowed only after generation is stable. |
| Second generator | **Forbidden.** Do not add OpenAPI Generator / spec-first codegen alongside springdoc. |

## Exclude set (Wave 5 generation)

Do not publish these as the public contract:

- `/api/checkout-lab/**`
- `/api/mirror-lab/**`
- `/api/rls-lab/**`
- `/api/test/**`
- Error Lab / Nitro-only internal routes
- Actuator (if added later) unless an explicit ops exception is recorded

## Allowlist / false-positive policy

Intentional differences that CI drift must **not** treat as breaks without an explicit review:

- Lab flags (`app.checkout-lab`, `mirror-lab`, `rls-lab`) changing 404 vs 2xx for lab paths (already excluded).
- `application/problem+json` bodies versus OpenAPI default error schemas.
- Relative `Location` on 201 (evidence, payment create) versus absolute URI examples.
- Header contracts (`ETag`, `Vary`, `Cache-Control`, `Idempotency-Replayed`) that annotations may omit (OA-ALW-01; not a failing oracle in Wave 5).

## Explicit non-goals (this decision)

- Spec-first YAML as the owner of production APIs.
- Swagger UI enabled on the `prod` profile.
- Generating client SDKs from OpenAPI in this repository.
- Using api-tests JSON Schema files as a substitute OpenAPI document.

## Wave 5 (implemented)

Catalog: [docs/testing/wave-5-openapi-springdoc/README.md](../testing/wave-5-openapi-springdoc/README.md).

Wave 5 closed **generator + path filter + exposure** (not CI drift):

1. `springdoc-openapi-starter-webmvc-api` **3.1.0** (no UI starter) with `paths-to-exclude` / `packages-to-exclude` matching the exclude set. Do **not** exclude `*.internal` packages — production controllers live under `*.internal.web`.
2. `/v3/api-docs` is authenticated on non-prod (no `permitAll`); `springdoc.api-docs.enabled=false` on `prod`; Swagger UI off everywhere.
3. Tests: public paths present, lab/test paths absent (labs **on**), no `.internal` in the document. Allowlist (OA-ALW-01): problem+json vs default error schemas, relative `Location`, omitted header annotations — not failing oracles.

Existing HTTP use cases (multipart evidence, labs, live POM, seed/reset) are unchanged; see the catalog impact table.

## Wave 6 (after Wave 5 is green)

Committed snapshot + CI breaking-change detection with this allowlist. Do not start until runtime `/v3/api-docs` is stable.
