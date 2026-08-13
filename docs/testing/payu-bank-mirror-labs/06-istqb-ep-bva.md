# 06 — Equivalence partitioning i BVA

Design only dla wierszy `designed`. Wartości **z kodu** 2026-08-13.

## EP-MRL — Session / CSRF / flag

| ID | Parametr | Partycje | Oczekiwanie | Pokrycie |
|---|---|---|---|---|
| EP-MRL-001 | FE flag | true / `'false'` / unset | lab on / off / **on** (default) | designed off-project |
| EP-MRL-002 | Spring flag | true / false | 200 vs 404 + brak beana | existing-it |
| EP-MRL-010 | CSRF token | absent / valid match / invalid / empty header | 403 / 2xx / 403 / 403 | fail existing-pom |
| EP-MRL-011 | Auth cookie BFF | none / valid / cleared po Unlock | 401 / 200 / 401 | Unlock existing-pom |
| EP-MRL-012 | Page class | `/admin/**` / `/psp/checkout/**` / `/login` | idle applies / **nie** / n/a | hosted existing-pw |
| EP-MRL-013 | storageState | Keycloak / empty / other role | admin / login / 403 UI | guest existing-pom |
| EP-MRL-014 | JS cookie view | HttpOnly `nuxt-session` vs non-HttpOnly `mrl-csrf` | invisible / visible | partial POM |
| EP-MRL-015 | Unlock action | logout+clear vs sam navigate `/login` | `/login` trwałe / bounce na admin | existing-pom logout |

## BVA-MRL — Idle, progi, pliki, TPP

| ID | Parametr | Granice | Expect | Pokrycie |
|---|---|---|---|---|
| BVA-MRL-010 | idle 120s | 119s / 120s / 121s | no lock / lock (tick 1s) / lock | 121 existing-pom; 119 designed |
| BVA-MRL-011 | step-up amount | 9999 / 10000 / 10001 | 200 no header / 403 / 403 | 10000 existing-ra; 9999 designed |
| BVA-MRL-012 | `stepUpUntil` na approval | 9999 / 10000 | pole absent / ISO now+300s | 10000 existing-ra |
| BVA-MRL-020 | evidence size | 2 MiB / 2 MiB+1 / 5MB+ (servlet) | 200 / 413 app / 413 servlet | +1 existing-ra |
| BVA-MRL-021 | validityUntil hosted | now-1 / now / now+1 | expired UI / implementer / valid | EXPIRED_LINK existing-pom |
| BVA-MRL-030 | TPP rate | 30 / 31 w 60s | 200 / 429 | designed |
| BVA-MRL-031 | 503 TTL | 9s / 11s po pierwszym 503 | 2nd=200 / nowy 1st=503 | designed |
| BVA-MRL-040 | lang | pl / en / xx / omitted | `?lang=pl` / `?lang=en` / brak / brak | pl existing-ra |

Clock: `fastForward(ttlMs)`, nie `sleep(ttl)`.

## EP-MRL — HTTP PayU / OAuth

| ID | Parametr | Partycje | Expect | Pokrycie |
|---|---|---|---|---|
| EP-MRL-100 | GET body | empty / `{}` / Transfer-Encoding | 200 / 403 get_with_body / 403 | `{}` existing-ra |
| EP-MRL-101 | OAuth Content-Type | form / json / missing | 200 / 401 / 401 | existing-ra analog |
| EP-MRL-102 | grant_type | client_credentials / trusted_merchant+ids / trusted_merchant braki / password / omitted | 200 / 200 / 401 / 401 / 401 | trusted existing-ra |
| EP-MRL-103 | lang | patrz BVA-040 | | |
| EP-MRL-104 | session status przy refund | COMPLETED / REFUNDED / inne | 200 REFUNDED / 409 / 409 | COMPLETED+2nd existing-ra |
| EP-MRL-105 | HMAC notify | valid / invalid / missing | 202 / 400 / 400 | designed MRL (CPL existing) |

## EP-MRL — Visual / network

| ID | Parametr | Partycje | Expect | Pokrycie |
|---|---|---|---|---|
| EP-MRL-200 | color scheme | light / dark tile | dwa goldens | dark tile existing |
| EP-MRL-201 | break toggle | off / on | pass / fail tagged | existing-pw |
| EP-MRL-210 | 503 counter | 1st / 2nd / 3rd same TTL | 503 / 200 / 200 | 1–2 existing |
| EP-MRL-211 | route mode | fulfill mocked / live wait | oba UI | existing both |
| EP-MRL-212 | CORS Origin | `http://localhost:3000` / other / `*` | credentials OK / fail / invalid | designed |
| EP-MRL-213 | suite | mocked fulfill / POM fulfill | allowed / **forbidden** | process C-05 |

## EP-MRL — Statements / disputes / consent / TPP token

| ID | Parametr | Partycje | Expect | Pokrycie |
|---|---|---|---|---|
| EP-MRL-300 | format | csv / pdf / xml/other | csv / pdf binary / csv (else) | csv existing; pdf mocked |
| EP-MRL-301 | PDF transport | Spring bytes / BFF text / BFF arrayBuffer | `%PDF-` / korupcja / `%PDF-` | arrayBuffer existing mocked |
| EP-MRL-310 | content-type evidence | txt / png / jpeg / pdf / csv / exe / empty type | 200 / 200 / 200 / 200 / 200 / 415 / 415 | txt+exe existing |
| EP-MRL-311 | filename | `note.txt` / `a/b.txt` / `..\x` | 200 / 400 / 400 | designed path |
| EP-MRL-320 | approval status | PENDING / APPROVED / unknown id | approve 200 / 409 / 404 | PENDING+APPROVED existing |
| EP-MRL-321 | checker vs maker | same subject / different | 403 / 200 | existing-ra |
| EP-MRL-330 | consent | none / GRANTED / REVOKED | 403 / 200 TPP / 403 | grant+revoke existing |
| EP-MRL-331 | TPP token source | query / header / both (header wins) / none | 200 / 200 / header value / 403 | query+header existing |
| EP-MRL-332 | revoke actor | owner / other JWT | 200 / 403 owner_mismatch | existing-ra |
| EP-MRL-340 | BFF TPP session | logged-in / anonymous | oba OK jeśli flaga (TPP nie wymaga dashboard) | designed |
