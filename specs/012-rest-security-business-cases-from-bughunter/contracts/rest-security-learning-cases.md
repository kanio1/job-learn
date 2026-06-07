# Contract: REST Security Learning Cases from BugHunter

This catalog defines future defensive API/business cases for the Payment Quality Engineering Lab. It is planning only. Do not implement tests from this file until the selected backend behavior exists and has been reviewed.

Common payment resource headers:

- Request auth: `Authorization: Bearer <jwt>` except allowed CORS/contract `OPTIONS`.
- Correlation: optional `X-Correlation-ID`, echoed or generated in response.
- Sensitive payment responses: `Cache-Control: no-store`.
- Payment errors: `Content-Type: application/problem+json`.
- Conditional mutations: `If-Match: "v{version}"`.
- Idempotent create/lifecycle requests: `Idempotency-Key`.

## Category A - API Misconfiguration

## Case A1 - Metadata PATCH rejects mass assignment outside metadata

### Business situation
A support operator edits payment metadata on `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` and the request accidentally includes fields such as `status`, `amountMinor`, or `merchantId`.

### REST/API behavior to design
Endpoint: `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`. Required headers: `Authorization`, `If-Match`, `Content-Type: application/merge-patch+json`, `Accept: application/json`. Success remains `200 OK` only for allowed metadata changes with `ETag`, `Cache-Control: no-store`, `Vary: Authorization, If-Match`, and JSON body. Unknown top-level writable fields should return `400 Bad Request` with `application/problem+json`, code `UNKNOWN_TOP_LEVEL_FIELD`, and `X-Correlation-ID`.

### Important HTTP concepts
PATCH, content type, body validation, problem details, ETag, If-Match, cache, correlation ID.

### Security/quality risk
Mass assignment can let clients change server-owned fields like status, amount, merchant ownership, version, or lifecycle timestamps.

### Proposed implementation idea
Keep `MetadataPatchRequest` as a narrow write DTO and configure strict unknown-field handling for payment write DTOs or validate top-level JSON keys before mapping. Do not change the domain model directly from request JSON.

### Future Rest Assured test idea
Send metadata PATCH with `status` next to `metadata` and assert `400`, `application/problem+json`, unchanged persisted status, and unchanged `ETag`.

## Case A2 - Unknown JSON fields policy is explicit for create payment order

### Business situation
A merchant client creates a payment order and sends extra fields such as `adminNote`, `status`, or `capturedAmountMinor`.

### REST/API behavior to design
Endpoint: `POST /api/merchants/{merchantId}/payment-orders`. Required headers: `Authorization`, `Idempotency-Key`, `Content-Type: application/json`, `Accept: application/json`. Success remains `201 Created` with `Location`, `ETag`, and JSON body. Unknown top-level fields should either be rejected with `400 Bad Request` and problem code `UNKNOWN_TOP_LEVEL_FIELD`, or explicitly documented as ignored; the chosen policy must be consistent.

### Important HTTP concepts
JSON binding, validation, `201 Created`, `Location`, idempotency, problem details.

### Security/quality risk
Silent acceptance of unknown fields hides client contract drift and may become mass assignment when DTOs change later.

### Proposed implementation idea
Choose strict unknown-field rejection for payment write DTOs while keeping response DTOs separate from entities.

### Future Rest Assured test idea
Create an order with an extra `status` field and assert the documented policy plus no unintended persisted lifecycle state.

## Case A3 - Payment response DTO does not expose internal persistence fields

### Business situation
A merchant reader opens payment detail and should see business facts, not database internals.

### REST/API behavior to design
Endpoint: `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`. Required headers: `Authorization`, `Accept: application/json`. Success: `200 OK`, `Content-Type: application/json`, `ETag`, `Cache-Control: no-store`, `Vary: Authorization`. Response body must not include internal fields such as database version number, idempotency key hash, request fingerprint hash, PSP mock internals beyond allowed safe references, stack traces, or raw actor token data.

### Important HTTP concepts
Read DTO, response body contract, cache, ETag, data minimization.

### Security/quality risk
Overexposed DTOs leak implementation details that can aid misuse and make API consumers depend on unstable internals.

### Proposed implementation idea
Keep explicit mappers such as `PaymentOrderMapper` and define allowed response fields in the contract before adding any field.

### Future Rest Assured test idea
Assert allowed fields exist and selected forbidden JSON paths are absent on payment detail and lifecycle responses.

## Case A4 - Read DTO and write DTO are deliberately separate

### Business situation
The operations console reads lifecycle facts but writes only a small command body for capture, cancel, refund, or metadata.

### REST/API behavior to design
Endpoints: lifecycle `POST` actions and metadata `PATCH`. Request DTOs should accept only command fields such as `amountMinor`, `reason`, or `metadata`. Response DTOs may include richer fields such as timestamps and status. Invalid write-only/read-only mixing should return `400 Bad Request` or be rejected by strict unknown-field policy.

### Important HTTP concepts
Command DTO, representation DTO, validation, problem details, content negotiation.

### Security/quality risk
Reusing response DTOs as request DTOs can accidentally allow clients to set fields that should be derived by the domain.

### Proposed implementation idea
Keep create, lifecycle, metadata, detail, list, summary, and history DTOs separate in `payment.internal.web`.

### Future Rest Assured test idea
Submit a lifecycle response-shaped body to capture and assert only supported command fields affect behavior.

## Category B - BOLA / IDOR / Tenant Isolation

## Case B1 - Merchant A cannot read Merchant B payment detail

### Business situation
Merchant A learns or guesses a payment order ID that belongs to Merchant B.

