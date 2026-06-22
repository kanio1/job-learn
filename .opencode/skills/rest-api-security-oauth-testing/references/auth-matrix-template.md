# Authorization Matrix Template

| Actor / Principal | Resource | Role / Scope | Expected HTTP | Notes |
|---|---|---|---|---|
| Merchant A | Own payment | payments:read | 200 | |
| Merchant A | Merchant B payment | payments:read | 403/404 | Contract decision |
| Anonymous | Any protected payment | none | 401 | |
