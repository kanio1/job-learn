---
name: epic-e10-payment-rule-configurator
parent: playwright-ops-wave-2
epic: E10
tasks: [PW-OPS-T16, PW-OPS-T17]
last_updated: 2026-08-20
---

# Epic E10 — Payment Rule Configurator (#18)

**Cel produktowy:** tenant admin ustawia auto-capture, próg risk, refund policy.  
**Cel dydaktyczny:** dynamic form, BVA, decision table, keyboard slider, ARIA disabled.

**Gate:** tenant settings ETag już jest (`PATCH /api/tenants/current/settings`). Rozszerzyć JSON, nie nowy CRUD bez If-Match.

Nuxt UI: `USwitch`, `UInputNumber`, `USlider`, `URadioGroup`, `UAccordion`.  
POM: `RuleConfiguratorComponent`.

To **nie** jest silnik reguł PSP. Policy jest czytana przy capture **w labie** jako: auto-capture OFF → UI/API informacyjne + opcjonalnie reject auto path. **Minimalna** egzekucja: GET policy na create/capture decision — jeśli autoCapture false, nie ma auto POST capture (nie dodajemy schedulera). AC API: PATCH policy 200; capture nadal ręczny. Egzekucja auto-capture = **P2** (może zostać dokumentacyjna: „policy stored, not executed by a worker”).

---

## Story E10-S1 — Policy persistence + ETag

**Task:** `PW-OPS-T16` · P0

### Jako / chcę / aby

Jako tenant admin zapisuję politykę i drugi admin dostaje 412 na stale settings_version.

### Business case

`BC-OPS-18` — Tenant payment policy. Reuse `settings_version` / If-Match tenant settings.

### Use case

`UC-OPS-34` — PATCH settings body zawiera `paymentPolicy`.

### SQL

[01-infra B.6](../01-infra-postgres-keycloak-stack.md) V36.

### Domain rules

```text
autoCapture: boolean
maxAutoCaptureMinor: integer >= 0, required if autoCapture true, ignored if false
riskThreshold: 0..100 inclusive
refundPolicy: MANUAL | AUTOMATIC
```

- autoCapture false → maxAutoCaptureMinor może być 0; UI disabled.
- AUTOMATIC refund **nie** wyłącza dual-control PIN w E5 (lab: AUTOMATIC = skip dual-control **tylko** ≤ próg PIN; high-value i tak PIN). Udokumentować decision table.
- Authority: istniejące `platform:tenant:settings:update`.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| RA-OPS-180 | RA | PATCH policy 200 nowy ETag |
| RA-OPS-181 | RA | stale If-Match 412 |
| RA-OPS-182 | RA | riskThreshold -1 / 101 400 |
| RA-OPS-183 | RA | autoCapture true + missing max 400 |
| RA-OPS-184 | RA | readonly 403 |
| RA-OPS-185 | RA | tenant isolation |

---

## Story E10-S2 — BVA + conditional UI

**Task:** `PW-OPS-T17` · P0

### Use case

`UC-OPS-35`

```text
threshold: 0 boundary, 1, 99, 100 boundary
Auto capture OFF → amount input disabled / aria-disabled
Auto capture ON  → amount required
```

Decision table (skrót):

| autoCapture | maxMinor | expected |
|---|---|---|
| OFF | any | 200; amount control disabled |
| ON | 0 | 400 or 200 depending min — **min 1 when ON** |
| ON | 1 | 200 |
| ON | omitted | 400 |

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-180 | E2E | OFF → InputNumber disabled |
| PW-OPS-E2E-181 | E2E | ON → required; submit empty 400/UI error |
| PW-OPS-E2E-182 | E2E | slider 0 and 100 saved |

---

## Story E10-S3 — Keyboard slider

**Task:** `PW-OPS-T17` · P0

### Acceptance criteria

- [ ] Focus slider: ArrowRight/Left, Home, End.
- [ ] ARIA `valuenow` / `valuemin` / `valuemax`.
- [ ] Reduced-motion: bez wymogu animacji.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-183 | E2E | Home → 0; End → 100 |
| PW-OPS-E2E-184 | E2E | RadioGroup Manual vs Automatic keyboard |

### Learning

Playwright + BVA + decision table + ARIA. Nie hardcode pixel pozycji slidera — `aria-valuenow`.
