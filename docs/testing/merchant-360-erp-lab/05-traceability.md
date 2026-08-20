# 05 — Traceability FR → test

AT: [08-acceptance-tests.md](08-acceptance-tests.md). EP/BVA: [06](06-istqb-ep-bva.md). DT/ST/MR: [07](07-dt-st-uc-mr.md).

| FR | UC | AT | E2E / SEC | API PW | RA | Task | Prio |
|---|---|---|---|---|---|---|---|
| FR-M360-LIST | 02, 04 | 10 | E2E-001 | API-001, 004 | 010 | T02, T05 | P0 |
| FR-M360-Q | 01, 02 | 10 | E2E-021, 022, 032 | API-002, 010 | 011–017 | T02 | P0 |
| FR-M360-PAY-FILTER | 03 | 11 | E2E-025–028 | API-003 | 020–026 | T03, T09 | P0 |
| FR-M360-TABLE | 10–13 | 10 | E2E-020–052 | API-010 | — | T06–T08 | P0 |
| FR-M360-URL | 10 | 10 | E2E-030, 031 | — | — | T06 | P0 |
| FR-M360-SLIDE | 20 | 20 | E2E-060–063 | API-020 | — | T10 | P0 |
| FR-M360-FORM | 21 | 21 | E2E-070–073 | — | 030 | T11 | P0 |
| FR-M360-RBAC | 30 | 30 | SEC-010–014 | API-040 | 040 | T12 | P0 |
| FR-M360-ETAG | 31 | 31 | SEC-020 | API-041 | 050–055 | T13 | P0 |
| FR-M360-IMPORT | 40, 41 | 40 | E2E-080–085 | API-030 | 060–063 | T14 | P0 |
| FR-M360-KANBAN | 42 | 42 | E2E-090–094 | API-031 | (lifecycle existing) | T15 | P0 |
| FR-M360-TREE | 50 | 50 | E2E-100–104 | API-050 | 070 | T16 | P1 |
| FR-M360-SEARCH | 51 | 50 | E2E-110–112 | API-051 | 071 | T17 | P1 |
| FR-M360-CHART | 52 | 50 | E2E-120–121 | API-052 | — | T18 | P1 |
| FR-M360-CAL | 60 | 60 | E2E-130–131 | — | 080 | T19 | P2 |
| FR-M360-TL | 61 | 20 | E2E-132–133 | — | 081 | T19 | P1 |
| FR-M360-STEP | 62 | 60 | E2E-134–136 | — | — | T19 | P2 |
| FR-M360-EDITOR | — | 60 | E2E-140–141 | — | — | T19 | P2 cond. |
| FR-M360-EDITGRID | 63 | 60 | E2E-150–151 | API-060 | 090–091 | T20 | P1 |

P0 Fale 1–5 = epic Customer 360 analog na merchantach. P1/P2 nie blokują T01–T15.
