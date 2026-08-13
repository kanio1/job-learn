---
name: epic-e0-foundation
parent: browser-session-visual-network-lab
epic: E0
tasks: [MRL-T01]
last_updated: 2026-08-13
---

# Epic E0 — Foundation & Guardrails

**Cel produktowy:** jeden hub labów lustrzanych, flagi, brak mobile, brak wycieku do prod.  
**Cel dydaktyczny:** feature flags, trzy światy tożsamości, rozdział mocked vs POM.

**Połączenia:** Error Lab, Checkout Lab hub `/admin/checkout-lab`, `app.checkout-lab.enabled`.

---

## Story E0-S1 — Hub + flagi  
**Task:** `MRL-T01` · P0

### Jako / chcę / aby
Jako uczeń SDET chcę stronę `/admin/mirror-lab` z kartami Session / Visual / Network / PayU mirrors / Bank-like, aby wejść w laby bez szukania URL-i.

### Acceptance criteria
- [ ] Strona hub (Nuxt) z `data-testid` per karta.
- [ ] Flagi: `app.session-lab.enabled`, `app.visual-lab.enabled`, `app.network-lab.enabled` (lub jedna `app.mirror-lab.enabled` + podflagi). Default `false`; `dev`/`test` mogą włączyć.
- [ ] Nav link tylko gdy flaga on (wzorzec checkout-lab).
- [ ] Brak breakpointów mobile-first jako AC; layout desktop dashboard (Nuxt UI).
- [ ] Copy wyjaśnia trzy światy (A dashboard / B hosted / C machine).
- [ ] Off → 404 na lab routes (BFF + UI).

### Learning
- `MOD:` flaga jak testing/CPL.
- `KC:` hub **nie** wymaga nowego realm.
- `PW:` `getByRole` / `data-testid`; zero `waitForTimeout`.

### Połączone z
- `apps/frontend/app/pages/admin/checkout-lab/index.vue`
- `apps/frontend/app/pages/error-lab.vue`
- C-02, C-03
