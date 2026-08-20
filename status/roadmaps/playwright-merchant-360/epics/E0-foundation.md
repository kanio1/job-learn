---
name: epic-e0-foundation
parent: playwright-merchant-360
epic: E0
tasks: [PW-M360-T00]
last_updated: 2026-08-20
---

# Epic E0 — Foundation & guardrails (docs only)

**Cel produktowy:** jedna oś ID, stos, non-goals, zanim ktokolwiek ruszy Flyway.  
**Cel dydaktyczny:** milestone-pw jak real-stack-learning; zero mocków.

**Połączenia:** [00-context](../00-context-requirements.md), [01-infra](../01-infra-postgres-keycloak-stack.md), [docs/testing/merchant-360-erp-lab](../../../docs/testing/merchant-360-erp-lab/).

---

## Story E0-S1 — Rejestr ID i stos

**Task:** `PW-M360-T00` · P0

### Jako / chcę / aby

Jako tech lead chcę spójne prefiksy BC/UC/RA/PW i runbook Podman, aby fale 1+ nie mieszały mocków z żywym Keycloak.

### Business case

`BC-M360-00` — Zespół uczy się TS/Playwright na tym samym deployu co operator (`--app`), nie na `route.fulfill`.

### Use case

`UC-M360-00` — Operator odpala `scripts/dev-stack.sh --app`, sprawdza `/api/status` i issuer Keycloak, potem POM.

### Acceptance criteria

- [x] Katalog `status/roadmaps/playwright-merchant-360/` + catalog testów (BC/UC/TC + BF/EP/DT/AT + plan `tests-pom`).
- [x] Non-goals: brak CRM module, brak `page.route`, 412 nie 409, brak fake Revenue.
- [x] Stos udokumentowany: `--app` vs default vs `--full`.

### Test IDs

Brak automatów w E0. Oracle ręczny: runbook.

### Learning

- Różnica monitorowania sieci (`waitForResponse`) vs mock (`page.route`) — [Playwright Network](https://playwright.dev/docs/network).
