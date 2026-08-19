# 01 — POM remaining ranked gaps (Slice 2)

**What to build:** Users assign-roles + manager GET `/api/users` 403≠502, `merchant.denied` BFF/UI, ARIA `/forbidden`, CSRF merchant contrast, CORS OPTIONS, Error Lab 304 seed, leftover catalog retag.

**Blocked by:** None — live `--app` stack.

**Seams:** Playwright E2E + BFF (`tests-pom`)

**Status:** done

**Category:** enhancement

- [x] Users assign-roles UI + listUsers oracle
- [x] Manager GET `/api/users` 403 problem+json, not 502
- [x] `merchant.denied` storageState + GET merchants 403 + UI deny
- [x] ARIA snapshot `forbidden-page` (default suite, not visual)
- [x] POST `/api/merchants` without CSRF is 201
- [x] OPTIONS cors-cookie 204 + GET evil Origin
- [x] Error Lab 304 seeds a payment order first
- [x] Catalog leftover retag (EP-W2-015, UC-W2-18, SEC-031, GAP-T03, FR-S04b/S06, 08 denied/ARIA)
- [x] Playwright green gate for listed specs + default POM regression + lint