### REST/API behavior to design
Endpoint: `GET /api/merchants/{merchantIdB}/payment-orders/{paymentOrderIdB}` with Merchant A token. Required headers: `Authorization`, `Accept: application/json`. Expected error should be the chosen ownership policy: preferably masked `404 Not Found` with `application/problem+json`, `Cache-Control: no-store`, `Vary: Authorization`, and no body detail revealing Merchant B.

### Important HTTP concepts
Authorization, path ownership, masked 404, cache, problem details.

### Security/quality risk
BOLA/IDOR lets one tenant read another tenant's payment data.

### Proposed implementation idea
Continue using `findForMerchant` plus JWT `merchant_id` ownership check. Document which endpoints mask versus return `403`.

### Future Rest Assured test idea
Create order under Merchant B and read it using Merchant A token; assert masked result and no leaked fields.

## Case B2 - Merchant A cannot mutate Merchant B payment lifecycle

### Business situation
Merchant A attempts to capture or cancel Merchant B's payment order.

### REST/API behavior to design
Endpoint family: lifecycle `POST /api/merchants/{merchantIdB}/payment-orders/{paymentOrderIdB}/{action}`. Required headers: `Authorization`, `Idempotency-Key`, `If-Match`, `Content-Type: application/json`. Expected error should be clarified as `403 Forbidden` or masked `404 Not Found`, with `application/problem+json`, `Cache-Control: no-store`, and no lifecycle state change.

### Important HTTP concepts
BOLA, mutation authorization, `If-Match`, idempotency, persistence oracle.

### Security/quality risk
Cross-tenant mutation is more severe than cross-tenant read because it can move funds or disrupt payments.

### Proposed implementation idea
Apply a shared ownership guard before precondition and before idempotency reservation for merchant lifecycle actors.

### Future Rest Assured test idea
Attempt capture with Merchant A token against Merchant B path and assert error plus unchanged status/history.

## Case B3 - HEAD does not leak cross-tenant existence

### Business situation
A merchant probes `HEAD` to learn whether a payment order ID exists for another merchant.

### REST/API behavior to design
Endpoint: `HEAD /api/merchants/{merchantIdB}/payment-orders/{paymentOrderIdB}` with Merchant A token. Expected error: same ownership policy as `GET`. Response must have no body. For masked `404`, include `Cache-Control: no-store`, `Vary: Authorization`, and `X-Correlation-ID`; do not include `ETag` for inaccessible resources.

### Important HTTP concepts
HEAD semantics, existence leakage, cache, ETag, authorization.

### Security/quality risk
Different `HEAD` status or headers can reveal whether another tenant's resource exists.

### Proposed implementation idea
Route `HEAD` through the same readable-resource lookup as `GET` and avoid adding resource headers until after authorization.

### Future Rest Assured test idea
Compare cross-tenant `GET` and `HEAD` outcomes and assert `HEAD` has no body and no `ETag`.

## Case B4 - Masked 404 uses no-store

### Business situation
The API masks cross-tenant payment detail as not found.

### REST/API behavior to design
Endpoint family: payment detail `GET`, `HEAD`, history `GET`, lifecycle `POST`, metadata `PATCH` where masked ownership applies. Error: `404 Not Found`, `Content-Type: application/problem+json` where a body exists, `Cache-Control: no-store`, `Vary: Authorization`, `X-Correlation-ID`.

### Important HTTP concepts
Masked 404, cache control, Vary, problem details.

### Security/quality risk
If a shared cache stores masked or unmasked responses without `Vary: Authorization`, one user's authorization result can affect another user's view.

### Proposed implementation idea
Centralize payment error headers through `PaymentHttpHeaders` and cover masked 404 explicitly in the contract.

### Future Rest Assured test idea
Assert cross-tenant masked `404` includes `Cache-Control: no-store` and `Vary: Authorization`.

## Case B5 - Platform user versus merchant user behavior is explicit

### Business situation
A platform payment reader needs cross-merchant read, while merchant readers are tenant-bound.

### REST/API behavior to design
Endpoints: detail/list/summary/history. Platform read/audit roles may receive `200 OK` across merchants according to the role matrix. Merchant roles must match `merchant_id` path claim. Errors: missing platform authority `403`, merchant mismatch masked `404` or `403` per clarified policy.

### Important HTTP concepts
Role matrix, tenant ownership, `403` versus masked `404`, `Vary: Authorization`.

### Security/quality risk
Confusing platform and merchant semantics causes either over-permissive reads or broken support/audit workflows.

### Proposed implementation idea
Document actor classes in the payment contract and keep controller checks aligned with `SecurityConfig`.

### Future Rest Assured test idea
Use Merchant A reader, Merchant B reader, platform reader, and platform auditor tokens against the same order and assert each expected outcome.

## Category C - BFLA / Role Authorization

## Case C1 - Merchant reader cannot capture

### Business situation
A merchant employee with read-only permission tries to capture an authorized payment.

### REST/API behavior to design
Endpoint: `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture`. Required request headers if authorized would be `Authorization`, `Idempotency-Key`, `If-Match`, `Content-Type: application/json`. Expected error: `403 Forbidden`, `application/problem+json` if handled by the application, no lifecycle mutation, no history entry, no PSP mock call.

### Important HTTP concepts
BFLA, Spring Security route authorization, status code, persistence oracle.

### Security/quality risk
Function-level authorization failures let read-only actors perform money-moving actions.

### Proposed implementation idea
Keep route matchers separate for read and lifecycle authorities, and avoid deriving lifecycle permission from read permission.

### Future Rest Assured test idea
Reader token attempts capture; assert `403` and unchanged payment status.

