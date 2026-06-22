# Specs Analysis — Payment Quality Engineering Lab

Ten katalog zawiera dokumentację analityczną i podsumowania dla każdej specyfikacji Kiro.
Każdy spec ma własny podfolder z plikami analizy, decyzji i statusu.

## Struktura

```
docs/specs-analysis/
├── README.md                          ← ten plik — indeks i roadmapa
├── 01-backend-authority-refactor/
│   └── README.md                      ← analiza, decyzje, status, lessons learned
├── 02-iam-roles-and-keycloak-login/
│   └── README.md
├── 03-payment-operations-dashboard/
│   └── README.md
└── 04-tenant-model-and-isolation/
    └── README.md
```

Pliki źródłowe spec (requirements.md, design.md, tasks.md) pozostają w:
`.kiro/specs/{feature-name}/`

## Roadmapa spec

```
001-012  (specs/)          — Archiwum: legacy specs (pre-Kiro, fazy 1–12)
    │
    ▼
[KIRO SPECS — aktywna roadmapa]
    │
    ├── ✅ backend-authority-refactor    — fundament typed authorities + azp validation
    ├── ⏳ iam-roles-and-keycloak-login  — Keycloak composite roles + OIDC login + role-aware UI
    ├── 🔲 tenant-model-and-isolation    — tenant jako encja DB + egzekwowanie izolacji
    ├── ~85% payment-operations-dashboard — dashboard operacyjny + HTTP learning surface
    │
    └── [przyszłe] tenant-management / payment-order-notes / user-management
```

## Status

| # | Spec | Status | Opis |
|---|---|---|---|
| 1 | `backend-authority-refactor` | ✅ Ukończony | Typed authority catalog, explicit converter allowlist, azp validator |
| 2 | `iam-roles-and-keycloak-login` | ⏳ Gotowy do implementacji | Composite roles Keycloak, OIDC login, role-aware dashboard |
| 3 | `payment-operations-dashboard` | 🔶 ~85% (opcjonalne testy + cleanup) | Dashboard operacyjny, HTTP learning panels |
| 4 | `tenant-model-and-isolation` | 🔲 Spec gotowa, implementacja nierozpoczęta | Tenant module, Flyway FK, enforcement, PBT P1–P6 |
| 5 | `user-management` | 🔲 Spec gotowa (req+design+tasks), implementacja nierozpoczęta | Keycloak Admin API façade, moduł `iam`, RBAC users, PBT P1–P6 |
| 6 | `audit-log-dashboard` | 🔲 Spec gotowa (req+design+tasks), implementacja nierozpoczęta | Spring Modulith events, moduł `audit`, Flyway audit_event, PBT P1–P6 |
| 7 | `deterministic-seed-and-test-isolation` | 🔲 Spec gotowa (req+design+tasks), implementacja nierozpoczęta | Profile-gated seeder, moduł `testing`, feature-flagged reset, PBT P1–P6 |
