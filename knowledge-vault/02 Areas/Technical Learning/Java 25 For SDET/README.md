---
type: moc
status: draft
area: Java 25 For SDET
date: 2026-05-21
tags:
  - java-25
  - sdet
  - backend-testing
  - effective-java
---

# Java 25 For SDET - MOC

Ta ścieżka uczy Java 25 przez realny kod backendu i testów. Celem nie jest teoria języka w izolacji, tylko umiejętność czytania, pisania i reviewowania kodu Java w testowalnym systemie płatniczym.

## Lekcje

| Lesson | Module | Topics | Repo connection |
|---:|---|---|---|
| 1 | Java Syntax From Zero | klasy, metody, parametry, return, static imports | REST Assured tests |
| 2 | Objects And Records | records, immutability, DTO | `CreateMerchantRequest`, `MerchantResponse` |
| 3 | Collections | `List`, `Map`, `Map.of`, `Map.copyOf` | request body, test data |
| 4 | Exceptions | checked/unchecked, domain exceptions | `InvalidMerchantReferenceException` |
| 5 | Enums | statusy, state machine | `MerchantStatus`, future `PaymentStatus` |
| 6 | Time API | `Instant`, testing time | `createdAt`, `updatedAt` |
| 7 | UUID | identity, parsing, Spring binding | `@PathVariable UUID` |
| 8 | Optional | repository lookup | `findById`, not found |
| 9 | Streams | kolekcje w testach | AssertJ filtering |
| 10 | Generics | repository, response DTOs | Spring Data/JPA |
| 11 | Annotations | `@Test`, `@Entity`, `@RestController`, `@Valid` | cały backend |
| 12 | JDK Tooling | warnings, agents, JVM args | Mockito `-javaagent` |
| 13 | Effective Java For Testability | immutability, factories, dependency injection | domain/test design |

## Java Lens Per Sprint

| Sprint | Java focus |
|---|---|
| Payment Order | records, enums, value objects, validation exceptions |
| Lifecycle | enum state machine, methods, invariants |
| Idempotency | `Map`, `Optional`, equality, unique keys |
| Webhooks | time, retry counters, immutable event payloads |
| Kafka | serialization records, schema evolution |
| GraphQL | DTO/projection types |
| gRPC | generated Java classes, builders, enums |

## Zasada SDET

Java w tym projekcie ma być uczona przez testowalność:

- Czy typ pomaga wyrazić regułę domenową?
- Czy test może łatwo stworzyć dane?
- Czy wyjątek mówi, co poszło źle?
- Czy kolekcje są mutowalne tylko tam, gdzie trzeba?
- Czy warning w buildzie jest przyszłym ryzykiem?
