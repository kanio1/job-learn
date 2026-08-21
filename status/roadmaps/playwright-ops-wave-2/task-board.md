---
name: playwright-ops-wave-2-tasks
origin: POST_KIRO_WORK
status: DESIGNED_NOT_STARTED
last_updated: 2026-08-20
---

# Task board — kolejność implementacji

Statusy: `OPEN` / `DONE`. Fala 1 (T01–T04, T20) zaimplementowana.

| ID | Fala | Epic | Treść | Testy (projektowane) | Status |
|---|---|---|---|---|---|
| PW-OPS-T00 | 0 | E0 | Ten katalog + research + catalog BC/UC | — | DONE (docs) |
| PW-OPS-T01 | 1 | E1 | Flyway V31 merchant contact_phone/address | RA-OPS-001 (IT schema) | DONE |
| PW-OPS-T02 | 1 | E1 | GET/PATCH merchant ETag If-Match 412/428 (skip jeśli M360 T13 już w kodzie) | RA-OPS-050…055 | DONE |
| PW-OPS-T03 | 1 | E1 | Conflict UI UTabs Your/Latest + BFF Zod | PW-OPS-SEC-020, PW-OPS-E2E-130 | DONE |
| PW-OPS-T04 | 1 | E2 | Unsaved guard NuxtLink/Back/beforeunload | PW-OPS-E2E-160…164 | DONE |
| PW-OPS-T05 | 2 | E3 | Modulith `support` + Flyway V32 cases + REST | RA-OPS-110…118 | DONE |
| PW-OPS-T06 | 2 | E3 | PATCH case status + If-Match; illegal 409 | RA-OPS-119…122 | DONE |
| PW-OPS-T07 | 2 | E3 | Support Work Queue Kanban + menu Move + dragTo | PW-OPS-E2E-110…114 | DONE |
| PW-OPS-T08 | 3 | E4 | POST bulk-assign partial + Retry failed | PW-OPS-E2E-150…153, RA-OPS-150 | DONE |
| PW-OPS-T09 | 4 | E5 | V33 refund-challenges + UPinInput (∥ F2) | RA-OPS-170…179, PW-OPS-E2E-170…176 | DONE |
| PW-OPS-T10 | 5 | E6 | `ops` module + WS BFF proxy + inject | RA-OPS-125…127, PW-OPS-API-020 | DONE |
| PW-OPS-T11 | 5 | E6 | Overview Live Operations feed UI | PW-OPS-E2E-120…125 | DONE |
| PW-OPS-T12 | 5 | E7 | Notifications table + popover badge | PW-OPS-E2E-190…194, RA-OPS-190 | DONE |
| PW-OPS-T13 | 6 | E8 | Saved views localStorage then V35 API | PW-OPS-E2E-140…146, RA-OPS-140 | DONE |
| PW-OPS-T14 | 6 | E8 | Column profiles UCheckboxGroup ↔ URL | PW-OPS-E2E-147 | DONE |
| PW-OPS-T15 | 6 | E9 | UDashboardSearch live entities + RBAC + last-wins (skip core jeśli M360 T17) | PW-OPS-E2E-200…203, PW-OPS-SEC-040 | DONE |
| PW-OPS-T16 | 7 | E10 | V36 payment_policy JSON + ETag | RA-OPS-180…185 | DONE |
| PW-OPS-T17 | 7 | E10 | Rule configurator Switch/Slider/BVA/keyboard | PW-OPS-E2E-180…184 | DONE |
| PW-OPS-T18 | 8 | E11 | `@nuxtjs/i18n` + ULocaleSelect + locale project | PW-OPS-E2E-210…213 | DONE |
| PW-OPS-T19 | 9 | E12 | Evidence UCarousel / fallback gallery | PW-OPS-E2E-220…224 | DONE |
| PW-OPS-T20 | 1 | E1 | POM `ConflictDiffComponent` + `multi-user.fixture` | PW-OPS-SEC-020 | DONE |
| PW-OPS-T21 | 2 | E3 | POM `KanbanBoardComponent` / `SupportCaseCard` | PW-OPS-E2E-110 | DONE |
| PW-OPS-T22 | 5 | E6 | POM `OpsFeedComponent` + WS wait helper | PW-OPS-E2E-120 | DONE |

## Zależności

```text
T00
 └─ T01 → T02 → T03 → T20 → T04
 └─ T05 → T06 → T07 → T21 → T08
 └─ T09                          (∥ T05; dual-control V21 already exists)
 └─ T10 → T11 → T22 → T12        (payments already emit; inject for chaos)
 └─ T13 → T14
 └─ T15                          (after list APIs exist; coordinate M360 T17)
 └─ T16 → T17
 └─ T18                          (after POM component objects; locator strategy)
 └─ T19                          (evidence upload already exists)
```

Nie startować T03/T07/T11 zanim odpowiadające RA na Testcontainers **i** smoke `--app` nie przejdą.

T02: jeśli branch ma już merchant ETag z M360 T13, task = tylko PATCH contact fields + conflict DTO.

T15: jeśli M360 T17 live, task = RBAC groups + last-wins only.

T18: nie zaczynać bez zgody na zależność `@nuxtjs/i18n`.

T10: nie zaczynać bez zgody na `spring-boot-starter-websocket`.
