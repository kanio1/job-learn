---
name: cpl-session-prompts
parent: checkout-protocol-lab
audience: Cursor Agent (Grok 4.5 / Composer 2.5)
purpose: High cache-hit, multi-turn implementation of CPL-T01…T07 (Foundation → Protocol Core MVP slice)
last_updated: 2026-08-09
---

# CPL — Prompt pack (cache-friendly)

## Jak używać (ważniejsze niż sam tekst)

### Zasady high cache hit rate (Cursor + Grok/Composer)

1. **Stabilny prefiks, zmienny ogon.** Wklej **SESSION CONTRACT** raz na początku czatu i **nie przepisuj go** w kolejnych wiadomościach. Follow-upy = tylko delta (task ID, błąd, „continue”).
2. **@ścieżki zamiast paste treści.** Nie wklejaj całych epiców do chatu — odwołuj się do plików. Treść plików ładuje się spójnie; Twój prompt zostaje krótki i powtarzalny.
3. **Bez dat/„dziś”/losowych ID na górze** sticky części — to zabija prefix cache.
4. **Jeden vertical na chat.** Ten pack = CPL foundation/protocol slice (`T01`→`T07`). Nowy chat gdy skaczesz do FE E4 albo Playwright E7.
5. **Nie zmieniaj mid-thread modelu** bez potrzeby. Grok 4.5 High = trudne decyzje arch/security; Composer 2.5 Fast = rutynowe pliki/testy po ustalonej ścieżce.
6. **Rules/skills sticky, status mutable.** Konwencje w `AGENTS.md`; postęp w `task-board.md` (agent aktualizuje status taska po DONE).
7. **Follow-up = 3–8 linii**, nie re-brief. Jeśli musisz „przypomnieć” — `@` kontraktu / epic, nie paste.

### Co @ dołączyć przy starcie czatu (raz)

```text
@AGENTS.md
@status/roadmaps/checkout-protocol-lab/README.md
@status/roadmaps/checkout-protocol-lab/task-board.md
@status/roadmaps/checkout-protocol-lab/01-infra-postgres-keycloak-security.md
@status/roadmaps/checkout-protocol-lab/epics/E0-foundation.md
@status/roadmaps/checkout-protocol-lab/epics/E1-protocol-core.md
@apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java
@apps/backend/src/main/java/lab/paymentquality/testing/internal/web/TestController.java
@apps/backend/src/main/resources/application.yml
```

Skills (gdy agent ma je w kontekście / user @):

```text
@.agents/skills/java-spring-review/SKILL.md
@.agents/skills/rest-api-test-design/SKILL.md
@.agents/skills/implementation-learning-loop/SKILL.md
```

### Kolejność pracy w tym czacie

```text
CPL-T01 → T02 → T03 → T04 → T05 → T06 → T07
(Stop przed E2 notifier — nowy mini-pack / kontynuacja z krótkim follow-upem)
```

---

## MESSAGE 0 — SESSION CONTRACT (wklej RAZ, potem nie ruszaj)

Skopiuj blok poniżej jako **pierwszą** wiadomość użytkownika w nowym Agent chat.  
Dołącz @-pliki z listy powyżej (nie wklejaj ich treści).

````markdown
# SESSION CONTRACT — Checkout Protocol Lab (CPL) · Wave A

You are the implementation agent for Payment Quality Engineering Lab.
Model preference: Grok 4.5 (architecture/security decisions) or Composer 2.5 (mechanical coding after plan is fixed). Follow repo `AGENTS.md`.

## Mission (stable)

Implement **only** CPL Wave A tasks in order, one work package per turn unless I explicitly batch:

`CPL-T01 → T02 → T03 → T04 → T05 → T06 → T07`

Canonical backlog (read, do not reinvent):

- `status/roadmaps/checkout-protocol-lab/README.md`
- `status/roadmaps/checkout-protocol-lab/task-board.md`
- `status/roadmaps/checkout-protocol-lab/epics/E0-foundation.md`
- `status/roadmaps/checkout-protocol-lab/epics/E1-protocol-core.md`
- `status/roadmaps/checkout-protocol-lab/01-infra-postgres-keycloak-security.md`
- `status/roadmaps/checkout-protocol-lab/learning-map.md`

