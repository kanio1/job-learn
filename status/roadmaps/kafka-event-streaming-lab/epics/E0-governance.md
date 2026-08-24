---
name: epic-e0-governance
parent: kafka-event-streaming-lab
epic: E0
tasks: [KAFKA-T00, KAFKA-T01]
last_updated: 2026-08-23
status: DONE
---

# Epic E0 — Governance & ADR (gate) — DONE

**Cel:** broker jako externalization, nie drugie SoT; spójny język; agenci nie flagują Kafki w `eventlab` jako creep.

Gate wyjścia: ADR ACCEPTED + edycje governance. **Spełnione 2026-08-23.**

## Story E0-S1 — Decyzja

**Task:** `KAFKA-T01` · P0 · **DONE**

AC (wykonane):

1. ADR 0002 → **ACCEPTED** (iteracja 3: Lenses luneta, cienkie E3, brak E6 produktu).
2. `AGENTS.md` / `CLAUDE.md`: Kafka only in `eventlab` / overlay.
3. Skills: `eventlab-kafka` + lab≠prod; `spring-modulith`, `code-review`, `rest-api-test-design`, `ask-engineering-flow`, `grilling`, `implementation-learning-loop`, `triage`, `java-spring-review`, `nuxt-frontend`.
4. Lenses skills (`kafka-topic-audit`, `kafka-security-audit`, `kafka-schema-review`, `kafka-dlq-review`) — dopisek lab≠prod.
5. Glossary w `.codex/CONTEXT.md`; review checklist eventlab.
6. Zero kodu aplikacji.

Następny: E1 (`KAFKA-T02`).
