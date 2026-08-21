---
name: epic-e1-broker-infra
parent: kafka-event-streaming-lab
epic: E1
tasks: [KAFKA-T02, KAFKA-T03, KAFKA-T04, KAFKA-T05]
last_updated: 2026-08-21
---

# Epic E1 — Broker infrastructure (lab overlay)

**Cel produktowy:** operator może opcjonalnie wystartować stack z brokerem jedną komendą; domyślny stack i CI pozostają bez Kafki.
**Cel dydaktyczny:** KRaft bez ZooKeeper, advertised listeners host-vs-sieć, topologia tematu (partycje/RF), smoke IT na Testcontainers.

Gate: E0 DONE (ADR ACCEPTED).

## Story E1-S1 — Compose overlay

**Task:** `KAFKA-T02` · P0

Jako operator uruchamiam brokera overlayem compose, aby nie modyfikować domyślnego środowiska reszty milestone'ów.

AC:
1. `infra/compose/compose.kafka.yml`: `apache/kafka` 4.x KRaft combined, PLAINTEXT, dual listeners (`localhost:9092` host / `payment-quality-kafka:19092` sieć); tag pinowany i udokumentowany vs Testcontainers 2.0.5.
2. Overlay nie zmienia `compose.yml`; `down` default stacku nie dotyczy brokera dopóki overlay nie użyty.
3. Docs setup: sekcja w `docs/setup/` (start/stop, port, UI operatora opcjonalne).
4. `RA-KAFKA-001`: IT smoke — kontener up, create/list topic, produce/consume roundtrip (Awaitility).

## Story E1-S2 — Tryb --kafka

**Task:** `KAFKA-T03` · P1

Jako developer startuję pełny lab z brokerem przez `scripts/dev-stack.sh --kafka`, aby uczyć się na żywym stacku.

AC:
1. Trzeci tryb (nie łączy się z `--app`/`--full`); Spring dostaje `app.event-lab.enabled=true` + profil `kafka`.
2. Health check: temat istnieje/auto-tworzony; fail-fast z czytelnym komunikatem.
3. `RA-KAFKA-002`: skrypt idempotentny (drugi start nie duplikuje tematu).

## Story E1-S3 — Wsparcie testowe

**Task:** `KAFKA-T04` · P0

Jako SDET mam `KafkaContainerSupport` obok `PostgresContainerSupport`, aby IT nie odpalały kontenera per klasa.

AC:
1. Singleton container (`org.testcontainers.kafka.KafkaContainer`, obraz jak compose); współdzielenie między klasami IT.
2. Awaitility dodane (test scope, pin) LUB house helper — jedna decyzja na cały milestone.
3. Surefire excludes `**/*KafkaIT*.java`; `AT-KAFKA-001`: pełny kontekst Spring startuje bez brokera przy flagach domyślnych.

## Story E1-S4 — Temat v1

**Task:** `KAFKA-T05` · P0

Jako learner widzę 3 partycje na `lab.auditable-actions.v1`, aby zaobserwować porządek per-key i przeplatanie między kluczami.

AC:
1. Temat: 3 partycje, RF1, polityka tworzenia jawna (admin client w support lub auto-create z konfiguracją).
2. `RA-KAFKA-003`: topologia assert (partitions=3, RF=1).

Nuxt UI: brak (fala backendowa). Testy: RA-KAFKA-001…003, AT-KAFKA-001.