## Case C2 - Auditor reads history but cannot mutate

### Business situation
A platform auditor investigates payment history but must not authorize, capture, cancel, refund, or patch metadata.

### REST/API behavior to design
Endpoint allowed: `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history` returns `200 OK` for `platform:payments:audit`. Mutation endpoints return `403 Forbidden` for the same token. All responses include `Cache-Control: no-store` and `X-Correlation-ID`.

### Important HTTP concepts
Least privilege, read-only audit role, lifecycle mutation denial, cache.

### Security/quality risk
Audit roles often get over-granted because they need broad visibility.

### Proposed implementation idea
Keep `platform:payments:audit` limited to history/detail decisions only if explicitly approved, not lifecycle operations.

### Future Rest Assured test idea
Use one auditor token to assert history success and capture/refund/metadata patch denial.

## Case C3 - Platform lifecycle role can operate across merchants

### Business situation
A platform operations user needs to cancel a payment for any merchant during support intervention.

### REST/API behavior to design
Endpoint: lifecycle `POST` action under any merchant path. Required headers: `Authorization` with `platform:payments:lifecycle`, `Idempotency-Key`, `If-Match`, `Content-Type: application/json`, `Accept: application/json`. Success: `200 OK`, JSON lifecycle response, `ETag`, `Cache-Control: no-store`, `Vary: Authorization, If-Match`.

### Important HTTP concepts
Platform authority, tenant bypass by role, idempotency, conditional update, audit/history.

### Security/quality risk
Platform cross-merchant power must be explicit and auditable, not accidental.

### Proposed implementation idea
Allow `platform:payments:lifecycle` in ownership guard and record actor/correlation in status history.

### Future Rest Assured test idea
Platform lifecycle token cancels Merchant B order and asserts status/history include safe actor and correlation facts.

## Case C4 - Missing scope or missing role is distinguishable from ownership mismatch

### Business situation
A JWT is valid but lacks any payment authority, or has a merchant role without a `merchant_id`.

### REST/API behavior to design
Endpoint family: payment create/read/list/lifecycle/history. Missing route authority should return `403 Forbidden`. Valid merchant role with missing `merchant_id` on tenant-bound endpoints should return documented `403` or masked `404`. Error bodies should not include JWT claims.

### Important HTTP concepts
JWT claims, authorization, ownership, `403` versus `404`, problem details.

### Security/quality risk
Ambiguous failures make tests weak and can leak claim details in error messages.

### Proposed implementation idea
Create an authorization matrix table per endpoint and keep error messages generic.

### Future Rest Assured test idea
Use denied token and merchant-reader-without-merchant-id token against detail and lifecycle endpoints; assert distinct documented results.

## Category D - CORS / Browser Boundary

## Case D1 - Preflight allows Idempotency-Key for create and lifecycle

### Business situation
The Nuxt app sends `Idempotency-Key` from browser/client flows.

### REST/API behavior to design
Endpoint: `OPTIONS /api/merchants/{merchantId}/payment-orders` and lifecycle action paths. Request headers: `Origin: http://localhost:3000`, `Access-Control-Request-Method: POST`, `Access-Control-Request-Headers: authorization,content-type,idempotency-key`. Response: `200 OK` or `204 No Content`, `Access-Control-Allow-Origin: http://localhost:3000`, allowed methods include `POST`, allowed headers include `Idempotency-Key`. No bearer token required for preflight.

### Important HTTP concepts
CORS preflight, custom headers, idempotency, browser boundary.

### Security/quality risk
If preflight blocks idempotency headers, browser clients may drop retry safety or fail only in UI.

### Proposed implementation idea
Keep dev/test CORS allow headers aligned with backend-required request headers.

### Future Rest Assured test idea
Send preflight with `Idempotency-Key` and assert CORS allow headers without authentication.

## Case D2 - Preflight allows If-Match for conditional mutations

### Business situation
The operations console sends `If-Match` for capture, cancel, refund, and metadata PATCH.

### REST/API behavior to design
Endpoint: `OPTIONS /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` and action paths. Request headers: `Origin`, `Access-Control-Request-Method: PATCH` or `POST`, `Access-Control-Request-Headers: authorization,content-type,if-match`. Response must allow `If-Match` and the requested method for known origins.

### Important HTTP concepts
CORS, conditional requests, `If-Match`, PATCH, lifecycle POST.

### Security/quality risk
Without preflight support, browser clients cannot use lost-update protection.

### Proposed implementation idea
Expose `If-Match` in dev/test CORS allowed headers and document production profile separately.

### Future Rest Assured test idea
Assert preflight for metadata PATCH includes `Access-Control-Allow-Headers` containing `If-Match`.

## Case D3 - Preflight without Authorization is accepted but actual request is protected

### Business situation
Browsers do not send the bearer token on CORS preflight, but the actual payment request still needs auth.

### REST/API behavior to design
Preflight: `OPTIONS /api/**` from known origin succeeds without `Authorization`. Actual request: `GET`, `POST`, `PATCH`, or `HEAD` without `Authorization` returns `401 Unauthorized` with `WWW-Authenticate` from resource server behavior where applicable.

### Important HTTP concepts
CORS preflight, authentication, `401`, `WWW-Authenticate`.

### Security/quality risk
Teams sometimes require auth on preflight and break browsers, or permit actual requests because `OPTIONS` is permitted.

### Proposed implementation idea
Keep `HttpMethod.OPTIONS, /api/**` permitted, while all business methods remain under Spring Security authorization.

### Future Rest Assured test idea
Assert unauthenticated `OPTIONS` succeeds and unauthenticated `POST /capture` returns `401`.

