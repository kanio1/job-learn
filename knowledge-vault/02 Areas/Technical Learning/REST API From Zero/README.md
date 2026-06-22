---
type: moc
status: ready
area: REST API From Zero
date: 2026-05-21
tags:
  - moc
  - rest-api-from-zero
  - merchant-registry
  - backend-testing
  - sdet
---

# REST API From Zero - MOC

Ten indeks zbiera pierwsze lekcje REST API przed wejściem w REST Assured.

## Cel obszaru

Zrozumiec request/response flow w realnym kodzie Payment Quality Engineering Lab, zanim zaczniesz automatyzowac API testy w REST Assured.

## Kolejność nauki

1. `Merchant Request and Response Flow.md`
2. `../JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/01-12 REST Assured Foundations.md` - Lesson 1: What REST Assured Is

## Lekcja 01

- `Merchant Request and Response Flow.md`

## Lesson 08

- [[Lesson 08 - Summary Endpoint Contract, Status Codes, and Error Taxonomy]] — pełny kontrakt HTTP endpointu summary, status codes, security matcher ordering, error taxonomy

## Podsumowania sesji

- `REST REST Assured Java - Session Summary - Merchant API Tests.md`

Zakres:

- UI -> Nuxt Server API -> Spring Security -> Controller -> DTO -> Validation -> Service -> Domain -> Repository -> PostgreSQL -> Response.
- REST Assured -> Spring Security -> Controller -> DTO -> Service -> Repository -> PostgreSQL -> assertions.
- Happy path, validation error, duplicate error, auth denial.
- Unikalny `merchantReference` jako test data focus.
- Interview story: how I trace an API request through backend layers.

## Powiązane ścieżki

- `../JUnit REST Assured/README.md`
- `../Backend Testing Review/README.md`
- `../Spring Boot Spring MVC/README.md`
- `../Spring Data JPA and Flyway/README.md`
- `../PostgreSQL and SQL From Zero/README.md`
- `../Security and Authorization Testing/README.md`
- `../../../01 Projects/Payment_Quality_Engineering_Lab/Payment Gateway SDET Learning Plan.md`

## Guardrail

Ten obszar nie dodaje nowej funkcjonalnosci biznesowej. Na tym etapie nie dodajemy Payment Order, Kafki, GraphQL ani gRPC.
