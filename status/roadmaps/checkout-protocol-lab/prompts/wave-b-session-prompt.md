---
name: cpl-wave-b-session-prompt
parent: checkout-protocol-lab
audience: Cursor Agent (Grok 4.5 / Composer 2.5)
purpose: High cache-hit multi-turn for CPL-T08…T13 (E2 ingestion + E3 fulfillment)
depends_on: Wave A complete (T01–T07 DONE)
last_updated: 2026-08-09
---

# CPL — Wave B prompt pack (cache-friendly)

**Zasada:** nowy Agent chat. Nie wklejaj historii Wave A. Stały kontrakt raz; follow-upy krótkie.

## @ przy starcie (raz)

```text
@AGENTS.md
@status/roadmaps/checkout-protocol-lab/README.md
@status/roadmaps/checkout-protocol-lab/task-board.md
@status/roadmaps/checkout-protocol-lab/01-infra-postgres-keycloak-security.md
@status/roadmaps/checkout-protocol-lab/epics/E2-event-ingestion.md
@status/roadmaps/checkout-protocol-lab/epics/E3-async-fulfillment.md
@status/roadmaps/checkout-protocol-lab/learning-map.md
```

Opcjonalnie (gdy agent ma szukać wzorców w kodzie Wave A — `@` katalogu lub konkretnych klas, nie paste):

```text
@apps/backend/src/main/java/lab/paymentquality/checkoutlab
@apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java
```

Kolejność Wave B:

```text
CPL-T08 → T09 → T10 → T11 → T12 → T13
(Stop przed E4 / T14 — Wave C = nowy chat)
```

---

## MESSAGE 0 — SESSION CONTRACT (wklej RAZ)

````markdown
# SESSION CONTRACT — Checkout Protocol Lab (CPL) · Wave B

You are the implementation agent for Payment Quality Engineering Lab.
Follow repo AGENTS.md. Prefer Grok 4.5 for security/HMAC decisions; Composer 2.5 OK for mechanical follow-ups.

## Mission

Implement **only** CPL Wave B in order, one work package per turn unless I batch:

`CPL-T08 → T09 → T10 → T11 → T12 → T13`

Canonical backlog:

- `status/roadmaps/checkout-protocol-lab/epics/E2-event-ingestion.md`
- `status/roadmaps/checkout-protocol-lab/epics/E3-async-fulfillment.md`
- `status/roadmaps/checkout-protocol-lab/task-board.md`
- `status/roadmaps/checkout-protocol-lab/01-infra-postgres-keycloak-security.md`

Wave A is already DONE (module, Flyway, OAuth stub, POST/GET sessions, pass-through). Reuse it — do not redesign.

## Hard constraints

1. Stay in `lab.paymentquality.checkoutlab` (+ `internal.*`).
2. Flag `app.checkout-lab.enabled`; off → no lab surface.
3. **No Kafka.** Inbox = Postgres `checkout_event` + scheduled/poller worker.
4. **No Keycloak realm changes.** Notify has **no JWT** — HMAC on raw body.
5. Do not alter `payment_orders`, `idempotency_records`, `event_publication`, `MockPspClient`.
6. Fulfillment changes **only** via worker after verified events — never from continueUrl.
7. Signature: HMAC-SHA256 on raw bytes; constant-time compare; `t=,v1=` header; tolerance from properties.
8. Status matrix: verify OK → **202** enqueue; bad sig / stale t → **400** (no retry); transient → **503** (retry); duplicate eventId → **200** `{duplicate:true}`.
9. Do not edit `.kiro/**`. Mark DONE in `task-board.md`.
10. No secrets in git. Skip `restkit/**` / `paymentsupport/**` unless asked.
11. Stop after T13 unless I say continue. No E4 FE / Playwright in this chat.

## Patterns already in repo (reuse)

- Pass-through chain already covers `POST /api/checkout-lab/notify`
- Lab Bearer filter for `/sessions` — do **not** put JWT on notify
- Tables: `checkout_session`, `checkout_event`, `checkout_fulfillment`
- `CheckoutLabProperties.hmacSecret` + `signatureToleranceSeconds`

## Per-task DoD

AC from epic only · focused tests · smallest verify · mark DONE · Learning note (HTTP/REST/SQL/KC) · Next line · no auto-next unless batched.

## Output each turn

Plan → Diff summary → Verify → Learning note → Next

## Out of scope

E4 hosted UI, E5 booking, E6 inspector/scenarios (except minimal hooks if required by T08), E7 PW pack, T20 idempotency, Kafka, real PSP.

## First action

Wait for kickoff naming T08. Do not implement until then.
````

---

## MESSAGE 1 — Kickoff T08

````markdown
# Kickoff — CPL Wave B

Start at: **CPL-T08** (E2-S1 stub notifier).

AC: @status/roadmaps/checkout-protocol-lab/epics/E2-event-ingestion.md (E2-S1)

Emit signed POST to session.notifyUrl after a lab-only simulate outcome (Approve path stub):
- Headers: Lab-Event-Id, Lab-Signature (t=,v1=), Content-Type application/json, X-Correlation-ID
- Sign raw body with hmacSecret
- Types: checkout.session.completed / canceled (as needed)

Wire a minimal flag-gated simulate endpoint or internal trigger so tests can emit without FE.
Do not implement receiver verify yet (T09) beyond what T08 needs to call notifyUrl.

When T08 green, stop and wait.
````

---

## FOLLOW-UPS

```markdown
Continue. Next: CPL-T09 only.
AC: @status/roadmaps/checkout-protocol-lab/epics/E2-event-ingestion.md (E2-S2)
Receiver: raw body HMAC + tolerance → inbox RECEIVED → 202; bad sig → 400; DB fail → 503.
No JWT. No fulfillment mutate here.
Stop when DONE + Learning note + Next.
```

```markdown
Continue. Next: CPL-T10 only.
AC: E2-S3 duplicate Lab-Event-Id → 200 {duplicate:true}; UNIQUE event_id; no second fulfill side-effect.
Stop when DONE + Learning note + Next.
```

```markdown
Continue. Next: CPL-T11 only.
AC: @status/roadmaps/checkout-protocol-lab/epics/E3-async-fulfillment.md (E3-S1)
@Scheduled poller; RECEIVED→PROCESSING→DONE/FAILED; FOR UPDATE SKIP LOCKED.
Stop when DONE + Learning note + Next.
```

```markdown
Continue. Next: CPL-T12 only.
AC: E3-S2 refetch session before apply; OOO-safe.
Stop when DONE + Learning note + Next.
```

```markdown
Continue. Next: CPL-T13 only — last Wave B task.
AC: E3-S3 fulfillment SM (AWAITING_PAYMENT→CONFIRMED|CANCELLED|EXPIRED).
continueUrl must not change SM.
After DONE: Wave B closeout (T08–T13 table + what is stubbed). Do NOT start T14.
Stop.
```

---

## Wave C (później, NOWY chat)

`T14 → T16` (+ later T17/T18) — hosted checkout FE + lie return + pay_no_return + assurance.