## Case D4 - Exposed headers include ETag, Location, X-Correlation-ID, Allow, and Accept-Patch

### Business situation
The browser app needs to read backend contract headers to store version markers, navigate to created resources, and show trace IDs.

### REST/API behavior to design
CORS responses for known origin should include `Access-Control-Expose-Headers` with at least `ETag`, `Location`, `X-Correlation-ID`, and preferably `Allow`, `Accept-Patch`, `Cache-Control`, `Vary`. Payment success responses still include their normal headers.

### Important HTTP concepts
CORS exposed headers, ETag, Location, correlation ID, OPTIONS contract.

### Security/quality risk
The backend may send correct headers that browser JavaScript cannot read, causing unsafe stale updates or weak support evidence.

### Proposed implementation idea
Extend CORS exposed headers only where useful, and keep the frontend server proxy forwarding the same set.

### Future Rest Assured test idea
Send CORS request from known origin and assert `Access-Control-Expose-Headers` includes `ETag` and `X-Correlation-ID`.

## Case D5 - Unknown Origin is not trusted

### Business situation
A request arrives with `Origin: https://unknown.example`.

### REST/API behavior to design
Endpoint family: any `/api/**`. For unknown origin preflight, response must not include `Access-Control-Allow-Origin: https://unknown.example`. Actual non-browser clients may still receive normal auth errors or successes based on credentials, but browser CORS trust must not be granted.

### Important HTTP concepts
CORS origin allow-list, browser boundary, authentication versus origin trust.

### Security/quality risk
Reflecting arbitrary origins allows browser-based cross-origin reads when combined with credentials or token exposure mistakes.

### Proposed implementation idea
Use explicit allowed origins in `SecurityConfig` and avoid origin reflection.

### Future Rest Assured test idea
Send preflight with unknown origin and assert no matching `Access-Control-Allow-Origin`.

## Category E - HTTP Methods and Method Tampering

## Case E1 - X-HTTP-Method-Override is ignored or rejected

### Business situation
A client sends `POST /capture` or `POST /payment-orders/{id}` with `X-HTTP-Method-Override: PATCH` hoping the server treats it as a different method.

### REST/API behavior to design
Endpoint family: all payment endpoints. Request header `X-HTTP-Method-Override` should not alter routing. Prefer `400 Bad Request` or ignore the header and process the actual method only. If rejected, return `application/problem+json`, `Cache-Control: no-store`, and no mutation.

### Important HTTP concepts
HTTP method semantics, header trust, method override, problem details.

### Security/quality risk
Method override can bypass route authorization or method-specific protections if enabled accidentally.

### Proposed implementation idea
Do not register method-override filters. Optionally add a defensive filter in shared web to reject override headers for `/api/**`.

### Future Rest Assured test idea
Send override header on a lifecycle endpoint and assert it cannot convert a denied method into a mutation.

## Case E2 - TRACE is disabled

### Business situation
A client sends `TRACE /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`.

### REST/API behavior to design
Endpoint family: `/api/**`. Expected result: `405 Method Not Allowed` or server-level disabled method response. Must not echo request headers or body. Include `Allow` if Spring handles the route; include no sensitive data.

### Important HTTP concepts
TRACE, method safety, `405`, header echo risk.

### Security/quality risk
TRACE can expose headers in echo-style responses on poorly configured servers.

### Proposed implementation idea
Rely on Spring/Tomcat defaults and document the expected result; add explicit hardening only if verification shows echo behavior.

### Future Rest Assured test idea
Send TRACE with `Authorization` and assert the token is not echoed.

## Case E3 - 405 Method Not Allowed includes Allow

### Business situation
A developer accidentally calls `PUT /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`.

### REST/API behavior to design
Endpoint: payment detail resource with unsupported method. Expected: `405 Method Not Allowed`, `Allow: GET, HEAD, PATCH, OPTIONS`, `Content-Type: application/problem+json`, `Cache-Control: no-store`, `X-Correlation-ID`.

### Important HTTP concepts
Method negotiation, `Allow`, problem details, cache.

### Security/quality risk
Missing `Allow` makes clients guess and weakens contract tests.

### Proposed implementation idea
Keep `HttpRequestMethodNotSupportedException` mapped through payment error handler and set supported methods from Spring.

### Future Rest Assured test idea
Call unsupported `PUT` and assert `405`, `Allow`, problem code, and no state change.

## Case E4 - OPTIONS detail advertises Allow and Accept-Patch

### Business situation
A client discovers the payment detail resource capabilities before choosing PATCH.

### REST/API behavior to design
Endpoint: `OPTIONS /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`. Response: `204 No Content`, `Allow: GET, HEAD, PATCH, OPTIONS`, `Accept-Patch: application/merge-patch+json`, `X-Correlation-ID`, no body. Authentication policy: allowed as contract/CORS request, not a business authorization decision.

### Important HTTP concepts
OPTIONS, Allow, Accept-Patch, no-body response, correlation ID.

### Security/quality risk
Inconsistent capability discovery causes clients to use wrong methods or media types.

### Proposed implementation idea
Keep explicit `OPTIONS` handlers close to controller routes or centralize capabilities by endpoint family.

### Future Rest Assured test idea
Assert `OPTIONS` detail returns `204`, exact `Allow`, `Accept-Patch`, and empty body.

## Case E5 - PATCH requires supported media type

### Business situation
A client sends metadata update with `Content-Type: text/plain` or missing content type.

