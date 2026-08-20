---
name: epic-e0-foundation
parent: playwright-ops-wave-2
epic: E0
tasks: [PW-OPS-T00]
last_updated: 2026-08-20
---

# Epic E0 — Foundation & guardrails (docs only)

**Cel produktowy:** jedna oś ID, stos, non-goals, zanim ktokolwiek ruszy Flyway V31.  
**Cel dydaktyczny:** drugi milestone-pw obok Merchant 360; zero mocków HTTP i WS.

**Połączenia:** [00-context](../00-context-requirements.md), [01-infra](../01-infra-postgres-keycloak-stack.md), [docs/testing/ops-wave-2-interaction-lab](../../../docs/testing/ops-wave-2-interaction-lab/).

---

## Story E0-S1 — Rejestr ID i stos

**Task:** `PW-OPS-T00` · P0

### Jako / chcę / aby

Jako tech lead chcę spójne prefiksy BC/UC/RA/PW-OPS i runbook Podman, aby fale 1+ nie mieszały `routeWebSocket` z żywym Keycloak.

### Business case

`BC-OPS-00` — Zespół uczy się TS/Playwright na tym samym deployu co operator (`--app`), nie na mocku ramek WS.

### Use case

`UC-OPS-00` — Operator odpala `scripts/dev-stack.sh --app`, sprawdza `/api/status` i issuer Keycloak, potem POM.

### Acceptance criteria

- [x] Katalog `status/roadmaps/playwright-ops-wave-2/` + catalog testów.
- [x] Non-goals: brak `page.route` / `routeWebSocket`, 412 nie 409 na ETag, brak payment Kanban (to M360), brak fake KPI.
- [x] Flyway zarezerwowane V31+ (M360 V23–V30).
- [x] Stos udokumentowany: `--app` vs default vs `--full`.

### Test IDs

Brak automatów w E0. Oracle ręczny: runbook.

### Learning

- Różnica inspekcji WS (`page.on('websocket')`) vs mock (`routeWebSocket`) — [WebSocketRoute](https://playwright.dev/docs/api/class-websocketroute).
