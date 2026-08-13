---
name: epic-e2-visual-lab
parent: browser-session-visual-network-lab
epic: E2
tasks: [MRL-T06, MRL-T07, MRL-T14]
last_updated: 2026-08-13
---

# Epic E2 — Visual Comparison Lab

**Cel produktowy:** stabilna strona do nauki `toHaveScreenshot` (PayU/bank UI: status color, CTA, disclaimer).  
**Cel dydaktyczny:** golden files, próg vs mask, light/dark, kiedy **nie** robić visual.

**Połączenia:** F-D5 `visual-regression.spec.ts` (badge only) — E2 **rozszerza**, nie kasuje. Playwright [Visual comparisons](https://playwright.dev/docs/test-snapshots).

Czemu to nie jest `page.screenshot()` na pamiątkę: asercja pikselowa względem committed PNG; fail produkuje diff.

---

## Story E2-S1 — Stable tiles page  
**Task:** `MRL-T06` · P0 · FR-V01 FR-V02

### Jako / chcę / aby
Jako nowicjusz visual chcę kafelki ze **stałym** copy i wyłączonym ruchem, aby baseline nie flakował.

### Acceptance criteria
- [ ] `/admin/visual-lab` (`ssr: false` jak inne admin).
- [ ] `prefers-reduced-motion` / brak spinnerów w kafelkach.
- [ ] Brak `new Date()`, UUID, correlation id w tekście kafelka (dynamic w `[data-dynamic]` poza kafelkiem).
- [ ] Kafelki `data-testid`:
  - `visual-tile-merchant-badge`
  - `visual-tile-payment-badge`
  - `visual-tile-problem-details`
  - `visual-tile-hosted-cta`
  - `visual-tile-idle-lock`
- [ ] Alert: „Do not screenshot live payment lists — IDs change”.

### Learning
- `PW:` locator screenshot vs full page.
- Anti-pattern: visual na tabeli z seed UUID.

---

## Story E2-S2 — Component + full-page specs  
**Task:** `MRL-T07` · P0 · FR-V04 FR-V06

### Jako / chcę / aby
Jako test architect chcę mocked Chromium spec z goldenami, aby CI łapało regresję koloru.

### Acceptance criteria
- [ ] Spec `tests/e2e/visual-lab.spec.ts` (mocked session).
- [ ] `expect(locator).toHaveScreenshot('visual-tile-hosted-cta.png')` per kafelek.
- [ ] Jeden `expect(page).toHaveScreenshot('visual-lab-full.png', { stylePath })`.
- [ ] README w docs: `--update-snapshots`, commit PNG, review diff.
- [ ] `maxDiffPixelRatio` dziedziczy 0.02 z config; **nie** podnosić per test.
- [ ] F-D5 badge spec zostaje (osobny plik).

### Learning
- `PW:` snapshot folder `*.spec.ts-snapshots/`.
- CI: ten sam OS/image co baseline (C-06).

---

## Story E2-S3 — Dark, mask, break-visual  
**Task:** `MRL-T14` · P1 · FR-V03 FR-V05

### Jako / chcę / aby
Jako SDET chcę dark mode, maskowanie `[data-dynamic]` i toggle zepsutego koloru, aby zobaczyć **fail**.

### Acceptance criteria
- [ ] Color mode toggle; osobne golden `*-dark.png` albo `colorScheme: 'dark'` project (desktop only).
- [ ] `stylePath` ukrywa `[data-dynamic] { visibility: hidden }`.
- [ ] Toggle `data-testid="visual-lab-break"` zmienia CTA na zły kolor (np. danger zamiast primary).
- [ ] Test `break visual` **skipped by default** albo osobny tag `@visual-negative` dokumentujący oczekiwany fail + instrukcja „uruchom żeby zobaczyć diff”.
- [ ] Copy: ARIA snapshot ≠ visual (struktura vs piksele).

### Learning
- `PW:` mask, stylePath, colorScheme.
- `PW:` `toMatchAriaSnapshot` na tym samym kafelku (opcjonalny test P2).

### Stop-gates
- Brak Argos/Percy w MVP.
- Brak mobile device project.
- Nie commitować baseline z Fedora vs Ubuntu mix bez decyzji C-06.
