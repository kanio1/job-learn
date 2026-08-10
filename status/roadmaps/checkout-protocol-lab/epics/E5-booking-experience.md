---
name: epic-e5-booking-experience
parent: checkout-protocol-lab
epic: E5
tasks: [CPL-T25, CPL-T26]
last_updated: 2026-08-09
---

# Epic E5 — Booking Experience (Bookero-like)

**Cel produktowy:** cienka warstwa „rezerwacja cash vs online”, żeby protokół miał sens produktowy.  
**Cel dydaktyczny:** ten sam fulfillment SM; online odpala CPL; cash omija PSP.

**Połączenia:** E1 create session; E3 fulfillment; Error Lab jako hub; Keycloak JWT **opcjonalnie** dla strony dashboard (jak inne admin pages).

---

## Story E5-S1 — Booking Lab page  
**Task:** `CPL-T25` · P1 · FR-12

### Jako / chcę / aby
Jako recepcja Bookero-like chcę utworzyć rezerwację cash lub online.

### Acceptance criteria
- [ ] Form: kwota, currency, tryb `CASH` | `ONLINE`, continueUrl, czas na płatność.
- [ ] `CASH` → fulfillment `CONFIRMED` natychmiast **bez** session PSP (lub session oznaczona paid-offline — udokumentuj jedną ścieżkę).
- [ ] `ONLINE` → wywołanie create session (E1) + pokazanie / otwarcie `redirectUri`.
- [ ] Widok statusu rezerwacji czyta **fulfillment**, nie return hint.
- [ ] Flaga off → strona niedostępna.

### Learning
- `REST:` dwa sposoby dojścia do CONFIRMED (cash vs event).
- `HTTP:` online path ćwiczy pełny redirect chain.
- `KC:` jeśli strona za auth middleware — użyj istniejących ról dashboard; **bez nowych ról realm**.
- `PW:` jedna ścieżka cash (szybka) + jedna online (pełna).

### Połączone z
- E3-S3
- E1-S2
- Nuxt auth middleware (wyjątki jak F-D2 tylko dla hosted; Booking zwykle chroniony)

---

## Story E5-S2 — Nav entry under Error Lab / Learning  
**Task:** `CPL-T26` · P2

### Jako / chcę / aby
Jako learner chcę wejście z dashboardu.

### Acceptance criteria
- [ ] Link z `error-lab.vue` (obok psp-redirect-trigger) gdy lab enabled.
- [ ] Ukryty / 404 gdy disabled.
- [ ] `data-testid` dla PW.

### Learning
- `PW:` discovery surface jak inne Error Lab demos.
- `KC:` ta sama sesja dashboard.

### Połączone z
- `apps/frontend/app/pages/error-lab.vue`
- F-D2 trigger pattern
