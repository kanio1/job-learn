# 08 — Acceptance tests

Obserwowalne, biznesowe. Gherkin skrócony. Mapowanie na FR / UC / TC.

Pokrycie: **designed**. Agent implementuje TC, nie osobny framework AT.

---

## AT-M360-10 — Serwerowa tabela merchantów

**Given** platform.admin na żywym `--app`  
**When** ustawia status ACTIVE, q unikalnego merchanta, sort Updated, strona  
**Then** GET `/api/merchants` ma te query; tabela pokazuje tylko trafienia; Back przywraca URL  
**FR:** FR-M360-LIST, Q, TABLE, URL  
**TC:** E2E-020…032, API-001, RA-010…017  
**Nie przechodzi gdy:** sort tylko w przeglądarce; count z `content.length` pierwszej strony

---

## AT-M360-11 — Payment list nie kłamie statusem

**Given** manager na `MERCHANT-Wn` z orderem CAPTURED  
**When** filtr CAPTURED i sort Amount  
**Then** 200, wiersze CAPTURED, query `sort=amountMinor`  
**FR:** FR-M360-PAY-FILTER  
**TC:** E2E-025…026, RA-020…022  
**Nie przechodzi gdy:** backend nadal przyjmuje tylko `CREATED`

---

## AT-M360-20 — Merchant 360 panel

**Given** merchant na liście  
**When** Open → dialog → Escape  
**Then** GET detail 200; dialog hidden; fokus nie uwięziony; route listy bez zmiany  
**FR:** FR-M360-SLIDE  
**TC:** E2E-060…063, API-020

---

## AT-M360-21 — Create w boundach produktu

**Given** platform.admin  
**When** reference 3–64 + tenantReference  
**Then** 201 i wiersz; 2 znaki = brak POST; duplikat 409  
**FR:** FR-M360-FORM  
**TC:** E2E-070…073, BVA-040…045

---

## AT-M360-30 — Readonly nie mutuje

**Given** `readonly.user`  
**When** otwiera registry i woła POST activate przez BFF  
**Then** UI 0 Activate/Create; HTTP 403  
**FR:** FR-M360-RBAC  
**TC:** SEC-010, API-040, RA-040

---

## AT-M360-31 — Stale merchant jest 412

**Given** dwóch adminów, ten sam merchant, ETag vN  
**When** B zapisze, A wyśle If-Match vN  
**Then** 412; UI Reload; GET pokazuje wersję B  
**FR:** FR-M360-ETAG  
**TC:** SEC-020, RA-050…053  
**Nie przechodzi gdy:** 409 użyte jako lock

---

## AT-M360-40 — Import nie brudzi UK przed commit

**Given** valid.csv i duplicate.csv  
**When** preview potem commit valid  
**Then** preview bez INSERT; commit widać w GET; duplikat rejected; drugi commit 409  
**FR:** FR-M360-IMPORT  
**TC:** E2E-080…085, RA-060…063

---

## AT-M360-42 — Kanban to lifecycle

**Given** CREATED na worker merchancie  
**When** Move to AUTHORIZED  
**Then** POST authorize 200 If-Match; po reload karta w AUTHORIZED; nielegalny move 4xx + rollback  
**FR:** FR-M360-KANBAN  
**TC:** E2E-090…094, API-031

---

## AT-M360-50 — Tree / search / chart z API

**Given** platform.admin  
**When** expand Alpha; Ctrl+K unique ref; otworzy payments summary  
**Then** treeitem dzieci; search GET last query; legend = `byStatus` JSON  
**FR:** FR-M360-TREE, SEARCH, CHART  
**TC:** E2E-100…121, API-050…052

---

## AT-M360-60 — Daty i wizard (Fala 7)

**Then** calendar/`expiresAt` z `page.clock`; stepper nie POST-uje pustego amount; inline PATCH name + If-Match  
**FR:** FR-M360-CAL, STEP, EDITGRID  
**TC:** E2E-130…151

---

## Macierz AT → fala

| AT | Fala | Prio |
|---|---|---|
| 10, 11 | 1–2 | P0 |
| 20, 21 | 3 | P0 |
| 30, 31 | 4 | P0 |
| 40, 42 | 5 | P0 |
| 50 | 6 | P1 |
| 60 | 7 | P2 |
