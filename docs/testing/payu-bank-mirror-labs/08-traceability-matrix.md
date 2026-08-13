# 08 — Macierz śledzenia (FR → testy)

Priorytet = dalszej implementacji `designed`. Pokrycie: `existing-*` | `designed` | `docs-only`.

## Session

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-S01 inspector | E2E-010/011, API-023/030, EP-014 | existing-pom partial | P0 |
| FR-S02 trzy światy cookies | E2E-024, API-032/033, guest | existing-pw + existing-pom | P0 |
| FR-S03 idle | E2E-020–025, BVA-010, DT-02 | existing-pom 121s + Unlock; designed TTL-1 | P0 |
| FR-S04 logout | E2E-021/022, UC-01 | existing-pom `/login`; designed deep link | P0 |
| FR-S05 concurrent | E2E-030, API-020–022 | existing-pom cienko | P1 |
| FR-S06 CSRF | E2E-040–042, API-010–014, DT-01 | existing-pom fail; designed happy + kontrast | P1 |
| FR-S07 guest | E2E-005, session-guest | existing-pom | P0 |
| FR-S08 no JWT storage | E2E-012, NET-104 | existing-pom | P0 |
| C-03 flag off | API-001–007, E2E-003, DT-05 | existing-it Spring; designed FE/BFF | P0 |

## Visual

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-V01 page | E2E-100 | existing-pw | P0 |
| FR-V02 tiles | VIS-001–006, E2E-110 | existing-pw | P0 |
| FR-V03 dark | VIS-020/021 | existing-pw tile; designed colorScheme | P1 |
| FR-V04 full+mask | VIS-010 | designed | P1 |
| FR-V05 break | VIS-040 | existing-pw tagged | P1 |
| FR-V06 anti-case | VIS-090 | docs-only | P0 |
| NFR-03 próg | playwright.config 0.02 | existing | — |

## Network

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-N01 503-retry | NET-001/100, E2E-200/201, API-150–152 | existing-pw + existing-pom; designed TTL | P0 |
| FR-N02 abort/offline | NET-002/102, E2E-210/211 | existing-pw abort; designed offline | P0 |
| FR-N03 lie | NET-003, E2E-220, UC-02 | existing-pw UI; designed fulfillment oracle | P0 |
| FR-N04 strip headers | NET-004/005 | designed | P1 |
| FR-N05 HAR | NET-006/103, E2E-230 | existing-pw fixture | P1 |
| FR-N06 CORS creds | API-140/141, EP-212 | designed | P1 |
| FR-N07 split | DT-03, README suites | existing (oba suite) | P0 |

## PayU mirrors (Wave 3 + review)

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-P01 GET body | API-100–102, EP-100, EG-06 | existing-ra GET+body; designed POST regresja | P1 |
| FR-P02 lang | API-103/131/132, E2E-300, BVA-040 | existing-ra Location; designed hosted copy | P1 |
| FR-P03 refund notify | API-120–124, DT-07, ST-04 | existing-ra REFUNDED+409+count=1 | P0 |
| FR-P04 expiry UI | E2E-310, API-130 | existing-pom + existing-ra | P1 |
| FR-P05 iframe | E2E-320/321, NET-010, UC-03 | existing-pw mocked; designed POM live | P1 |
| FR-P06 grants | API-110–112, E2E-330 | existing-ra trusted_merchant; designed UI panel | P1 |

## Bank-like

| Wymaganie | ID | Pokrycie | Prio |
|---|---|---|---|
| FR-B01/B06 step-up | API-200–203, E2E-400–402, DT-04, BVA-011 | existing-ra; designed UI | P2 |
| FR-B02 statements | API-210–213, E2E-410–412, NET-008, EP-301 | existing-pw PDF mocked + POM CSV; designed live PDF + RA bytes | P1 |
| FR-B03 disputes | API-220–228, E2E-420/421, BVA-020 | existing-ra 415/413/200; designed UI | P1 |
| FR-B04 maker-checker | API-230–236, E2E-430/431, ST-02, UC-04 | existing-ra + existing-pom fill | P0 |
| FR-B05 consent | API-240–250, E2E-440/441, DT-06, ST-03 | existing-ra header/owner; designed 429 + UI | P1 |

## Constraints / NFR

| ID | Test | Pokrycie |
|---|---|---|
| C-02 no mobile | brak project iPhone | docs-only |
| C-04 trzy tokeny | hub copy + OAuth vs Keycloak vs HMAC | existing hub + RA |
| C-05 POM no fulfill | E6 review / DT-03 | existing-pom 503 |
| C-06 visual CI image | VIS notes | process |
| C-07 realm freeze | brak zmian realm | docs-only |
| C-08 iframe same-origin | E2E-320 | existing-pw |
| C-09 no JWT in state | E2E-012 | existing-pom |
| NFR-01 correlation | API-001 echo | existing-it |
| Flyway V15/V16 | RA/IT migrate | existing |
| Modulith | API-009 | existing |

## Review-fix traceability

| Finding | ID | Pokrycie |
|---|---|---|
| Idle Unlock bez re-auth | E2E-021/022, UC-01, EG-01 | existing-pom URL; designed persistence |
| PDF BFF as text | API-212, E2E-411/412, EP-301 | existing-pw mocked |
| Refund bez zmiany statusu | API-120/121, DT-07 | existing-ra |
| BFF ignoruje flagę | API-004–007, DT-05 | designed FE |
| TPP query only | API-242/248/250, GAP-T09 | existing-ra header; designed UI/429 |
| CSRF bez sesji | API-010–015 | session required in code; tests partial |
| Maker re-approve / owner revoke | API-234/244 | existing-ra |
| POM DOM hack approve | E2E-430 | existing-pom fill |
| GET `?lang=` psuje id | API-103 | existing-ra |
| Multipart servlet vs 2MB | API-223, BVA-020 | existing-ra 413 |
| 503 leak across tests | API-152, BVA-031 | designed |

## Gates

| Gate | Warunek |
|---|---|
| REST-MULTIPART-01 | evidence 211–223 w RA **jest**; gate docs może wymagać `apps/api-tests` — nie mylić z brakiem 413 |
| REST-REDIRECT-01 | CPL — E4 nie otwiera ponownie |
| Keycloak ACR | **nie** w MRL; step-up = header lab |
| Designed z tej mapy | **nie implementować** dopóki nie ma osobnego zadania |

## Heatmapa (skrót as-built)

| Obszar | RA/IT | PW mocked | POM | Designed leftover |
|---|---|---|---|---|
| Flag Spring | tak | — | — | FE flag project |
| Idle + Unlock | — | hosted no overlay | clock + `/login` | TTL-1, overlay merchants |
| CSRF | — | — | 403 | happy, kontrast merchant |
| Visual tiles | — | tak | — | full-page |
| 503 / HAR / abort | — | tak | 503 | offline, CORS, TTL |
| GET-body / lang / refund | tak | widget, idle-absent | expiry | inspector refund UI |
| Step-up / multipart | tak | — | — | UI |
| Maker-checker | tak | — | fill+click | 409 w UI |
| Consent/TPP | tak | — | — | 429, UI header |
| PDF bytes | — | fulfill | CSV | live BFF |
