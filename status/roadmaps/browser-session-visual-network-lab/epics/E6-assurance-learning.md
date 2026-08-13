---
name: epic-e6-assurance
parent: browser-session-visual-network-lab
epic: E6
tasks: [MRL-T10, MRL-T11, MRL-T17, MRL-T29]
last_updated: 2026-08-13
---

# Epic E6 — Assurance & learning evidence

**Cel produktowy:** warstwy testów zaprojektowane, nie zaimplementowane w tym pakiecie.  
**Cel dydaktyczny:** piramida RA / PW-API / PW-E2E mocked / POM live / learner.

---

## Story E6-S1 — REST Assured / IT skeletony  
**Task:** `MRL-T10` · P0

### Jako / chcę / aby
Jako backend tester chcę listę klas do napisania przy implementacji.

### Acceptance criteria (design)
- [ ] Flag off IT (wzorzec `CheckoutLabEndpointsDisabledIT`).
- [ ] CSRF 403 + Bearer kontrast (gdy E1-S5).
- [ ] GET-with-body 403 (gdy E4-S1).
- [ ] Disputes multipart (gdy E5-S3) — kandydat `apps/api-tests` REST-MULTIPART-01.
- [ ] Wykluczenia `restkit/` `paymentsupport/` nadal obowiązują.

### Nie w tym design pack
Pisanie `*Test.java`.

---

## Story E6-S2 — Playwright split  
**Task:** `MRL-T11` · P0

### Jako / chcę / aby
Jako test architect chcę twardy split mocked vs POM w katalogu ID.

### Acceptance criteria (design)
- [ ] Katalog `docs/testing/payu-bank-mirror-labs/` z ID `PW-MRL-*`.
- [ ] POM: zero fulfill; cookies, clock, download, dual context.
- [ ] Mocked: visual-lab, route sequences, HAR replay.
- [ ] Guest project mapped.

---

## Story E6-S3 — Learner placeholders  
**Task:** `MRL-T17` · P1

### Jako / chcę / aby
Jako uczeń chcę puste specy w `tests-pom-learner`.

### Acceptance criteria (gdy implementacja wave 2)
- [ ] `specs/session-guest.spec.ts`, `visual-lab.spec.ts`, `network-lab.spec.ts` puste describe + README pointer.
- [ ] Nie import z `tests/e2e`.

---

## Story E6-S4 — Registry  
**Task:** `MRL-T29` · P2

### Jako / chcę / aby
Jako PM chcę wpis w `status/index.md` gdy program startuje / zamyka bramki.

### Acceptance criteria
- [ ] Design pack: wpis DESIGNED_NOT_STARTED (ten task board).
- [ ] Po implementacji E5-S3: rozważyć DONE_VERIFIED REST-MULTIPART-01 **tylko** z evidence api-tests.
