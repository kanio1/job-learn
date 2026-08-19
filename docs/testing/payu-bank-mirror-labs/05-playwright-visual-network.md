# 05 — Visual comparisons i network interception

Visual: Playwright [test snapshots](https://playwright.dev/docs/test-snapshots).  
Network: [Playwright network](https://playwright.dev/docs/network).

## Visual — definicja operacyjna

1. Golden PNG w `tests/e2e/visual-lab.spec.ts-snapshots/` (nazwa z testid).
2. `toHaveScreenshot` na locatorze kafelka, nie na żywych listach.
3. Próg globalny `maxDiffPixelRatio: 0.02` — nie podnosić żeby „przeszło”.
4. Dynamic: `stylePath: 'tests/e2e/visual-lab-mask.css'` lub `mask`.
5. Update: `--update-snapshots` + review git.
6. Środowisko = CI Playwright image (C-06).
7. `@visual-negative` wyłączone z default CI (`grepInvert`).

F-D5 (`visual-regression.spec.ts`) zostaje analogiem badge poza Visual Lab.

| ID | Cel | Pokrycie | FR | Prio |
|---|---|---|---|---|
| PW-MRL-VIS-001 | Merchant badge tile | existing-pom | V02 | P0 |
| PW-MRL-VIS-002 | Payment badge tile | existing-pom | V02 | P0 |
| PW-MRL-VIS-003 | Problem Details tile | existing-pom | V02 | P0 |
| PW-MRL-VIS-004 | Hosted CTA tile | existing-pom | V02 | P0 |
| PW-MRL-VIS-005 | Idle lock tile (statyczny copy, bez ticking seconds) | existing-pom | V02 | P0 |
| PW-MRL-VIS-006 | Expired tile | existing-pom | V02 | P0 |
| PW-MRL-VIS-010 | Full page + hide `[data-dynamic]` | designed | V04 | P1 |
| PW-MRL-VIS-020 | Dark CTA / `visual-tile-dark` | existing-pom | V03 | P1 |
| PW-MRL-VIS-021 | `colorScheme: 'dark'` na całym visual-lab | designed | V03 | P2 |
| PW-MRL-VIS-030 | ARIA snapshot tego samego CTA | designed | — | P2 |
| PW-MRL-VIS-040 | Break visual (dokumentowany fail) | existing-pom tagged | V05 | P1 |
| PW-MRL-VIS-090 | Anti-case: lista płatności z UUID | docs-only | V06 | P0 |

Idle lock **overlay** (prawdziwy, nie kafelek) **nie** jest goldenem — zmienia się TTL; UC clock w 03.

---

## Network interception

### Mocked (`tests/e2e`) — fulfill dozwolony

| ID | Technika | Cel | Pokrycie | FR | Prio |
|---|---|---|---|---|---|
| PW-MRL-NET-001 | stateful fulfill 503→200 | retry UI | designed | N01 | P0 |
| PW-MRL-NET-002 | `route.abort('timedout')` na `/slow` | ErrorState | designed | N02 | P0 |
| PW-MRL-NET-003 | `route.fetch` + body `success` na lie | UI kłamie | designed | N03 | P0 |
| PW-MRL-NET-004 | `continue` strip `Idempotency-Key` | 428/400 na **payment** create (nie MRL bank) | designed | N04 | P1 |
| PW-MRL-NET-005 | `continue` strip Cookie | BFF 401 | designed | N04 | P1 |
| PW-MRL-NET-006 | `routeFromHAR` `network-lab.har` | replay bez Cookie/Authorization | designed | N05 | P1 |
| PW-MRL-NET-007 | glob `**/*.{png,jpg}` abort | nie blokuje lab JSON | designed | — | P2 |
| PW-MRL-NET-008 | fulfill PDF `%PDF-1.4` na statements | magic bytes download | designed | B02 | P0 |
| PW-MRL-NET-009 | fulfill CSV | filename statement | designed | B02 | P1 |
| PW-MRL-NET-010 | widget: mock hosted GET + simulate w iframe | frameLocator Approve | designed | P05 | P1 |

### Live POM — fulfill zakazany

| ID | Technika | Cel | Pokrycie | FR | Prio |
|---|---|---|---|---|---|
| PW-MRL-NET-100 | `waitForResponse` 503 then 200 | żywy counter Nitro | existing-pom | N01 | P0 |
| PW-MRL-NET-101 | `waitForRequest` Idempotency-Key na CPL booking | analog CPL | existing analog | N04 | P1 |
| PW-MRL-NET-102 | `context.setOffline(true)` | ErrorState | existing-pom | N02 | P1 |
| PW-MRL-NET-103 | `recordHar` **gitignored** | debug | designed | N05 | P2 |
| PW-MRL-NET-104 | console guard no token | storage-safety | existing-pom | S08 | P0 |
| PW-MRL-NET-105 | `waitForResponse` csrf-demo 403 | existing-pom | S06 | P1 |
| PW-MRL-NET-106 | download PDF live, readFile magic | existing-pom | B02 | P1 |
| PW-MRL-NET-107 | `waitForRequest` TPP **bez** `?token=` (jest header) | designed | B05 | P1 |

### HAR rules

- `urlFilter` wąski: `**/api/network-lab/har-replay`
- Redact `Authorization`, `Cookie`, `Set-Cookie`
- Nie commitować live HAR
- Fixture tylko zsyntetyzowany (`source: har`)

### Service workers

Jeśli `page.route` „nie widzi” requestów: `serviceWorkers: 'block'` w context lab. Nie wprowadzać MSW.

### Contrasty

| Zdarzenie | Mocked | POM |
|---|---|---|
| 503 retry | fulfill sekwencja | dwa prawdziwe POST |
| Lie | fetch+override body | live lie endpoint (JSON `success` ≠ persistence) |
| PDF | fulfill Buffer | download z BFF arrayBuffer (`%PDF-`) |
| Widget | mock hosted w ramce | live session + `contentFrame` `psp-outcome` + CONFIRMED |
| Offline | — | `error-state` (nie lie success) |
