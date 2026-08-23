# Milestone implement — generic slice-loop template

Stable prefix for milestone skills that implement one roadmap slice per user message
(pattern proven on `playwright-merchant-360` / `playwright-ops-wave-2`). Copy this file,
fill the `<placeholders>`, save next to it (e.g. `<milestone>-implement.md`), and register
a matching skill under `.agents/skills/` only when the milestone actually starts.

## Every turn

1. `.codex/<milestone>-slice.md` — Next task id only.
2. `status/roadmaps/<milestone>/task-board.md` — that row + dependencies.
3. Matching epic under `status/roadmaps/<milestone>/epics/`.
4. Layer files listed on that epic. Do not open all catalogs.
5. Layer skills by change type: `tdd` then `spring-modulith` / `nuxt-frontend` / `playwright-pom`; REST Assured design via `rest-api-test-design`.

## Non-negotiables (lab-wide)

- Live stack only (`scripts/dev-stack.sh --app`). No `page.route` / `route.fulfill` / HAR in `tests-pom`.
- Optimistic lock = **412** + `If-Match`. **409** = duplicate / idempotency / illegal transition only.
- Flyway owns schema; versions increase with implementation (confirm max before writing); no `CREATE INDEX CONCURRENTLY`; JPA `ddl-auto: validate`.
- Modulith: public API at module root; no `*.internal` leaks across modules.
- Unique test data per worker (`uniqueMerchantReference(testInfo)` style); never seed data as owner.
- No fake KPI/revenue, no Nuxt UI bump, no new Keycloak roles unless the epic says so.
- Skip `restkit/` and `paymentsupport/` suites. Do not edit `.kiro/**`.
- One task per user message. Stop when that slice's AC are green or blocked.
- Do not commit unless the user asks.

## Verify (scoped)

- Backend: from `apps/backend`, command starts with `./mvnw`, single class first (`./mvnw -Dtest=Class#method test`).
- Frontend TS: `corepack pnpm typecheck` && `corepack pnpm lint` from `apps/frontend` when Vue/TS changed.
- Live POM: only the spec named in the epic; passwords from env only.

## After the slice

Update **only**: `.codex/<milestone>-slice.md` and the matching task-board row.
Reply with: closed id, files, commands run, Next id. Then stop.

## Milestone state fields

| Field | Meaning |
|---|---|
| Milestone | roadmap directory name |
| Status | NOT_STARTED / IN_PROGRESS / COMPLETE |
| Last closed | last finished task id |
| Next | next task id or — |
| Locked | frozen decisions (Flyway versions, pins, contract choices) |

When the milestone reaches COMPLETE: mark it here and in the slice overlay, stop implementing,
and retire the milestone skill (delete dir + symlink) so the catalog stays small.
