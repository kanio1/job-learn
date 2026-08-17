# 02 — Checkout learning rows on the same seed

**What to build:** The same learning seed also fills checkout protocol tables with a deterministic, constraint-legal population so SQL/DQ exercises can reconcile sessions, events, fulfillments, and anomalies. Checkout HTTP APIs and pollers are not used to create the rows. Session status spelling follows the schema (`CANCELED` on the session).

**Blocked by:** 01 — Learning seed HTTP + SMALL payment world

**Seams:** Spring Testcontainers (counts, scenarios, determinism); REST Assured only to the extent the learning-seed truth body now includes checkout metrics

**Status:** ready-for-agent

- [ ] After `POST /api/test/seed-learning`: 2000 checkout sessions, 5000 events, 1950 fulfillments, 50 anomalies
- [ ] Fifty sessions have no fulfillment (missing-fulfillment business anomaly); those fifty are reflected in `checkout_anomaly`
- [ ] Index-driven mix includes happy completed, canceled, expired, refunded, event retry, 503 ACK, duplicate event, and wrong fulfillment — all rows satisfy CHECK/FK/UNIQUE
- [ ] Truth document checkout keys match those counts; two consecutive seeds stay identical including checkout identities
- [ ] Existing 104-row seed still does not populate checkout (wipe-only, as today)
- [ ] No checkout production behaviour changes
