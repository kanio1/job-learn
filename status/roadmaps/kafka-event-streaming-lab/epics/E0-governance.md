---
name: epic-e0-governance
parent: kafka-event-streaming-lab
epic: E0
tasks: [KAFKA-T00, KAFKA-T01]
last_updated: 2026-08-21
---

# Epic E0 — Governance & ADR (gate)

**Cel produktowy:** brak — fala czysto decyzyjno-dokumentacyjna.
**Cel dydaktyczny:** zrozumieć, dlaczego broker wchodzi jako externalization na outbox, a nie jako drugie source of truth; spójny język (glossary) przed kodem.

Gate wejścia: brak. Gate wyjścia: user akceptuje ADR 0002 i edycje governance — dopiero wtedy E1+.

## Story E0-S1 — Decyzja

**Task:** `KAFKA-T01` · P0

Jako właściciel laba akceptuję/odrzucam ADR 0002, aby dalsze fale nie były flagowane jako scope creep przez agentów.

AC:
1. ADR 0002 status → ACCEPTED (lub REJECTED z komentarzem).
2. `AGENTS.md`: non-goal "No Kafka" → "Kafka only in `eventlab` / approved overlay"; pozostałe non-goals bez zmian (PSP, split, fake KPI…).
3. `.codex/review-checklist.md`: sekcja boundary review dla eventlab (nie OPEN, brak importów internal, V37+ tylko eventlab).
4. Skills: `spring-modulith` (+modules), `code-review`, `rest-api-test-design` — dopisek "Kafka only in eventlab" zamiast twardego "No Kafka".
5. `.codex/CONTEXT.md`: glossary (10 terminów z 00-context) + aktualizacja mapy modułów o eventlab (E5 dopina pełny refresh CONTEXT.md).
6. Zero zmian w kodzie aplikacji i testach.

Testy: brak (docs only). Walidacja: przegląd diffów dokumentów; agenci przestają flagować Kafka jako creep po E0.