## Hard constraints (do not violate)

1. New Modulith module: `lab.paymentquality.checkoutlab` + `internal.*` only.
2. Feature flag: `app.checkout-lab.enabled` (default **false**). Off → no controllers / 404.
3. **No Kafka.** Async later = Postgres inbox (not in Wave A beyond schema).
4. **No Keycloak realm changes.** Lab OAuth stub is in-app, not OIDC.
5. **Do not alter** `payment_orders`, `idempotency_records`, `event_publication`, `MockPspClient`, or treat `tenants.webhook_base_url` as IPN.
6. Do not implement E2 notify HMAC logic beyond security matcher stubs needed by T04; stop after T07 unless I say continue.
7. Do not edit `.kiro/**`. Update CPL `task-board.md` statuses when a task is DONE.
8. No secrets in git. Use env placeholders.
9. Keep Spring Modulith boundaries; `ModulithArchitectureTest` must stay green.
10. Backend validation excludes `restkit/**` and `paymentsupport/**` unless I ask otherwise.

## Patterns to copy (read code, match style)

- Flag + conditional beans: `testing` module / `TestController` + `app.testing.enabled`
- Security extra chain: `SecurityConfig` `@Order(2)` for `/api/test/*`
- Flyway locations list in `application.yml` + `application-test.yml`
- Amount/currency CHECKs: `payment/V2__create_payment_orders.sql` style
- Schema sketch: `01-infra-postgres-keycloak-security.md` §A.3 (V12)

## Per-task DoD (every task)

1. Implement only that task’s AC from the epic file.
2. Add/adjust focused tests proving AC.
3. Run the smallest credible verification (compile / affected tests).
4. Mark the task DONE in `task-board.md`.
5. End reply with a short **Learning note** (HTTP/REST/SQL/KC tags) — 3–5 bullets max.
6. End with **Next:** exact next task ID + one-line ask to proceed (do not auto-start next unless I said “continue through T0x”).

## Output shape (every turn)

1. **Plan** (3–6 bullets) — files to touch
2. **Diff summary** after changes
3. **Verify** — commands + results
4. **Learning note**
5. **Next**

## Out of scope this chat

E2–E7 product features, Playwright journeys, Booking UI, scenario engine, REST-REDIRECT status closure, Kafka, real PSP.

## First action

Wait for my Wave-A kickoff message naming the starting task (usually `CPL-T01`). Do not implement until that message.
````

---

## MESSAGE 1 — Kickoff Wave A (pierwszy task)

Wklej **po** kontrakcie (osobna wiadomość). Treść poniżej zostaw stałą; zmieniaj tylko linię `Start at` jeśli wznawiasz.

````markdown
# Kickoff — CPL Wave A

Start at: **CPL-T01** (E0-S1 Modulith skeleton + flag).

Read AC from `@status/roadmaps/checkout-protocol-lab/epics/E0-foundation.md` (E0-S1).
Mirror patterns from `@apps/backend/src/main/java/lab/paymentquality/testing/internal/web/TestController.java` and existing module `package-info` / Modulith tests.

When T01 is green, **stop** and wait — unless I reply with the Continue template.
````

---

## FOLLOW-UPS — szablony (cache-friendly, krótkie)

Używaj **tego samego czatu**. Nie wklejaj ponownie SESSION CONTRACT.

### A) Continue next task

```markdown
Continue. Next: CPL-T0N only.
AC: @status/roadmaps/checkout-protocol-lab/epics/<E0|E1-...>.md
Stop when DONE + Learning note + Next line.
```

Przykłady:

```markdown
Continue. Next: CPL-T02 only.
AC: @status/roadmaps/checkout-protocol-lab/epics/E0-foundation.md (E0-S2) + schema in @status/roadmaps/checkout-protocol-lab/01-infra-postgres-keycloak-security.md
```

```markdown
Continue. Next: CPL-T03 only.
AC: E0 / INFRA-CFG-01 in @status/roadmaps/checkout-protocol-lab/epics/E0-foundation.md
```

