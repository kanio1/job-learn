---
name: epic-e1-broker-infra
parent: kafka-event-streaming-lab
epic: E1
tasks: [KAFKA-T02, KAFKA-T03, KAFKA-T04, KAFKA-T05, KAFKA-T19]
last_updated: 2026-08-23
---

# Epic E1 — Broker overlay + Lenses telescope

**Cel produktowy:** opcjonalny broker jedną komendą; domyślny stack bez Kafki.
**Cel dydaktyczny:** KRaft, advertised listeners, topologia 3×RF1, luneta Lenses na *naszym* temacie.

Gate: E0 DONE. Skill: `eventlab-kafka`.

## Story E1-S1 — Compose overlay

**Task:** `KAFKA-T02` · P0

AC:
1. `infra/compose/compose.kafka.yml`: `apache/kafka` 4.x KRaft, PLAINTEXT, `localhost:9092` / `payment-quality-kafka:19092`; tag pin vs Testcontainers 2.0.5.
2. **Brak** AKHQ/kafka-ui.
3. Overlay nie psuje `compose.yml`.
4. Docs: `docs/setup/` start/stop + kolizja 9092 z Lenses CE ([02-lenses-telescope.md](../02-lenses-telescope.md)).
5. `RA-KAFKA-001`: smoke produce/consume (Awaitility).

## Story E1-S2 — Tryb --kafka

**Task:** `KAFKA-T03` · P1

AC:
1. Trzeci tryb; nie łączy się z `--app`/`--full`.
2. Spring: `app.event-lab.enabled=true` + profil `kafka`.
3. Auto-create OFF; skrypt **tworzy** temat (3 partycje, RF1) idempotentnie.
4. `RA-KAFKA-002`.

## Story E1-S3 — Wsparcie testowe

**Task:** `KAFKA-T04` · P0

AC:
1. `KafkaContainerSupport` singleton (`org.testcontainers.kafka.KafkaContainer`).
2. Awaitility test-scope.
3. Surefire excludes `**/*KafkaIT*.java`.
4. `AT-KAFKA-001`: kontekst bez brokera przy flagach domyślnych.

## Story E1-S4 — Temat v1

**Task:** `KAFKA-T05` · P0

AC:
1. `lab.auditable-actions.v1`: 3 partycje, RF1.
2. `RA-KAFKA-003`.
3. Dokument: RF=1 jest lab-shaped; `kafka-topic-audit` zgłosi critical — lab≠prod.

## Story E1-S5 — Lenses środowisko payment-lab

**Task:** `KAFKA-T19` · P1

AC:
1. Lenses widzi overlay (agent lub env `payment-lab`); CE demo **nie** binduje host 9092.
2. Po E2 (gate miękki na SQL): instrukcja `SELECT` na `lab.auditable-actions.v1`.
3. Zero kodu Nuxt.

Nuxt UI: brak. Testy: RA-KAFKA-001…003, AT-KAFKA-001.
