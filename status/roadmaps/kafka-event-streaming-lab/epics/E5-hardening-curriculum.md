---
name: epic-e5-hardening-curriculum
parent: kafka-event-streaming-lab
epic: E5
tasks: [KAFKA-T18]
last_updated: 2026-08-23
---

# Epic E5 — Hardening (bez dashboardu lagu)

**Cel dydaktyczny:** rebalance, seed-guard, property koperty.
**Nie:** ECharts, generator load, druga instancja jako produkt ops.

Gate: E3 DONE (E4 opcjonalne).

## Story E5-S1 — Rebalance lesson

**Task:** `KAFKA-T18` · P2

AC:
1. `RA-KAFKA-050`: dwa listenery jednej grupy w jednym IT — każdy rekord raz (unique constraint).
2. Nie assertuj konkretnego przydziału partycji.
3. Lenses może *pokazać* rebalance — nie jest oracle.

## Story E5-S2 — Seed-guard & property

**Task:** `KAFKA-T18` · P1

AC:
1. `RA-KAFKA-051`: reset/seed + DataLearningDataset przy flag off ⇒ zero rekordów, zero połączeń.
2. jqwik: mapper koperty zachowuje eventId/action/targetType/correlationId; field-additive v1.
3. `AT-KAFKA-003`.

## Story E5-S3 — Dokumentacja

AC:
1. `.codex/CONTEXT.md` glossary + mapa `eventlab`.
2. `status/index.md` + `.codex/current-state.md` IMPLEMENTED per fala.
3. `.kiro/**` nietknięte.
4. Wrap-up: outbox vs CDC, key ordering, at-least-once→efekt, DLT — mapping na ID testów. Lenses = luneta.
