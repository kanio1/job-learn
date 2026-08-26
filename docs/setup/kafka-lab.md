# Kafka Lab — overlay and Lenses telescope

Lab broker is `infra/compose/compose.kafka.yml` (apache/kafka:4.0.0 KRaft, PLAINTEXT).

## Start

```bash
cp infra/compose/.env.example infra/compose/.env
scripts/dev-stack.sh --kafka
# creates lab.auditable-actions.v1 (3 partitions RF1) and DLT lab.event-lab.dlq.v1 idempotently
```

Do not combine `--kafka` with `--app` / `--full`. Host `9092` belongs to the lab overlay.

## Lenses vs lab

- Lenses HQ: http://localhost:9991 (admin/admin) — telescope only
- Lenses environment `payment-lab` should point at `PLAINTEXT://payment-quality-kafka:19092`
- Demo CE Kafka (telecom_italia_data) must **not** bind host 9092 while lab overlay is up

See `.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md` — RF=1 is lab-shaped, not a production bug.

## Verify

```bash
docker exec payment-quality-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker exec payment-quality-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic lab.auditable-actions.v1
```

## Tests

- Broker-free: `./mvnw test` (Surefire excludes `*KafkaIT`)
- With broker: `./mvnw -Dit.test=EventLabBrokerKafkaIT verify`
- Live POM: `PLAYWRIGHT_KAFKA=1 corepack pnpm --dir apps/frontend exec playwright test --config playwright.pom.config.ts --project=chromium-kafka`
