# Lenses: telescope, not the product — lab ≠ prod

Canonical framing for coding agents and learners. Do not copy this into every epic; link here.

## Two Kafkas (do not mix)

| Cluster | What it is | Host 9092 | Use for |
|---|---|---|---|
| **Payment lab overlay** | `infra/compose/compose.kafka.yml` (`apache/kafka`, KRaft, PLAINTEXT, RF1, 3 partitions on `lab.auditable-actions.v1`) | **Yes — this owns 9092** | Produce from Modulith outbox; KafkaIT; thin Event Lab; Lenses environment `payment-lab` |
| **Lenses CE demo** | Community Edition stack (`~/lenses-ce` or `/tmp/lenses-compose.yml`): HQ :9991, MCP :8000, sample topics (`telecom_italia_data`, vessels, …) | **Must not bind 9092** while the lab overlay is up | Operator playground + Lenses skills against *sample* data, labelled as playground |

If both try to publish `localhost:9092`, one fails. Lab overlay wins. CE demo Kafka stays on the internal `lenses` network only (no host 9092 / 8081). 8081 on the host is already taken by Keycloak in this lab.

## Lenses is a luneta

Lenses UI + MCP **look at** topics. They do not replace:

- transactional outbox
- `eventlab` consumer + unique `(consumer_group, event_id)`
- Testcontainers `*KafkaIT` (CI oracle)
- inject duplicate/poison in *our* domain
- the payment-order proof-of-delivery card

Do not add AKHQ, kafka-ui, lag charts, or a message browser in Nuxt. Open Lenses instead.

## MCP / Grok skills — when

User-global skills (installed under `~/.grok/skills/`, from `lensesio/agentic-engineering-for-apache-kafka`):

| Skill | When on **lab** cluster | When on **CE playground** |
|---|---|---|
| `kafka-consumer-lag` | After `eventlab-inspector` exists — diagnose lag on *our* group | Anytime on demo groups |
| `kafka-dlq-review` | After DLT `lab.event-lab.dlq.v1` exists | Demo connectors/DLT |
| `kafka-topic-audit` | **With the lab≠prod frame below** | Fine as production-style drill |
| `kafka-schema-review` | Skip in wave 1 (JSON envelope, no Schema Registry) | CE has Registry — playground only |
| `kafka-security-audit` | Will flag PLAINTEXT — expected, not a product defect | Same |
| `kafka-connector-review` / `kafka-perf-review` / ShadowTraffic | Not wave 1 | Playground |

Lenses MCP is **not** a CI oracle. Do not call it from Failsafe or Playwright.

Grok MCP server name: `Lenses` (stdio via `~/lenses-mcp/run-stdio.sh`, API key in `~/lenses-mcp/.env`). HTTP MCP on `:8000` is the CE sidecar; Event Lab coding uses the lab broker, not that demo.

## lab ≠ prod (read this before `kafka-topic-audit`)

Ostrożnie ze skillami Lenses na klastrze labu: **`kafka-topic-audit` będzie krzyczeć „RF=1 = critical”**.

Na single-node KRaft to **prawda operatorska** (w produkcji RF=1 = utrata danych przy padzie brokera) i **fałszywa lekcja produktowa**, jeśli nie opatrzysz jej ramką **lab ≠ prod**:

- Lab is one combined KRaft node on purpose (ADR 0002 D-2/D-3). RF1 + 3 partitions teach keys, ordering, groups, DLT — not HA.
- PLAINTEXT is the same: JWT guards HTTP; Kafka ACLs are a later profile, not wave 1.
- Unbounded or short retention findings on lab topics are teaching prompts, not tickets to “fix production”.
- Naming `{domain}.{entity}.{event}` vs `lab.auditable-actions.v1` is an accepted lab name, not a rename-the-product finding.

When the skill reports Critical on the lab cluster, the agent **must** classify each finding as:

1. **Lab-shaped (ignore as product work)** — RF1, single broker, PLAINTEXT, no Schema Registry in wave 1.
2. **Real lab bug** — topic missing, wrong partition count, DLT absent after E3, consumer group empty when Event Lab is on.
3. **Playground-only** — findings about `telecom_italia_data` and CE connectors.

Never open a payment-lab ticket to “set RF=3” without a dedicated multi-broker epic (not this roadmap).
