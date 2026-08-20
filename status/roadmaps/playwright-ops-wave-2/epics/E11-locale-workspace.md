---
name: epic-e11-locale-workspace
parent: playwright-ops-wave-2
epic: E11
tasks: [PW-OPS-T18]
last_updated: 2026-08-20
---

# Epic E11 — Locale / Currency Workspace (#21)

**Cel produktowy:** operator wybiera EN / PL / SV; kwoty i daty się formatują.  
**Cel dydaktyczny:** Playwright `locale` / `timezoneId`; POM bez hardcode `"Save"`.

**Gate:** osobna **zgoda** na zależność `@nuxtjs/i18n`. `ULocaleSelect` w docs jest parą z i18n + `@nuxt/ui/locale`.

**Nie** pełne tłumaczenie dashboardu. 2–3 ekrany: login chrome opcjonalnie skip; **Merchants list**, **Payment detail**, **Support queue** labels + formatters.

Merchant 360 ma i18n w non-goals — ten milestone **wyjątek** świadomy.

---

## Story E11-S1 — LocaleSelect + persist

**Task:** `PW-OPS-T18` · P1

### Jako / chcę / aby

Jako operator w Szwecji widzę daty ISO-like i kwoty z przecinkiem.

### Business case

`BC-OPS-21` — ERP workspace locale. Browser `locale` ≠ user preference (user może nadpisać).

### Use case

`UC-OPS-36` — `ULocaleSelect` w dashboard navbar. Persist cookie albo `user_saved_views`-like `user_preferences.locale` (jeśli nowa kolumna — `iam` V37; albo cookie `pq-locale`). **Rekomendacja:** cookie + i18n `detectBrowserLanguage` false gdy cookie set.

### Formatting oracle (lab, EUR)

| Locale | Amount 123456 minor | Date 2026-08-20 |
|---|---|---|
| en-US | €1,234.56 | 8/20/2026 |
| pl-PL | 1 234,56 € | 20.08.2026 |
| sv-SE | 1 234,56 € | 2026-08-20 |

Użyć `Intl.NumberFormat` / `DateTimeFormat` z tagiem locale. Test **nie** łapie whitespace NBSP krucho — `toMatch(/1[\s\u00a0]234,56/)` dla PL.

### Playwright project

**Jeden** project `locale` w `playwright.pom.config.ts`, nie 3× cały suite:

```ts
{ name: 'locale', testMatch: /specs\/locale-.*\.spec.ts/, use: { /* per test.use */ } }
```

W spec:

```ts
test.use({ locale: 'pl-PL', timezoneId: 'Europe/Warsaw' });
```

[Emulation](https://playwright.dev/docs/emulation#locale--timezone): wpływa na `navigator.language`, `Accept-Language`.

### Acceptance criteria

- [ ] POM: `getByRole('button', { name: /save|zapisz|spara/i })` **albo** `data-testid="merchant-save"` — preferuj testid na mutate actions w i18n screens.
- [ ] Istniejące specy Chromium default **en** zostają zielone (nie tłumaczyć wszystkich stringów naraz).
- [ ] Readonly nadal bez Save.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-210 | E2E | pl-PL payment amount regex |
| PW-OPS-E2E-211 | E2E | sv-SE date 2026-08-20 |
| PW-OPS-E2E-212 | E2E | switch LocaleSelect EN→PL persist reload |
| PW-OPS-E2E-213 | E2E | en-US amount €1,234.56 |

### Learning

Nie hardcoduj `"Save"` w POM jeśli testujesz lokalizację. `test.use({ locale })` ≠ tłumaczenie UI (to browser locale); user select nadpisuje.
