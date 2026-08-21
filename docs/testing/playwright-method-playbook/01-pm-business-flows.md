# 01 — Product manager: po co ten flow

Sukces operatora to **pieniądze i izolacja**, nie „strona się otworzyła”.

| Flow | Wartość | Nie-oracle (fałszywy sukces) |
|---|---|---|
| Gość / Sign out | nikt bez sesji nie widzi rejestru | pusta tabela 200; Sign out = Keycloak `end_session` |
| Merchant create | unikalny byt w tenancie | sam `innerText` bez GET |
| Tenant vs RBAC | tenant nie widzi Bety; manager bez read to **nie** izolacja | 403 managera nazwane „tenant” |
| Create order | jedno zlecenie, replay nie dubluje | CPL `continueUrl` / hint `status=` |
| Authorize/capture | kolejność + ETag | drugi ekran ze `"v99"` przechodzi |
| Dual-control | maker ≠ checker | „refund 200” od tego samego `sub` |
| Checkout lab | fulfillment | query `status=success` bez Approve |

Persony i seed UUID: `docs/testing/live-pom-wave-2/09-core-domain-flows.md`.  
Dwa światy produktowe: operator `payment_orders` vs Checkout Protocol Lab — nie w jednym TC.

Milestone-PW na tym samym rejestrze: [Merchant 360 flows](../merchant-360-erp-lab/00-business-flows.md) (znajdź / 360 / RBAC / import) i [Ops Wave 2 flows](../ops-wave-2-interaction-lab/00-business-flows.md) (412 conflict, support queue, PIN, WS). Co para daje i czego uczą E2E vs REST: [m360-ops-wave-2-value-and-learning.md](../m360-ops-wave-2-value-and-learning.md).

GAP zamknięte w M5: UI `tenantReference` dla platform admin; notes `SUPPORT_AGENT` ma liście JWT zgodne z `rbacMatrix`.

Otwarte (nie zmieniamy produktu bez decyzji): BFF `payment-orders/index.post.ts` nadaje `Idempotency-Key`, gdy klient go nie przyśle — SCN-PAY-04/05 to 201 na BFF, nie 400 ze Springa.
