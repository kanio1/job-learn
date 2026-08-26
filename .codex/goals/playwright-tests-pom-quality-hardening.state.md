# Playwright `tests-pom` quality hardening — current state

> MUTABLE AND SMALL. Replace values; do not append history here.

- Program: ACTIVE
- Current ticket: `09-final-validation-review-and-evidence`
- Current acceptance ID: T09-A08 TLS manager lifecycle repair
- Phase: TLS_LIFECYCLE_ETAG_VALIDATION
- Implementation: COMPLETE — versioned ETag responses emit `Cache-Control: no-transform`; BFF preserves `ETag`/`If-Match` opaquely.
- Backend validation: PASS — payment conditional 1/1, merchant 3/3, tenant settings 7/7, support REST 12/12.
- POM static validation: PASS — `corepack pnpm typecheck:pom`; `git diff --check`.
- TLS runtime: PASS — `chromium-tls-manager` 5/5, including the browser/Caddy lifecycle ETag chain.
- Next action: record the completed T09-A08/A11 closure in the ticket summary.
- Evidence: `status/evidence/playwright-tests-pom-quality-hardening.md`; decision: `.codex/adr/0003-versioned-rest-etag-no-transform.md`.
