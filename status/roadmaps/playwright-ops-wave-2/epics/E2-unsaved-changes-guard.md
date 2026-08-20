---
name: epic-e2-unsaved-changes-guard
parent: playwright-ops-wave-2
epic: E2
tasks: [PW-OPS-T04]
last_updated: 2026-08-20
---

# Epic E2 — Unsaved Changes Guard (#16)

**Cel produktowy:** brudny merchant form nie znika po przypadkowym Back / kliknięciu Merchants.  
**Cel dydaktyczny:** trzy drogi opuszczenia — NuxtLink, `page.goBack()`, native `beforeunload`.

**Gate:** E1 merchant edit form istnieje.

Nuxt UI: `UBreadcrumb`, `UModal`.  
Nie nowy Slideover lab — to **navigation guard**, inna klasa problemu.

---

## Story E2-S1 — In-app leave (Link + Back)

**Task:** `PW-OPS-T04` · P0

### Jako / chcę / aby

Jako operator z gwiazdką „unsaved changes” chcę Stay albo Discard, gdy kliknę Merchants albo Back.

### Business case

`BC-OPS-16` — CRM dirty form. Utrata telefonu po 412-recovery jest gorsza niż extra modal.

### Use case

`UC-OPS-16` — Edit phone (bez save) → sidebar Merchants **lub** `page.goBack()` → `UModal`:

```text
You have unsaved changes.
[Stay] [Discard and leave]
```

### Domain rules

- Dirty = różnica vs last loaded GET (nie vs initial mount if still equal).
- Stay: URL nie zmienia się; modal znika; PATCH count = 0.
- Discard: nawigacja kontynuowana; PATCH count = 0; nastepny GET bez lokalnych zmian.
- Czysty form: nawigacja bez modala.

### Acceptance criteria

- [ ] `onBeforeRouteLeave` (Vue Router / Nuxt) dla `/admin/merchants/{id}/edit` (lub slideover edit — **jeden** surface; preferuj dedicated edit page albo 360 form w slideover M360 — nie oba guardy rozjechane).
- [ ] Modal `getByRole('dialog', { name: /unsaved/i })`.
- [ ] Stay: `expect(page).toHaveURL(/merchant/)`.
- [ ] Discard: URL listy merchantów.
- [ ] Breadcrumb klik też strzela guard.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-160 | E2E | goBack → modal Stay → nadal edit URL |
| PW-OPS-E2E-161 | E2E | NuxtLink Merchants → Discard → list URL |
| PW-OPS-E2E-162 | E2E | czysty form → Back **bez** dialogu |

---

## Story E2-S2 — Native beforeunload

**Task:** `PW-OPS-T04` · P0

### Use case

`UC-OPS-17` — reload / close karty. Playwright: [dialogs](https://playwright.dev/docs/dialogs).

```ts
page.on('dialog', async (dialog) => {
  expect(dialog.type()).toBe('beforeunload');
  await dialog.dismiss(); // Stay
});
await page.close({ runBeforeUnload: true });
```

### Acceptance criteria

- [ ] `window.onbeforeunload` tylko gdy dirty.
- [ ] Test dismiss = strona zostaje (close aborted) **albo** dokumentuj że Chromium close+dismiss zachowanie — jeśli flake, tag `@browser-dialog` i P1.
- [ ] Accept dialog = leave without PATCH.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-163 | E2E | dirty + close runBeforeUnload + dismiss |

---

## Story E2-S3 — Guard nie zapisuje

**Task:** `PW-OPS-T04` · P0

### Acceptance criteria

- [ ] Od otwarcia modala do Stay: **zero** requestów PATCH merchant (nasłuch `waitForResponse` z predicate + expect count 0 w `page.on('request')` buffer).
- [ ] To jest claim biznesowy w spec, nie schowany w POM.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-164 | E2E | Stay → captured PATCH requests length 0 |

### Learning

Trzy różne API Playwright na jeden product rule. POM: `UnsavedGuardDialog.stay() / discard()`.