```markdown
Continue. Next: CPL-T04 only.
AC: security chains in @status/roadmaps/checkout-protocol-lab/epics/E1-protocol-core.md + @status/roadmaps/checkout-protocol-lab/01-infra-postgres-keycloak-security.md §C
Match style: @apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java
Permit only: POST /api/checkout-lab/oauth/token and POST /api/checkout-lab/notify (stubs OK if handlers not ready).
```

```markdown
Continue. Next: CPL-T05 only.
AC: E1-S1 OAuth stub @status/roadmaps/checkout-protocol-lab/epics/E1-protocol-core.md
form-urlencoded client_credentials; lab token ≠ Keycloak JWT.
```

```markdown
Continue. Next: CPL-T06 only.
AC: E1-S2 create session 302 + Location @status/roadmaps/checkout-protocol-lab/epics/E1-protocol-core.md
RA/IT must use redirects follow=false. Persist checkout_session + fulfillment AWAITING_PAYMENT.
```

```markdown
Continue. Next: CPL-T07 only.
AC: E1-S4 GET session @status/roadmaps/checkout-protocol-lab/epics/E1-protocol-core.md
Then stop Wave A. Summarize files touched T01–T07 and leftover risks before E2.
```

### B) Batch (gdy chcesz 2–3 taski bez stopu)

```markdown
Continue through CPL-T0A…T0B (inclusive). Still one logical PR mindset; report after each task briefly; full Learning note once at end of batch.
Do not go past T0B.
```

Przykład startowy:

```markdown
Continue through CPL-T01…T03 inclusive. Stop before T04.
```

### C) Fix verification failure (volatile na końcu)

```markdown
Fix only the failure below. Do not expand scope.

Command:
<code>

Output:
<paste truncated error>
```

### D) Re-align if agent drifts

```markdown
Stop. Re-read SESSION CONTRACT constraints 1–10.
You drifted by: <one sentence>.
Resume CPL-T0N only; revert unrelated edits if needed.
```

### E) End Wave A / handoff

```markdown
Wave A complete check:
1) Confirm T01–T07 DONE in @status/roadmaps/checkout-protocol-lab/task-board.md
2) List endpoints that exist vs stubbed
3) Propose Wave B kickoff (T08+) as a NEW chat using the same contract pattern — do not start E2 here.
```

---

## Mini-kontrakt Wave B (później, NOWY chat)

Gdy skończysz T07, **nowy chat**. Skopiuj SESSION CONTRACT, zamień sekcję Mission na:

```text
Implement CPL Wave B: T08 → T13 (E2 ingestion + E3 worker/SM).
Canonical: epics/E2-event-ingestion.md, epics/E3-async-fulfillment.md
Notify path: HMAC raw body; NO Keycloak JWT; 400 vs 503; 202 enqueue.
```

Nie mieszaj Wave A i B w jednym długim wątku jeśli kontekst puchnie (context ring).

---

## Cheat sheet — co agent ma znać bez paste

| Task | Uczysz się | Kluczowy assert |
|---|---|---|
| T01 | MOD + flag | enabled=false → 404 |
| T02 | SQL Flyway locations | V12 tables + validate |
| T03 | config/secrets | defaults false; env placeholders |
| T04 | HTTP security matchers | oauth+notify without KC JWT |
| T05 | form-urlencoded OAuth | 200 token / 401 bad secret |
| T06 | **302 + Location** | follow=false |
| T07 | REST retrieve | GET session oracle for E3 |

---

## Anti-patterns (nie rób tego w promptach)

- Wklejanie całego `E0-foundation.md` co turę
- „Zrób cały CPL MVP” w jednej wiadomości
- Proszenie o Kafka / real PayU / realm JSON edits
- Dopisywanie daty/godziny do SESSION CONTRACT
- Zmiana constraints mid-chat bez sekcji D)
- Paste 500 linii logów — tylko tail błędu

---

## Gotowiec: pierwsza sesja (checklist)

1. Nowy Agent chat (Grok 4.5 High **lub** Composer 2.5).
2. Wiadomość 0 = SESSION CONTRACT + @ pliki.
3. Wiadomość 1 = Kickoff T01.
4. Potem tylko szablony Continue / Fix.
5. Po T07 → End Wave A → nowy chat na E2.
