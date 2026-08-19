# 01 — POM P0–P1 evidence slice

**What to build:** Align Playwright origin docs, close CSRF/revoke/Users live-POM gaps against as-built BFF, add Mirror lab flag-off overlay on :3012, retag catalogs that still say `designed` / `existing-pw` for those cases.

**Blocked by:** None — live `--app` stack.

**Seams:** Playwright E2E + BFF (`tests-pom`); compose overlay for Mirror flag-off

**Status:** done

**Category:** enhancement

- [x] Origin matrix: host DX `localhost` vs `--app` `127.0.0.1` (`test:e2e` / `test:e2e:app`)
- [x] CSRF wrong token 403, `mrl-csrf` in `document.cookie`, guest csrf-demo 401
- [x] Device revoke: POST 200, GET still 200, revoked id gone (not 401)
- [x] Users create + disable UI (TENANT_ALPHA, unique username)
- [x] Mirror flag-off :3012 nav/BFF 404
- [x] Catalog retag SEC-005/006, FR-OIDC, existing-pw, CPL PW-API-026
- [x] Playwright green gate
