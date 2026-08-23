# Out of scope — Kafka E6 observability dashboard

Status: rejected for this lab (2026-08-23, ADR 0002 iteration 3)

Do **not** implement:

- Consumer lag heatmaps / `records-lag-max` charts in Nuxt
- ECharts (or any new chart dependency) for Kafka
- AdminClient → BFF metrics API as a product surface
- LIVE/PAUSED/Follow live tables driven by Kafka
- Load generator 1–10k / `lab.event-lab.load.v1` as an Event Lab feature
- AKHQ / kafka-ui beside Lenses
- Schema Registry, Kafka Streams, SCRAM, compacted changelog in wave 1–5 of this roadmap

**Why:** Lenses UI + MCP already teach operator observability. Building it again is a fake KPI-adjacent dashboard (standing non-goal) and duplicates the telescope.

Revisit only with a new ADR if the lab runs **two Spring instances** or a multi-broker cluster on purpose.
