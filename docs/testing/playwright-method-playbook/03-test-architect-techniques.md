# 03 — Test architect: dobór techniki

Pętla: ryzyko → BC → UC → technika → warstwa → dane (~104) → `test()`.

| Pytanie | Technika | Klasa |
|---|---|---|
| Kto / jaki zbiór? | DT | `MerchantAccessMatrix` (w tym `SCN-RO-01`) |
| Ten sam key, inne body? | DT | `IdempotencyMatrix` (brak/pusty key na BFF → 201 + mint) |
| Akcja × If-Match? | DT | `IfMatchActionMatrix` — cancel/capture, 4 wiersze |
| Legalna krawędź stanu? | ST | `PaymentStatusMachine`, `MerchantStatusMachine` |
| Granice amount/reference/If-Match? | EP/BVA | `AmountPartitions`, `OrderReferencePartitions`, `MerchantReferencePartitions`, `IfMatchAndKeyBoundaries` — REST |
| Relacja dwóch wykonań? | metamorphic | `methods/metamorphic/` — REST (MR-IDEM/UNIQ/ETAG/FILTER/ISO) |
| Kroki aktora + nie-oracle? | UC | `GuestToLoginJourney`, `CreateOrderJourney`, tenant.admin create (JWT) |
| Mode × outcome checkout? | pairwise | `CheckoutModeOutcome` (świat CPL) |
| Overlay / IPv6 / dwa logouty? | EG | `OverlayAndIpv6` + 2 asercje w specach |

Metamorphic (relacja, nie pojedynczy expected):

| ID | Relacja | Oracle |
|---|---|---|
| MR-IDEM | ten sam key + body | to samo `paymentOrderId`, 200 |
| MR-UNIQ | inne key i ref | dwa 201, różne ID |
| MR-ETAG | GET bez zapisu; potem authorize | ETag stały, potem się zmienia |
| MR-FILTER | `minAmount` węższy ⊆ szerszy | każdy wiersz pierwszego ∈ drugi |
| MR-ISO | lista tenant.admin ⊆ platform | Beta ∉ tenant.admin |

Minimal covering: nie 5 ról × 15 czasowników w Playwright (to RA). E2E izolacji = trzy wiersze: platform, tenant.admin, manager.

Szczegóły UC/HTTP: `live-pom-wave-2/07` i `09`. Karty GWT: plan sesji §P.
