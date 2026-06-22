---
type: moc
status: draft
area: Spring Boot Spring MVC
date: 2026-05-21
tags:
  - spring-boot
  - spring-mvc
  - backend-testing
  - sdet
---

# Spring Boot Spring MVC - MOC

Ta ścieżka uczy Spring Web MVC jako granicy HTTP systemu. Tester/SDET musi rozumieć, co robi framework, co robi nasz kod i na której warstwie testować dane zachowanie.

## Lekcje

| Lesson | Topic | Repo connection | Test risk |
|---:|---|---|---|
| 1 | Request lifecycle in Spring MVC | `MerchantController` | mylenie HTTP boundary z domeną |
| 2 | `@RestController`, `@RequestMapping`, methods | merchant endpoints | błędne endpointy i metody HTTP |
| 3 | `@RequestBody` and JSON deserialization | `CreateMerchantRequest` | błędny body contract |
| 4 | Bean Validation at API edge | `@Valid`, `@Size`, `@NotBlank` | brak 400 dla invalid input |
| 5 | `@PathVariable UUID` and binding | malformed merchant id | test na złym poziomie |
| 6 | Exception handling | `MerchantExceptionHandler` | niestabilny error contract |
| 7 | DTO mapping | `MerchantMapper`, responses | wyciek encji/domain internals |
| 8 | Controller vs service responsibility | controller delegates to service | logika w złej warstwie |
| 9 | Security annotations and authorities | `@PreAuthorize` | endpoint bez ochrony |
| 10 | HTTP test vs controller unit test | REST Assured vs controller tests | kruche testy implementacji |

## Project Bridges

| Lesson | Note | Why it belongs here |
|---|---|---|
| 07 | [[Lesson 07 - Payment Order List Filter Search#7a Jakarta Validation And Spring MVC Lifecycle]] | Shows Jakarta constraints, `@Valid`/`@Validated`, query parameter binding, and why manually constructed request records may not be automatically validated |

## Zasada SDET

Pytaj:

- Czy to zachowanie należy do Spring bindingu, controller code, service czy domeny?
- Czy test HTTP jest lepszy niż unit test?
- Czy error response jest kontraktem klienta?
- Czy walidacja API-edge nie zastępuje reguł domeny?