### REST/API behavior to design
Endpoint: `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`. Required media type: `application/merge-patch+json` with documented temporary `application/json` compatibility if retained. Unsupported media type returns `415 Unsupported Media Type`, `application/problem+json`, `Accept-Patch: application/merge-patch+json`, `Cache-Control: no-store`, and no metadata change.

### Important HTTP concepts
Content-Type, `415`, Accept-Patch, PATCH, validation.

### Security/quality risk
Loose media-type acceptance can hide parser differences and contract drift.

### Proposed implementation idea
Eventually narrow PATCH consumes to merge-patch only after frontend proxy compatibility is confirmed.

### Future Rest Assured test idea
PATCH with `text/plain`; assert `415`, `Accept-Patch`, and unchanged metadata.

## Category F - Host / Proxy / Header Trust

## Case F1 - Host poisoning does not affect Location

### Business situation
A client creates a payment order with `Host: attacker.example`.

### REST/API behavior to design
Endpoint: `POST /api/merchants/{merchantId}/payment-orders`. Success: `201 Created`, `Location` should be a safe relative path such as `/api/merchants/{merchantId}/payment-orders/{paymentOrderId}` or a server-configured trusted base URL, never derived from untrusted `Host`.

### Important HTTP concepts
Host header, Location, resource creation, idempotency.

### Security/quality risk
Host poisoning can create malicious absolute links in responses, emails, logs, or client redirects.

### Proposed implementation idea
Continue using relative `Location` for payment creation and document this as the preferred local lab policy.

### Future Rest Assured test idea
Create order with hostile `Host` and assert `Location` does not contain that host.

## Case F2 - X-Forwarded-Host is ignored unless trusted

### Business situation
A client sends `X-Forwarded-Host: attacker.example` directly to the backend.

### REST/API behavior to design
Endpoint family: create and any future link-producing endpoint. Unless a trusted proxy profile is explicitly enabled, response links and security decisions must ignore `X-Forwarded-Host`.

### Important HTTP concepts
Forwarded headers, proxy trust boundary, Location, deployment profile.

### Security/quality risk
Trusting forwarded headers from direct clients can poison links and audit evidence.

### Proposed implementation idea
Do not enable forwarded-header processing by default. If later needed, put it behind profile/config and document trusted proxy assumptions.

### Future Rest Assured test idea
Send `X-Forwarded-Host` on create and assert no response header uses it.

## Case F3 - X-Forwarded-Proto does not create HTTP payment links

### Business situation
A proxy or client sends `X-Forwarded-Proto: http` while the public boundary should be HTTPS.

### REST/API behavior to design
Endpoint family: any future absolute link generation. Current relative `Location` is acceptable. If absolute links are introduced, use configured external base URL and never downgrade secure payment links from untrusted forwarded proto.

### Important HTTP concepts
Forwarded proto, HTTPS, Location, link generation.

### Security/quality risk
Downgrade links can send users to insecure URLs or break secure-cookie assumptions.

### Proposed implementation idea
Keep relative links for local lab APIs. Defer absolute payment links until a trusted proxy configuration exists.

### Future Rest Assured test idea
Send `X-Forwarded-Proto: http` and assert no absolute `http://` payment link appears.

## Case F4 - Forwarded header trust boundary is documented

### Business situation
The lab may later run behind a reverse proxy, but direct local backend requests are common in tests.

### REST/API behavior to design
No new endpoint required. Documentation should state whether `Forwarded`, `X-Forwarded-Host`, `X-Forwarded-Proto`, and `X-Forwarded-For` are ignored or trusted under each profile. Any future behavior must be testable with `/api/status` and payment create/detail endpoints.

### Important HTTP concepts
Proxy trust, deployment profiles, headers, audit evidence.

### Security/quality risk
Implicit proxy trust creates different security behavior in local tests versus deployed environments.

### Proposed implementation idea
Add a small architecture note before enabling forwarded-header support.

### Future Rest Assured test idea
Assert local/test profile ignores forwarded host/proto on link-producing endpoints.

## Category G - Cache and Security Headers

## Case G1 - Payment detail uses Cache-Control no-store

### Business situation
A user opens payment detail containing amount, status, metadata, and merchant identity.

### REST/API behavior to design
Endpoint: `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`. Success: `200 OK`, `Cache-Control: no-store`, `Vary: Authorization`, `ETag`, `X-Correlation-ID`, JSON body.

### Important HTTP concepts
Cache-Control, Vary, ETag, sensitive data.

### Security/quality risk
Payment details can be stored in browser or proxy caches and shown to the wrong user.

### Proposed implementation idea
Keep `PaymentHttpHeaders.sensitivePaymentResponse` on all detail responses.

### Future Rest Assured test idea
Assert payment detail has `Cache-Control: no-store` and `Vary: Authorization`.

## Case G2 - Payment list uses Cache-Control no-store

### Business situation
A merchant lists payment orders with filters that reveal operational history.

### REST/API behavior to design
Endpoint: `GET /api/merchants/{merchantId}/payment-orders`. Success: `200 OK`, JSON list, `Cache-Control: no-store`, `Vary: Authorization`, `X-Correlation-ID`.

### Important HTTP concepts
List resources, cache, authorization, query parameters.

### Security/quality risk
Cached lists can leak payment existence, amounts, or merchant activity.

### Proposed implementation idea
Apply sensitive payment headers consistently to list and summary endpoints.

### Future Rest Assured test idea
Call list with a reader token and assert no-store on success.

## Case G3 - Masked 404 uses Cache-Control no-store

### Business situation
Cross-tenant access is masked as not found.

### REST/API behavior to design
Endpoint: payment detail `GET` or `HEAD`. Error: `404 Not Found`, `Cache-Control: no-store`, `Vary: Authorization`, `X-Correlation-ID`; body only for `GET`.

