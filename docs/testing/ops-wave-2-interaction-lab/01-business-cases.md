# 01 — Business cases (Ops Wave 2)

Każdy BC ma właściciela epiku. Nie są to encje CRM. 10 feature’ów Wave 1 jest poza tym katalogiem.

| ID | Fala | Tytuł | Wartość | Ryzyko jeśli skip |
|---|---|---|---|---|
| BC-OPS-00 | 0 | Nauka na żywym Podman, nie na mocku WS | Wiarygodny curriculum SDET | Fałszywe E2E na `routeWebSocket` |
| BC-OPS-13 | 1 | Dwóch writerów, 412 + Your vs Latest | Integrity merchant | Lost update na `version` |
| BC-OPS-16 | 1 | Dirty form guard | Brak utraty telefonu | Ciche porzucenie PATCH-never-sent |
| BC-OPS-11 | 2 | Kolejka spraw support (nie payment status) | Ops queue | Drugi Kanban na tych samych kolumnach co M360 |
| BC-OPS-15 | 3 | Bulk assign partial success | ERP per-item | Jeden toast ukrywa 3 fail |
| BC-OPS-17 | 4 | Step-up PIN > €1000 | High-value refund | Dual-control bez drugiego czynnika |
| BC-OPS-12 | 5 | Live feed WebSocket | Push vs polling | Testy na mocku ramek |
| BC-OPS-19 | 5 | Notification center | Actionable inbox | Spam toast na każdy CAPTURE |
| BC-OPS-14 | 6 | Saved views + kolumny | Powrót do filtra po loginie | JWT w localStorage |
| BC-OPS-20 | 6 | Ctrl+K encje + RBAC | Szybki skok | Statyczne linki; wyciek cudzych merchantów |
| BC-OPS-18 | 7 | Tenant payment policy | BVA formularza | Slider bez kontraktu API |
| BC-OPS-21 | 8 | Locale EN/PL/SV | Format kwot/dat | Hardcode `"Save"` w POM |
| BC-OPS-22 | 9 | Evidence gallery | Przegląd załączników | Drugi Download Lab |

Poza BC: Kafka, PSP, Keycloak OTP, Customers, Revenue, payment Kanban, `page.route`.
