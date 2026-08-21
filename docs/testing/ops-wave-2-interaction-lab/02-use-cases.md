# 02 — Use cases

Format: aktor, trigger, sukces, błąd, oracle. Wszystkie na żywym BFF.

| ID | BC | Aktor | Główna ścieżka | Alternatywa / błąd | Oracle | Testy |
|---|---|---|---|---|---|---|
| UC-OPS-00 | 00 | Dev | `--app`, status 200, issuer | zły session password | curl / script | manual |
| UC-OPS-13 | 13 | platform.admin | PATCH merchant If-Match | 428 brak, 412 stale | ETag+DB | RA-OPS-050…055 |
| UC-OPS-14 | 13 | admin + operator | A save v8, B save v7 | 412 modal | dwa context | PW-OPS-SEC-020 |
| UC-OPS-15 | 13 | platform.operator | tabs Your/Latest | Escape bez PATCH | dialog | E2E-130…132 |
| UC-OPS-16 | 16 | platform.admin | dirty → Back Stay | Discard → list | URL; PATCH=0 | E2E-160…162 |
| UC-OPS-17 | 16 | platform.admin | beforeunload dismiss | accept leave | dialog type | E2E-163 |
| UC-OPS-11 | 11 | support.agent | POST case NEW | illegal skip 409 | DB status | RA-OPS-110…122 |
| UC-OPS-12 | 11 | support.agent | tab Work Queue | empty column | regions | E2E-110 |
| UC-OPS-18 | 11 | support.agent | Move/drag IN_PROGRESS | reload still there | PATCH 200 | E2E-110/111 |
| UC-OPS-19 | 11 | agent + operator | 412 rollback | toast | karta źródło | E2E-112 |
| UC-OPS-20 | 15 | support.agent | bulk-assign mixed | 200 succeeded/failed | modal rows | E2E-150, RA-150 |
| UC-OPS-21 | 15 | support.agent | Retry failed only ids | — | request body | API-031, E2E-152 |
| UC-OPS-22 | 17 | platform checker | challenge+verify PIN | 400 pin_not_required | hash≠pin | RA-170…179 |
| UC-OPS-23 | 17 | platform checker | pressSequentially / paste | wrong pin | modal | E2E-170…174 |
| UC-OPS-24 | 12 | platform.admin | WS connected | 401 disconnect | chip | T10 |
| UC-OPS-25 | 12 | manager + observer | capture → feed row | — | framereceived | E2E-120 |
| UC-OPS-26 | 12 | platform.admin | inject dup/OOO/bad | toast malformed | jeden wiersz | E2E-121…123 |
| UC-OPS-27 | 19 | support.agent | badge 3→4→3 persist | BOLA 404 | unread | E2E-190…193 |
| UC-OPS-28 | 14 | merchant.manager | save view localStorage | token absent | storage-safety | E2E-140/141 |
| UC-OPS-29 | 14 | manager vs operator | API views | other user empty | GET me | E2E-142/143, RA-140 |
| UC-OPS-30 | 14 | merchant.manager | view ↔ URL ↔ columns | Back | query | E2E-144…147 |
| UC-OPS-31 | 20 | platform.admin | Ctrl+K Enter merchant | empty q | search GET | E2E-200/201 |
| UC-OPS-32 | 20 | platform.admin | type race last q | stale results | last response | E2E-202 |
| UC-OPS-33 | 20 | merchant.manager | no MERCHANTS group | own payments | palette | SEC-040 |
| UC-OPS-34 | 18 | tenant.admin | PATCH paymentPolicy | 412 stale | ETag | RA-180…185 |
| UC-OPS-35 | 18 | tenant.admin | autoCapture OFF disabled | BVA 0/100 | aria | E2E-180…182 |
| UC-OPS-36 | 21 | platform.admin | LocaleSelect PL | persist reload | Intl regex | E2E-210…213 |
| UC-OPS-37 | 22 | support.agent | carousel next | keyboard | index | E2E-220/221 |
| UC-OPS-38 | 22 | support.agent | invalid evidence | 404 slide | problem | E2E-223, API-070 |

Happy-path interview: UC-14 → UC-18 → UC-19 → UC-20 → UC-16 → UC-23 → UC-25 → UC-26.  
Potem (P1): UC-34 → UC-35 → UC-36 → UC-37.

Flow: [00-business-flows.md](00-business-flows.md). Curriculum: [value-and-learning](../m360-ops-wave-2-value-and-learning.md).

### Czego uczą testy przypięte do UC

| UC | E2E uczy | REST (BFF/RA) uczy |
|---|---|---|
| UC-OPS-13–15 | dwa contexty; tabs Your/Latest | 200/412/428; JSONB/merchant version unchanged |
| UC-OPS-16/17 | Stay/Discard/`beforeunload` | PATCH length 0 |
| UC-OPS-11/18/19 | Move + 412 rollback | maszyna statusów; illegal 409 |
| UC-OPS-20/21 | counts per item | retry `caseIds` = failed |
| UC-OPS-22/23 | `pressSequentially` / clock | hash ≠ pin; 429 |
| UC-OPS-25/26 | `framereceived` na żywym WS | inject 201; readonly 403 |
| UC-OPS-34/35 | BVA + `aria-valuenow` | V36 JSONB; isolation `/current` |
| UC-OPS-36 | `test.use({ locale })` + Intl | — (brak RA i18n) |
| UC-OPS-37/38 | carousel klawiatura; 404 slide | API-070 problem+json |
