# 05 — Traceability FR → test

| FR | UC | E2E / SEC | API PW | RA | Task | Prio |
|---|---|---|---|---|---|---|
| FR-OPS-ETAG | 13 | SEC-020 | API-010 | 050–055 | T01, T02 | P0 |
| FR-OPS-CONFLICT | 14, 15 | E2E-130–132, SEC-021/022 | — | — | T03, T20 | P0 |
| FR-OPS-DIRTY | 16, 17 | E2E-160–164 | — | — | T04 | P0 |
| FR-OPS-CASE | 11 | — | API-030 | 110–122 | T05, T06 | P0 |
| FR-OPS-KANBAN | 12, 18, 19 | E2E-110–114 | API-030 | — | T07, T21 | P0 |
| FR-OPS-BULK | 20, 21 | E2E-150–153 | API-031 | 150–154 | T08 | P0 |
| FR-OPS-PIN | 22, 23 | E2E-170–176 | API-040 | 170–179 | T09 | P0 |
| FR-OPS-WS | 24–26 | E2E-120–125 | API-020 | 125–127 | T10, T11, T22 | P0 |
| FR-OPS-NOTIF | 27 | E2E-190–194 | — | 190–193 | T12 | P0 |
| FR-OPS-VIEWS | 28–30 | E2E-140–147 | API-050 | 140–143 | T13, T14 | P0 |
| FR-OPS-SEARCH | 31–33 | E2E-200–203, SEC-040/041 | API-060 | — | T15 | P1 |
| FR-OPS-POLICY | 34, 35 | E2E-180–184 | — | 180–185 | T16, T17 | P1 |
| FR-OPS-I18N | 36 | E2E-210–213 | — | — | T18 | P1 |
| FR-OPS-GALLERY | 37, 38 | E2E-220–224 | API-070 | — | T19 | P2 |

P0 Fale 1–5 = najwyższy learning (concurrent, dirty, kanban, bulk, PIN, WS, notif, views). P1/P2 nie blokują T01–T12.

M360 overlap: FR-M360-ETAG ⊂ FR-OPS-ETAG (implement once). FR-M360-SEARCH ⊂ FR-OPS-SEARCH (E9 skip core jeśli T17 live). FR-M360-KANBAN **disjoint** od FR-OPS-KANBAN.
