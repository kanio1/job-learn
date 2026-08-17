# 03 — Audit and event-publication learning rows

**What to build:** The same learning seed also inserts 10 000 audit events (with representative before/after JSON) and 10 000 synthetic event-publication rows (success / retry / incomplete). Publication rows are JDBC inserts, not live application events. After this ticket the SMALL truth table in the spec is complete.

**Blocked by:** 01 — Learning seed HTTP + SMALL payment world

**Seams:** Spring Testcontainers (counts, mix, determinism); REST Assured only to the extent the learning-seed truth body now includes audit and publication metrics

**Status:** ready-for-agent

- [ ] After learning seed: 10 000 `audit_event` rows; actions from the agreed merchant/payment set; outcomes only SUCCESS/DENIED/FAILED; `tenant_id` skewed like payments; a representative subset has both `before_state` and `after_state`
- [ ] After learning seed: 10 000 `event_publication` rows; 9000 completed first attempt, 700 one retry, 200 multiple retries, 100 incomplete (`completion_date` null)
- [ ] No `ApplicationEvent` publish / listener run is required to produce those publication rows
- [ ] Truth document audit and publication keys match; incomplete publications = 100; two consecutive seeds stay identical
- [ ] Learning seed still clears `audit_event` and `event_publication` before insert so leftover app events cannot pollute counts
- [ ] Existing `/api/test/seed` behaviour unchanged; no payment domain changes
