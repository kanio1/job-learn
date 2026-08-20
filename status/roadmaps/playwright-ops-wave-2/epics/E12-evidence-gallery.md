---
name: epic-e12-evidence-gallery
parent: playwright-ops-wave-2
epic: E12
tasks: [PW-OPS-T19]
last_updated: 2026-08-20
---

# Epic E12 — Evidence Gallery (#22)

**Cel produktowy:** druga połowa `EvidenceUpload` — przegląd załączników (carousel), nie tylko lista+download.  
**Cel dydaktyczny:** keyboard next/prev, lazy media, broken attachment, 403, istniejący download.

**Gate:** evidence GET/POST już są (`payments-evidence-export.spec.ts`). Pin 4.7.1: **wypisać** czy `UCarousel` jest w eksporcie. Fallback: dialog + Previous/Next.

Nuxt UI: `UCarousel` (Embla) jeśli dostępny.  
POM: `EvidenceCarouselComponent`.

Nie top-5 Wave 2 — Fala 9.

---

## Story E12-S1 — Gallery UI + keyboard

**Task:** `PW-OPS-T19` · P1

### Jako / chcę / aby

Jako support oglądam screenshoty sprawy / płatności strzałkami, bez pobierania każdego pliku.

### Business case

`BC-OPS-22` — Evidence już się wgrywa. Brakuje przeglądu.

### Use case

`UC-OPS-37` — Payment detail Evidence: carousel dots; Previous/Next; `select` emit index.

### Acceptance criteria

- [ ] Tylko evidence **tego** payment order (BOLA jak list).
- [ ] Keyboard: ArrowLeft/Right, Home/End jeśli Carousel to wspiera; inaczej przyciski z focus.
- [ ] Image lazy: `loading=lazy` na nieaktywnych slajdach.
- [ ] Non-image (PDF): slajd z ikoną + Download (istniejący GET bytes).
- [ ] `toMatchAriaSnapshot` opcjonalnie P2.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-220 | E2E | upload 3 images via existing UI → next → index 2 visible |
| PW-OPS-E2E-221 | E2E | ArrowRight keyboard |
| PW-OPS-E2E-222 | E2E | download still works (existing bytes oracle) |

---

## Story E12-S2 — Broken / denied

**Task:** `PW-OPS-T19` · P1

### Use case

`UC-OPS-38` — blob 404/500 → slajd error, nie biały crash. Cudzy evidenceId w URL → 404 jak dziś.

### Acceptance criteria

- [ ] Broken: `UAlert` w slajdzie; carousel nadal nawigowalny.
- [ ] Manager innej merchant → 404 thumbnail.
- [ ] Brak `page.route` do „zepsucia” — **albo** seed wiersza z nieistniejącym storage key (lab column już ma bytes w DB V19). Lepiej: RA wstawia evidence metadata wskazującą missing; UI GET 404. Jeśli bytes zawsze w DB, dodać flag test-only **nie**. Użyj niepoprawnego `evidenceId` w deep-link query `?evidence=` jeśli taki będzie; inaczej BffClient nie tworzy broken — **P2** skip jeśli nie da się bez mocka.

**Decyzja:** broken = GET evidence id losowy UUID → 404 slajd. Nie trzeba psuć storage.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-OPS-E2E-223 | E2E | open gallery invalid id → error slide |
| PW-OPS-E2E-224 | E2E | other merchant evidence 404 |
| PW-OPS-API-070 | PW REST | GET evidence 404 problem |

### Learning

Inna klasa UI niż upload. Nie duplikować Download Lab.