### Important HTTP concepts
Cache, masked 404, HEAD, Vary.

### Security/quality risk
Caching a masked response can break valid users later or reveal authorization differences.

### Proposed implementation idea
Make not-found payment errors use the same payment error headers as other sensitive responses.

### Future Rest Assured test idea
Assert cross-tenant masked 404 has no-store for both `GET` and `HEAD`.

## Case G4 - application/problem+json errors use Cache-Control no-store

### Business situation
Payment API returns validation, precondition, authorization, or lifecycle errors.

### REST/API behavior to design
Endpoint family: all payment endpoints. Error body: `application/problem+json` with `type`, `title`, `status`, `detail`, `code`, `correlationId`, and compatibility fields. Error headers: `Cache-Control: no-store`, appropriate `Vary`, `X-Correlation-ID`.

### Important HTTP concepts
Problem details, cache, error contract, correlation ID.

### Security/quality risk
Errors may include operational context and must not be stored by shared caches.

### Proposed implementation idea
Keep payment-scoped exception handling and avoid falling through to default HTML/error pages.

### Future Rest Assured test idea
Trigger malformed `If-Match` and assert problem body plus no-store.

## Case G5 - Vary Authorization where applicable

### Business situation
The same URL can return different results depending on bearer token and role.

### REST/API behavior to design
Endpoint family: authenticated payment reads and errors. Responses should include `Vary: Authorization`; conditional mutations should include `Vary: Authorization, If-Match`; idempotent create may include `Vary: Authorization, Idempotency-Key`.

### Important HTTP concepts
Vary, Authorization, caches, conditional headers.

### Security/quality risk
Without `Vary`, shared intermediaries can reuse a response across users.

### Proposed implementation idea
Keep centralized header constants and define expected `Vary` per endpoint family.

### Future Rest Assured test idea
Assert expected `Vary` on detail, list, create replay, and stale `If-Match` responses.

## Category H - Idempotency and Lifecycle Race Conditions

## Case H1 - Same idempotency key and same payload replays create

### Business situation
A merchant retries create after losing the first response.

### REST/API behavior to design
Endpoint: `POST /api/merchants/{merchantId}/payment-orders`. Same `Idempotency-Key` and same payload returns the original representation, currently `200 OK` for replay, with same `paymentOrderId`, JSON body, `ETag`, `Cache-Control: no-store`, and new/effective `X-Correlation-ID`.

### Important HTTP concepts
Idempotency, retry safety, `201` versus `200`, fingerprint.

### Security/quality risk
Without replay, clients create duplicate payment orders after network failures.

### Proposed implementation idea
Keep create idempotency scoped by merchant and request fingerprint.

### Future Rest Assured test idea
POST the same create twice and assert second response reuses original payment ID.

## Case H2 - Same idempotency key and different payload conflicts

### Business situation
A merchant accidentally reuses an idempotency key for a different amount or client reference.

### REST/API behavior to design
Endpoint: `POST /api/merchants/{merchantId}/payment-orders`. Same key, different fingerprint returns `409 Conflict`, `application/problem+json`, code `IDEMPOTENCY_CONFLICT`, no new order.

### Important HTTP concepts
Idempotency conflict, request fingerprint, status `409`.

### Security/quality risk
Reusing keys across different business intents can return the wrong payment result.

### Proposed implementation idea
Hash stable request facts and compare before replay.

### Future Rest Assured test idea
Reuse key with different amount and assert `409` plus no additional payment order.

## Case H3 - Same idempotency key and different lifecycle operation conflicts

### Business situation
A client uses the same idempotency key for `authorize` and then `capture`.

### REST/API behavior to design
Endpoint family: lifecycle actions. Same key with different action should not replay a different operation. Expected behavior: action-scoped idempotency allows separate records per action only if the contract says action is part of scope; alternatively return `409` when a key is reused across actions. The chosen rule must be explicit.

### Important HTTP concepts
Idempotency scope, lifecycle action, conflict, domain transition.

### Security/quality risk
Blurring action scope can replay authorize as capture or hide an unsafe duplicate.

### Proposed implementation idea
Keep lifecycle scope as merchant + payment order + action + key and document cross-action reuse policy.

### Future Rest Assured test idea
Use same key for authorize and capture and assert documented result with no wrong replay.

## Case H4 - Stale If-Match on capture returns 412

### Business situation
Two operators load `ETag: "v1"`. One authorizes or patches first; the other tries capture using stale `"v1"`.

### REST/API behavior to design
Endpoint: `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture`. Required headers: `Authorization`, `Idempotency-Key`, stale `If-Match`, `Content-Type: application/json`. Expected: `412 Precondition Failed`, `application/problem+json`, no PSP call, no mutation, no new history entry.

### Important HTTP concepts
ETag, If-Match, `412`, lost update, mutation ordering.

### Security/quality risk
Stale updates can capture based on old state or amount assumptions.

### Proposed implementation idea
Compare `If-Match` against current version before domain mutation unless exact idempotent replay applies.

### Future Rest Assured test idea
Use stale ETag after a version-changing action and assert `412` plus unchanged status.

## Case H5 - Double capture race has one winning mutation

### Business situation
Two capture requests arrive nearly at the same time for the same authorized payment.

### REST/API behavior to design
Endpoint: `POST /capture` with same starting `If-Match` but different idempotency keys. Expected: only one capture succeeds with `200 OK`; the other returns `412 Precondition Failed` or domain `422` based on timing, and total captured amount is not duplicated.

### Important HTTP concepts
Concurrency, ETag, idempotency, optimistic locking, persistence oracle.

