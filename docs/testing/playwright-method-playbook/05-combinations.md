# 05 — Zestawy technik

| Zestaw | Klasa | Dlaczego razem | Co ginie bez pary |
|---|---|---|---|
| DT + UC | `IsolationDtUc` | kto/kod + ścieżka UI | 403 managera = „tenant” |
| ST + DT | `LifecycleStDt` | krawędź + If-Match | 412 w złym stanie / tylko happy |
| UC + EP REST | `CreateUcEpRest` | 1 E2E + partycje BFF | 15 E2E na 400 |
| ST + DT 2 aktorów | `DualControlStDt` | CAPTURED + inny `sub` | „refund nie działa” |
| ST + DT nielegalne | `IllegalStDt` | fresh If-Match + zły from | 412/428 zamiast 422 |
| DT If-Match × akcja | `IfMatchActionMatrix` | cancel/capture nie tylko authorize | 428 tylko na authorize |
| EP + MR filtra | `MetamorphicListFilter` | wąski ⊆ szeroki minAmount | sam filtr happy |
| Nie łączyć | pairwise × ST × BVA | eksplozja | RA/unit |

Spec importuje zestaw i woła `app` / `api`. Nie drugi suite.
