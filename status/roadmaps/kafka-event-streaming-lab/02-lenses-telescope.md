---
name: kafka-event-streaming-lab-lenses
parent: kafka-event-streaming-lab
last_updated: 2026-08-23
---

# 02 — Lenses as telescope

Pełny framing: [lenses-lab-vs-prod.md](../../../.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md).

## Co już stoi lokalnie (2026-08-23)

- Lenses HQ http://localhost:9991 (`admin` / `admin`)
- MCP stdio w Groku: serwer `Lenses` → `~/lenses-mcp/run-stdio.sh` (klucz w `~/lenses-mcp/.env`, nie w git)
- CE compose: `~/lenses-ce/` (demo Kafka **nie** może bindować host 9092 obok overlayu labu)
- User skills: `~/.grok/skills/kafka-*` (topic-audit / consumer-lag / dlq-review / …)

## Zadanie implementacyjne (E1-S5 / `KAFKA-T19`)

1. Overlay labu na 9092.
2. Lenses agent (lub drugie środowisko HQ) **payment-lab** → bootstrap `PLAINTEXT://payment-quality-kafka:19092` (sieć: albo attach agent do sieci compose labu, albo advertised listener osiągalny z hosta).
3. Dokument: jak uruchomić CE *playground* bez 9092/8081 na hoście.
4. Smoke: po E2 `SELECT` w Lenses widzi `lab.auditable-actions.v1`.

Nie instalujemy AKHQ. Nie wołamy Lenses MCP z testów.

## Skills — kiedy na klastrze labu

Po E2: `kafka-topic-audit` **tylko z ramką lab≠prod** (RF=1 = critical w skillu, nie ticket).
Po E3: `kafka-consumer-lag`, `kafka-dlq-review` na grupie `eventlab-inspector` i DLT.  
Wave 1: **nie** `kafka-schema-review` / ShadowTraffic / connector-review jako praca produktu.