### Security/quality risk
Race conditions can double-capture or produce inconsistent history.

### Proposed implementation idea
Use version preconditions and database optimistic locking; keep service transaction boundaries small.

### Future Rest Assured test idea
Later concurrency test can issue two captures and assert only one status/history mutation wins.

## Case H6 - Cancel after capture is rejected

### Business situation
An operator tries to cancel a payment that has already been captured.

### REST/API behavior to design
Endpoint: `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel`. Required headers: `Authorization`, `Idempotency-Key`, current `If-Match`, `Content-Type: application/json`. Expected: `422 Unprocessable Entity`, `application/problem+json`, code `INVALID_TRANSITION`, unchanged `CAPTURED` state.

### Important HTTP concepts
Domain lifecycle, `422`, ETag, idempotency.

### Security/quality risk
Invalid lifecycle transitions can misstate financial state.

### Proposed implementation idea
Keep transition rules in `PaymentOrder` domain, not the controller.

### Future Rest Assured test idea
Authorize and capture, then cancel with current ETag and assert `422`.

## Case H7 - Refund after cancel is rejected

### Business situation
An operator attempts refund on a cancelled payment where no capture exists.

### REST/API behavior to design
Endpoint: `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund`. Required headers: `Authorization`, `Idempotency-Key`, `If-Match`, `Content-Type: application/json`. Expected: `422 Unprocessable Entity`, problem code `INVALID_TRANSITION` or `REFUND_AMOUNT_EXCEEDS_CAPTURED` per domain rule, unchanged state.

### Important HTTP concepts
Lifecycle state machine, `422`, problem details, persistence oracle.

### Security/quality risk
Refunding a non-captured or cancelled order would create impossible payment accounting.

### Proposed implementation idea
Keep refund preconditions in the domain entity and expose stable problem code mapping.

### Future Rest Assured test idea
Cancel a created order, then attempt refund and assert `422` plus unchanged history.

## Category I - Auth / JWT / OAuth Negative Cases

## Case I1 - Expired token returns 401 and WWW-Authenticate

### Business situation
A browser session or API client uses an expired access token.

### REST/API behavior to design
Endpoint family: any protected payment endpoint. Expected: `401 Unauthorized`, `WWW-Authenticate` header from resource server, no payment problem body requirement unless customized globally, no business mutation.

### Important HTTP concepts
JWT expiry, `401`, `WWW-Authenticate`, authentication versus authorization.

### Security/quality risk
Expired tokens must not be treated as denied business roles or allowed stale sessions.

### Proposed implementation idea
Use Spring Security resource server validation and avoid controller-level token parsing before authentication.

### Future Rest Assured test idea
Use expired JWT against payment detail and assert `401` plus `WWW-Authenticate`.

## Case I2 - Token without merchant_id returns 403 or masked 404

### Business situation
A merchant-scoped role token lacks the `merchant_id` claim.

### REST/API behavior to design
Endpoint: tenant-bound payment reads and mutations. Expected: documented result, preferably `403 Forbidden` for missing ownership claim or masked `404` where existence hiding is required. Response must not expose claims or token contents.

### Important HTTP concepts
JWT claims, ownership, `403`, masked `404`, problem details.

### Security/quality risk
Role without ownership claim can accidentally become global merchant access.

### Proposed implementation idea
Require `merchant_id` for all merchant authorities unless platform authority is present.

### Future Rest Assured test idea
Use merchant reader token without `merchant_id` and assert no access to detail/list.

## Case I3 - Token merchant_id mismatch with path merchantId

### Business situation
A Merchant A token calls a Merchant B path.

### REST/API behavior to design
Endpoint family: create, read, list, summary, history, lifecycle, metadata. Expected: consistent documented result. Create/list/summary may return `403`; detail/history may mask as `404`; mutations must not run. All payment errors should include no-store and correlation ID.

### Important HTTP concepts
Path ownership, JWT claim matching, authorization, cache.

### Security/quality risk
Path-claim mismatch is the core merchant tenant isolation failure mode.

### Proposed implementation idea
Extract a shared payment actor/ownership policy to reduce endpoint drift.

### Future Rest Assured test idea
Run one mismatch matrix across endpoint families and assert documented status per family.

## Case I4 - Invalid audience or issuer as a future topic

### Business situation
A token is validly signed by a different issuer or for a different audience.

### REST/API behavior to design
Endpoint family: protected `/api/**`. Expected: `401 Unauthorized` before controller invocation, `WWW-Authenticate`, no business mutation. Audience validation must be implemented only when the project decides the expected audience claim.

### Important HTTP concepts
OIDC issuer, JWT audience, resource server validation, `401`.

### Security/quality risk
Accepting tokens from the wrong issuer/audience can allow unintended clients or realms.

### Proposed implementation idea
Keep issuer validation configured; define audience validation later as a dedicated security configuration task.

### Future Rest Assured test idea
Use invalid issuer token and later invalid audience token; assert `401` and no controller problem body dependency.

## Category J - Spring Boot / OpenAPI Exposure

## Case J1 - Actuator exposure policy is explicit

### Business situation
The project may later add Spring Boot Actuator for health and diagnostics.

### REST/API behavior to design
Endpoint family: `/actuator/**` if dependency is added later. Default production-like policy: not exposed or expose only safe health info. Payment APIs remain under `/api/**`. Unauthorized actuator endpoints should not reveal beans, env, mappings, or config properties.

### Important HTTP concepts
Management endpoints, exposure allow-list, `404` or `401/403`, data minimization.

### Security/quality risk
Actuator misconfiguration can expose environment, mappings, heap, metrics, or shutdown controls.

