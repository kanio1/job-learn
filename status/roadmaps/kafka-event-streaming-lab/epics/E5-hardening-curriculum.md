---
name: epic-e5-hardening-curriculum
parent: kafka-event-streaming-lab
epic: E5
tasks: [KAFKA-T18]
last_updated: 2026-08-21
---

# Epic E5 — Hardening & curriculum wrap-up

**Cel produktowy:** lab jest odporny na długie życie (retention, seeds, dokumentacja).
**Cel dydaktyczny:** rebalance, replay, property-based pewność koperty; domknięcie języka i mapy modułów.

Gate: E3 DONE (E4 opcjonalne, niezależne).

## Story E5-S1 — Rebalance lesson

**Task:** `KAFKA-T18` · P2

Jako learner uruchamiam drugą instancję konsumenta w tej samej grupie w trakcie testu, abym zobaczył przeniesienie partycji bez duplikatów.

AC:
1. `RA-KAFKA-050`: dwa listenery jednej grupy w jednym IT — każdy rekord przetworzony dokładnie raz (unique constraint jako arbiter).
2. Test nie assertuje konkretnego przydziału partycji (niedeterministyczne) — tylko sumę i brak duplikatów.

## Story E5-S2 — Seed-guard & property

**Task:** `KAFKA-T18` · P1

Jako maintainer mam dowód, że seeds i flag-off nigdy nie dotykają brokera, a koperta v1 ewoluuje addytywnie.

AC:
1. `RA-KAFKA-051`: reset/seed endpoints + DataLearningDataset path przy flag off ⇒ zero rekordów, zero połączeń (ADR 0001 w świecie Kafki).
2. jqwik property: mapper koperty zachowuje eventId/action/targetType/correlationId dla generowanych zdarzeń; field-additive dekodowanie v1→v1.1.
3. `AT-KAFKA-003`: architecture verify + brak importów internal w eventlab.

## Story E5-S3 — Dokumentacja domknięta

**Task:** `KAFKA-T18` · P1

AC:
1. `.codex/CONTEXT.md`: glossary uzupełnione, mapa modułów z eventlab; `docs/testing/event-streaming-lab/` zaktualizowany o wyniki (PASS/FAIL evidence).
2. `status/index.md` + `.codex/current-state.md` wpis IMPLEMENTED per fala; `.kiro/**` nietknięte.
3. Lekcja podsumowująca: outbox vs CDC, key ordering, at-least-once→exactly-once effect, DLQ operacje — mapping na przetestowane ID.
