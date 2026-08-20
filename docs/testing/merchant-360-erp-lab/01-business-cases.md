# 01 — Business cases (Merchant 360)

Każdy BC ma właściciela fali. Nie są to encje CRM. Flow PM: [00-business-flows.md](00-business-flows.md).

| ID | Fala | Tytuł | Wartość | Ryzyko jeśli skip |
|---|---|---|---|---|
| BC-M360-00 | 0 | Nauka na żywym Podman, nie na mocku | Wiarygodny curriculum SDET | Fałszywe poczucie E2E |
| BC-M360-01 | 1 | Indeksowany list query tenant×status×updated | Registry >50 wierszy | Seq scan / zły porządek stron |
| BC-M360-02 | 1 | Paginowany kontrakt merchant list | ERP paging | Breaking overview/POM |
| BC-M360-03 | 1 | Uczciwy filter statusu płatności | Kanban/tabela | UI pokazuje CAPTURED, API 400 |
| BC-M360-10 | 2 | Filtry w URL + Back | Operator workflow | Utrata kontekstu |
| BC-M360-11 | 2 | Bulk activate DRAFT | Onboarding | Klik po jednym; partial fail niejasny |
| BC-M360-20 | 3 | 360 bez zmiany route | Gęstość ERP | Extra nawigacja; zły fokus |
| BC-M360-21 | 3 | Create na prawdziwych boundach | ISTQB+Zod | Fikcyjny NIP bez kolumny DB |
| BC-M360-30 | 4 | RBAC kolumn + 403 | Compliance | „Schowaliśmy przycisk” |
| BC-M360-31 | 4 | Dwa operatorzy, 412 | Integrity | Lost update na `version` |
| BC-M360-40 | 5 | Preview CSV przed INSERT | Bezpieczny import | Śmieci w UK reference |
| BC-M360-41 | 5 | Pipeline = lifecycle płatności | CRM Kanban bez Deals | Fałszywy board |
| BC-M360-50 | 6 | Drzewo tenant→merchant | BSS nawigacja | Platform gubi kontekst |
| BC-M360-51 | 6 | Ctrl+K na żywych encjach | Szybkie skoki | Tylko nav, nie dane |
| BC-M360-52 | 6 | Chart = summary API | Interview „pixels vs data” | Fake KPI (zakaz AGENTS) |
| BC-M360-60 | 7 | Kalendarz expiresAt | Ops daty | Zależność od „dziś” |
| BC-M360-61 | 7 | Timeline = audit+history | Śledzenie zmiany | Rozjazd warstw |
| BC-M360-62 | 7 | Wizard create payment | State-transition | Double submit |
| BC-M360-63 | 7 | Inline displayName | Editable grid | PATCH bez If-Match |

Poza BC: settlement, PSP, Customers table, Revenue column, `page.route` race.