### Proposed implementation idea
Do not add Actuator for this feature. If added later, specify `management.endpoints.web.exposure.include` narrowly and test it.

### Future Rest Assured test idea
Assert `/actuator/env` is not exposed in local/test policy unless explicitly enabled for a lesson.

## Case J2 - OpenAPI exposure policy is explicit

### Business situation
The team may later add OpenAPI/Swagger for API discovery.

### REST/API behavior to design
Endpoint family: `/v3/api-docs`, `/swagger-ui/**` if dependency is added later. Dev profile may expose docs. Production-like profile should disable or protect docs. Docs must describe public REST contracts without leaking internal package names or admin-only endpoints accidentally.

### Important HTTP concepts
OpenAPI, profile-specific exposure, documentation contract, authorization.

### Security/quality risk
API documentation can reveal hidden endpoints and internal models.

### Proposed implementation idea
Do not add springdoc in this planning phase. Add an exposure policy before introducing the dependency.

### Future Rest Assured test idea
Assert docs are disabled/protected under the selected profile and available only where intended.

## Case J3 - Swagger/OpenAPI does not expose internal/admin endpoints unexpectedly

### Business situation
If OpenAPI is added, generated docs may include endpoints intended only for local admin or internal development.

### REST/API behavior to design
Endpoint family: generated docs. Expected: only documented public `/api/status`, merchant, and payment endpoints appear unless admin/internal endpoints are explicitly in scope. No `.internal` package model names should become part of public contract.

### Important HTTP concepts
API discovery, DTO naming, endpoint filtering, documentation as contract.

### Security/quality risk
Docs can make accidental endpoints visible and stable to consumers.

### Proposed implementation idea
Use grouped OpenAPI configuration or annotations only after endpoint exposure policy is written.

### Future Rest Assured test idea
Fetch OpenAPI JSON in dev profile and assert expected paths exist and forbidden internal paths are absent.

## Case J4 - Error responses do not leak stack traces

### Business situation
A malformed request causes JSON parsing or validation failure.

### REST/API behavior to design
Endpoint family: payment endpoints. Expected: `application/problem+json` with safe fields only. Body must not include Java exception class names, stack traces, SQL, raw request bodies, raw tokens, or internal file paths.

### Important HTTP concepts
Problem details, error sanitization, content type, correlation ID.

### Security/quality risk
Verbose errors leak implementation details and secrets.

### Proposed implementation idea
Keep payment exception mapping explicit and avoid default error pages for payment controllers.

### Future Rest Assured test idea
Send malformed JSON and assert safe problem fields plus absence of `Exception`, `at lab.`, and raw body values.

## Category K - Evidence Hygiene

## Case K1 - Rest Assured logs redact Authorization and Cookie

### Business situation
A future Rest Assured test fails and logs request/response details.

### REST/API behavior to design
No production endpoint change. Test support should log enough to debug status, headers, and body while redacting `Authorization`, `Cookie`, `Set-Cookie`, access tokens, refresh tokens, and raw bearer values.

### Important HTTP concepts
Evidence hygiene, sensitive headers, failure logging, reproducibility.

### Security/quality risk
Test logs can leak tokens or session cookies into CI artifacts or shared notes.

### Proposed implementation idea
Later replace basic `RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()` with a redacting filter/helper in test support.

### Future Rest Assured test idea
Unit-test or characterize the redaction helper separately; do not print real tokens in normal scenario output.

## Case K2 - X-Correlation-ID is included in bug reports

### Business situation
A tester reports a failed capture or stale `If-Match` response.

### REST/API behavior to design
Endpoint family: all payment success and error responses should include `X-Correlation-ID`; payment problem body `correlationId` should match the header. Test evidence should include method, path, status, problem code, and correlation ID.

### Important HTTP concepts
Correlation ID, problem details, observability contract.

### Security/quality risk
Without correlation IDs, developers cannot connect test failures to logs/history.

### Proposed implementation idea
Keep `CorrelationIdFilter` and payment error body aligned; later create a bug-report evidence template.

### Future Rest Assured test idea
Assert supplied correlation ID is echoed in both header and problem body for a negative case.

## Case K3 - Expected versus actual response snapshots are safe

### Business situation
A future lesson compares expected and actual payment problem responses.

### REST/API behavior to design
No production endpoint change. Evidence should include sanitized snapshots of status, selected headers, and JSON body. It should exclude raw JWTs, cookies, full request body when it contains sensitive metadata, and private Keycloak material.

### Important HTTP concepts
Response snapshot, headers, problem details, redaction.

### Security/quality risk
Useful test artifacts can accidentally become sensitive records.

### Proposed implementation idea
Create a test evidence helper or vault template after implementation, not in this planning phase.

### Future Rest Assured test idea
For a failing validation scenario, capture a sanitized expected/actual diff in assertion messages or notes.

## Case K4 - Safe reproduction steps

### Business situation
A tester documents how to reproduce a tenant isolation or stale ETag issue.

### REST/API behavior to design
No production endpoint change. Reproduction steps should use local UUIDs, fake merchant references, generated idempotency keys, and sanitized tokens. Steps should include exact method, endpoint, required headers, expected status, expected problem code, and correlation ID.

### Important HTTP concepts
Reproducibility, endpoint contract, headers, evidence hygiene.

### Security/quality risk
Poor reproduction steps can become exploit instructions or leak real credentials.

### Proposed implementation idea
Add safe bug-report examples to vault lessons and keep them local-lab only.

### Future Rest Assured test idea
After tests exist, generate short local-only reproduction notes from test names and assertions.
