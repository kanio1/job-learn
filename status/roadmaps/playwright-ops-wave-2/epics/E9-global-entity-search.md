---
name: epic-e9-global-entity-search
parent: playwright-ops-wave-2
epic: E9
tasks: [PW-OPS-T15]
last_updated: 2026-08-20
---

# Epic E9 — Global Entity Search (#20)

**Cel produktowy:** Ctrl+K znajduje merchanta / płatność / akcję, nie tylko statyczne linki.  
**Cel dydaktyczny:** shortcut, focus, grouped results, keyboard, authorization, last-wins na **żywym** GET.

**Nie** nowy widget — [UDashboardSearch](https://ui.nuxt.com/docs/components/dashboard-search) już w `layouts/dashboard.vue`. Fuse + `searchDelay` (4.7.0). Shortcut `meta_k`.

**Koordynacja M360 T17:** jeśli live entity search już jest, ten epik = tylko RBAC grup + race last-wins + payments group. Nie drugi Ctrl+K.

Nuxt UI: istniejący DashboardSearch / CommandPalette.

---

## Story E9-S1 — Live groups + keyboard

**Task:** `PW-OPS-T15` · P0

### Jako / chcę / aby

Jako admin wpisuję `acme` i strzałkami otwieram płatność.

### Business case

`BC-OPS-20` — Global search ERP. Inne ćwiczenie niż Support Assignment Autocomplete.

### Use case

`UC-OPS-31`

```text
Ctrl+K → focus input → type → ArrowDown → ArrowDown → Enter
```

Grupy: MERCHANTS, PAYMENTS, ACTIONS (permission-filtered, jak dziś static actions).

### Rules

- Query `GET /api/search?q=&limit=8` aggregacja BFF **albo** Spring read-model. Limit twardy.
- Last in-flight: abort/ignore stale (ostatni `waitForResponse` wygrywa). **Bez** `page.route`.
- Debounce = `searchDelay` DashboardSearch (100ms default) + AbortController po stronie composable.
- Enter na merchant → `/admin/merchants/{id}`; payment → detail.

### Acceptance criteria

- [ ] Istniejące command-palette.spec (Error Lab) nadal działa.
- [ ] `loading` prop palety przy fetch.
- [ ] Empty `U` empty slot gdy 0 encji.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-200 | E2E | Ctrl+K type unique ref → group Merchants → Enter navigates |
| PW-OPS-E2E-201 | E2E | ArrowDown dwa razy + Enter na payment |
| PW-OPS-API-060 | PW REST | GET search 200 limit |

---

## Story E9-S2 — Race last-wins

**Task:** `PW-OPS-T15` · P0

### Use case

`UC-OPS-32` — szybkie `ac` potem `acme-unique-zzz`. UI pokazuje wyniki **drugiego** q, nie pierwszego.

Producer: dwa unikalne merchanty; type slowly vs paste. Oracle: last response URL `q=` + widoczne labele.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-202 | E2E | last waitForResponse q= wins; first merchant absent |

---

## Story E9-S3 — RBAC groups

**Task:** `PW-OPS-T15` · P0

### Use case

`UC-OPS-33` — role A widzi merchant result; merchant.manager **nie** widzi grupy MERCHANTS (canReadMerchants=false); widzi PAYMENTS własne.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-SEC-040 | SEC | manager: group Merchants absent; own payment present |
| PW-OPS-SEC-041 | SEC | readonly: actions Create merchant absent |
| PW-OPS-E2E-203 | E2E | denied user 403/empty nie crash |

### Learning

Shortcut + Fuse + authz. Nie hardcode wyników w POM.
