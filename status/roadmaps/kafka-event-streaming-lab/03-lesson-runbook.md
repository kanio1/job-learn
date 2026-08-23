---
name: kafka-event-streaming-lab-runbook
parent: kafka-event-streaming-lab
last_updated: 2026-08-23
---

# 03 — Lekcja 45 min (curriculum, nie 18 równoległych produktów)

Wymaga: E1–E3 DONE, `--kafka`, Lenses środowisko `payment-lab`.

| Min | Krok | Gdzie | Oczekiwany efekt |
|---|---|---|---|
| 0–5 | `scripts/dev-stack.sh --kafka`; zaloguj operator | terminal + UI | Event Lab w nav |
| 5–10 | Authorize/capture unikalnego orderu | dashboard | komenda REST |
| 10–15 | Lenses SQL: `SELECT * FROM \`lab.auditable-actions.v1\` …` po kluczu paymentOrderId | Lenses | 1 (lub uporządkowane) rekordy, key = id |
| 15–20 | `/admin/event-lab` search tego id **oraz** karta na detalu payment | Nuxt | 1 wiersz PROCESSED ≤ 5 s |
| 20–25 | Inject **duplicate** (ConfirmModal: „nie utworzy drugiego wiersza”) | Event Lab | nadal 1 wiersz |
| 25–35 | Inject **poison** | Event Lab + Lenses DLT | status DEAD, banner, płatność w Postgres bez zmian, rekord na `lab.event-lab.dlq.v1` |
| 35–40 | (opcjonalnie) `kafka-topic-audit payment-lab` | Grok | RF=1 critical → **lab≠prod**, nie fix |
| 40–45 | Replay mental: at-least-once vs unique constraint | docs | mapa na `RA-KAFKA-021…024` |

Playwright pokrywa kroki UI (authorize → visible; duplicate → 1; poison → DEAD), **nie** sesję Lenses.

Ten runbook jest AC `KAFKA-T20` (docs w `docs/setup/` + ten plik). Nie rozrastać do generatora 10k zdarzeń.
