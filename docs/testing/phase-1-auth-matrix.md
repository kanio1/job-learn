# Phase 1 Authorization Matrix

| Actor / Authority | Create | Read by ID | List | Activate | Suspend | Status |
|---|---:|---:|---:|---:|---:|---:|
| Unauthenticated | 401 | 401 | 401 | 401 | 401 | 200 |
| Malformed/expired/invalid JWT | 401 | 401 | 401 | 401 | 401 | 200 |
| `merchant.denied` no merchant roles | 403 | 403 | 403 | 403 | 403 | 200 |
| `platform.operator` read only | 403 | 200 | 200 | 403 | 403 | 200 |
| `platform.operator` create only | 201 valid data | 403 | 403 | 403 | 403 | 200 |
| `platform.operator` status-update only | 403 | 403 | 403 | 200 valid DRAFT | 200 valid ACTIVE | 200 |
| `platform.operator` full roles | 201 valid data | 200 | 200 | 200 valid DRAFT | 200 valid ACTIVE | 200 |

Invalid business state still returns business errors after authorization succeeds, for example `409 invalid_transition` for activating an ACTIVE or SUSPENDED merchant.
