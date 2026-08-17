# 01 — Learning seed HTTP + SMALL payment world

**What to build:** An explicit test-only operation loads a replace (not overlay) learning database: known lab tenant/merchant doors plus a synthetic crowd, 10 000 payments, 28 000 legal status-history rows, and a truth document for those counts. The 104-row deterministic seed stays the only payload of the existing seed/reset operations. Usual lab login still sees `TENANT_ALPHA` / `MERCHANT_ALPHA_001` as the bulk of payments. Nothing loads on boot or in prod.

**Blocked by:** None — can start immediately.

**Seams:** REST Assured HTTP; Spring Testcontainers (determinism + counts)

**Status:** ready-for-agent

- [ ] `POST /api/test/seed-learning` returns 200 when testing support is on and the profile is not prod; body operation is `seed-learning`, status `completed`, plus truth for payment-world metrics
- [ ] Same gates as existing test seed: 404 when testing is off; 404 on prod even if the testing flag is true
- [ ] Existing `POST /api/test/seed` still returns 200 and still yields 104 payment orders; `POST /api/test/reset` unchanged
- [ ] Optional `profile=SMALL` or omitted; any other profile is 400
- [ ] After learning seed: 5 tenants, 20 merchants, 10 000 payments, 28 000 history rows; status counts 6000 captured / 1200 refunded / 800 cancelled / 800 authorized / 400 expired / 800 created; 5500 payments on `TENANT_ALPHA`; ~55% on `MERCHANT_ALPHA_001`
- [ ] Every payment history is a legal chain only (`CREATED`, `CREATED→AUTHORIZED`, `…→CAPTURED`, `…→REFUNDED`, `CREATED→CANCELLED`, `CREATED→AUTHORIZED→EXPIRED`)
- [ ] Two consecutive learning seeds produce identical truth, identical payment ids for index 0 and 9999, and identical history lengths for those ids
- [ ] Inserts use JDBC from the testing module; payment public seed capability is not extended; `DeterministicDataset` / 104 fixtures are not modified
- [ ] RLS lab’s two existing rows are present after a learning seed
- [ ] No dual-control refund approval rows; timestamps span 2025-01-01 through 2026-08-15; currencies only PLN/EUR/USD
