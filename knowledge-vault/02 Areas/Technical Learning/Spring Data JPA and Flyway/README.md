---
type: moc
status: draft
area: Spring Data JPA and Flyway
date: 2026-05-21
tags:
  - spring-data-jpa
  - flyway
  - postgresql
  - backend-testing
---

# Spring Data JPA and Flyway - MOC

Ta ścieżka łączy kod Java z realnym schematem PostgreSQL. SDET musi umieć sprawdzić, czy encje, repozytoria i migracje razem chronią dane.

## Lekcje

| Lesson | Topic | Repo connection | Test risk |
|---:|---|---|---|
| 1 | Entity to table mapping | `Merchant` -> `merchants` | Java i DB nie mówią tego samego |
| 2 | Primary keys and UUIDs | `merchantId` | niestabilna identyfikacja danych |
| 3 | Columns and nullability | merchant fields | null/blank mismatch |
| 4 | Unique constraints | `normalized_reference` | duplicate data |
| 5 | Flyway migrations | `V1__create_merchants.sql` | schema drift |
| 6 | Repository methods | `JpaMerchantRepository` | błędne query assumptions |
| 7 | `@Version` optimistic locking | merchant version | lost update |
| 8 | DataJpaTest + Testcontainers | repository tests | testy na złej bazie |
| 9 | Transaction boundaries | service create/update | partial write |
| 10 | Future payment FK mapping | payment -> merchant | broken ownership |

## Zasada SDET

Każda migracja wymaga pytań:

- Jakie constrainty naprawdę wymusza DB?
- Jakie przypadki powinien pokryć repository test?
- Czy test używa PostgreSQL, a nie uproszczonej bazy?
- Czy dane testowe są unikalne i izolowane?
