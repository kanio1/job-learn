---
type: lesson
status: planned
area: PostgreSQL and SQL From Zero
lesson: 11
module: Test Data Isolation Strategies and Flyway Test Migrations
date: 2026-05-31
tags:
  - sql
  - postgresql
  - test-data-isolation
  - flyway
  - lesson-11
  - senior-sdet
---

# Lesson 11 — Test Data Isolation Strategies and Flyway Test Migrations

> **Evidence link:** `PostgresContainerSupport.java`, `V2__create_payment_orders.sql`, `V3__add_payment_order_list_indexes.sql`
>
> **Navigation:** [[PostgreSQL and SQL From Zero MOC]] | [[Lesson 11 - REST Assured Framework Architecture and Test Organization]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Zrozumieć zaawansowane test data isolation strategies dla PostgreSQL:
- **Per-test data creation** (obecny standard)
- **Schema-per-test** (izolacja na poziomie schema)
- **Truncation + @BeforeEach** (szybkie czyszczenie)
- **@Sql scripts** (pre-loaded test data)
- **Flyway test migrations** (test-specific schema changes)

## 2. Prerequisites

- Testcontainers PostgreSQL (Lesson 06).
- Flyway migrations (Lesson 06D).
- `@DataJpaTest` i `@SpringBootTest` (Lesson 06).
- Per-test merchant creation (Lesson 06+).

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `PostgresContainerSupport.java` | Shared PostgreSQL container dla wszystkich testów |
| `V2__create_payment_orders.sql` | Production migration (payment_orders table) |
| `V3__add_payment_order_list_indexes.sql` | Production migration (indexes) |
| `PaymentApiTestSupport.createActiveMerchant()` | Per-test merchant creation (HTTP calls) |
| `PaymentOrderListApiTestSupport.seedPaymentOrders()` | Per-test payment order seeding |

## 4. Kluczowe Pojęcia

### 4.1 Per-Test Data Creation (Obecny Standard)

```java
@Test
void createPaymentOrderReturns201() {
    // Arrange: create merchant via HTTP
    String merchantId = PaymentApiTestSupport.createActiveMerchant(port, operatorRequest);
    
    // Act: create payment order via HTTP
    PaymentOrderResponse response = paymentApi.createOrder(merchantId, creatorToken, aPaymentOrder());
    
    // Assert
    assertThat(response.amountMinor()).isEqualTo(1000);
}
```

**Trade-offs:**
- ✅ Pełna izolacja (każdy test ma własne dane)
- ✅ Realistic (dane tworzone przez production code)
- ❌ Wolne (HTTP calls dla każdego testu)
- ❌ Verbosity (każdy test powtarza setup)

**Kiedy używać?**
- Małe test suites (< 50 testów)
- Testy wymagające full stack (HTTP + DB)
- Gdy test data musi być created przez production code (np. idempotency)

### 4.2 Schema-Per-Test

```java
@Test
void testWithIsolatedSchema() {
    // Create unique schema for this test
    String schemaName = "test_" + UUID.randomUUID().toString().replace("-", "");
    jdbcTemplate.execute("CREATE SCHEMA " + schemaName);
    jdbcTemplate.execute("SET search_path TO " + schemaName);
    
    // Run Flyway migrations in this schema
    Flyway flyway = Flyway.configure()
        .dataSource(dataSource)
        .schemas(schemaName)
        .load();
    flyway.migrate();
    
    // Test logic...
    
    // Cleanup
    jdbcTemplate.execute("DROP SCHEMA " + schemaName + " CASCADE");
}
```

**Trade-offs:**
- ✅ Pełna izolacja (każdy test ma własną schema)
- ✅ Brak interference między testami
- ❌ Overhead (create/drop schema dla każdego testu)
- ❌ Złożone (wymaga custom test infrastructure)

**Kiedy używać?**
- Duże test suites (> 100 testów)
- Testy modyfikujące schema (DDL)
- Gdy testy nie mogą interferować (np. concurrent execution)

### 4.3 Truncation + @BeforeEach

```java
@BeforeEach
void cleanDatabase() {
    jdbcTemplate.execute("TRUNCATE TABLE payment_orders CASCADE");
    jdbcTemplate.execute("TRUNCATE TABLE merchants CASCADE");
    jdbcTemplate.execute("TRUNCATE TABLE idempotency_records CASCADE");
}

@Test
void testWithCleanDatabase() {
    // Database is empty at start of each test
    // Seed only what you need
    merchantRepository.save(aMerchant().build());
    
    // Test logic...
}
```

**Trade-offs:**
- ✅ Szybkie (TRUNCATE jest faster niż DELETE)
- ✅ Proste (jedna metoda @BeforeEach)
- ❌ Ryzyko flaky tests (jeśli TRUNCATE nie complete przed testem)
- ❌ Nie działa z foreign keys (chyba CASCADE)

**Kiedy używać?**
- Średnie test suites (50-100 testów)
- Testy wymagające empty database
- Gdy per-test creation jest zbyt wolne

### 4.4 @Sql Scripts (Pre-Loaded Test Data)

```java
@Sql(scripts = "classpath:test-data/seed-merchants.sql", 
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:test-data/cleanup.sql", 
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Test
void testWithPreloadedData() {
    // Database already contains merchants from seed-merchants.sql
    List<Merchant> merchants = merchantRepository.findAll();
    assertThat(merchants).hasSize(3);
    
    // Test logic...
}
```

**seed-merchants.sql:**
```sql
INSERT INTO merchants (merchant_id, reference, display_name, status, created_at, updated_at)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'MERCH-001', 'Test Merchant 1', 'ACTIVE', NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222', 'MERCH-002', 'Test Merchant 2', 'ACTIVE', NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333333', 'MERCH-003', 'Test Merchant 3', 'DRAFT', NOW(), NOW());
```

**Trade-offs:**
- ✅ Szybkie (dane ładowane raz)
- ✅ Readable (SQL scripts są self-documenting)
- ❌ Less flexible (fixed data)
- ❌ Maintenance (zmiana schema = zmiana scripts)

**Kiedy używać?**
- Testy wymagające specific data (np. edge cases)
- Testy nie modyfikujące danych (read-only)
- Gdy test data jest complex (wiele tabel, foreign keys)

### 4.5 Flyway Test Migrations

```java
// src/test/resources/db/migration/test/V99__seed_test_data.sql
INSERT INTO merchants (merchant_id, reference, display_name, status, created_at, updated_at)
VALUES ('99999999-9999-9999-9999-999999999999', 'MERCH-TEST', 'Test Merchant', 'ACTIVE', NOW(), NOW());

// Flyway automatically runs this migration in test profile
```

**Trade-offs:**
- ✅ Automatyczne (Flyway runs migrations)
- ✅ Consistent (test data w migrations)
- ❌ Globalne (dane dostępne dla wszystkich testów)
- ❌ Trudne do czyszczenia (migrations są one-way)

**Kiedy używać?**
- Test data wymagane przez wszystkie testy (np. reference data)
- Gdy test data musi być created przez migrations (np. triggers, constraints)

## 5. Walkthrough — Od Per-Test Creation Do Schema-Per-Test

```
Per-Test Creation (obecny):
1. Test zaczyna się z empty database
2. Test tworzy merchant via HTTP (POST /api/merchants)
3. Test aktywuje merchant via HTTP (POST /api/merchants/{id}/activate)
4. Test tworzy payment order via HTTP (POST /api/merchants/{id}/payment-orders)
5. Test asercjonuje response
6. Test kończy się (dane pozostają w DB)
7. Następny test powtarza kroki 2-6

Schema-Per-Test (alternatywa):
1. Test zaczyna się
2. Test tworzy unique schema (CREATE SCHEMA test_abc123)
3. Test ustawia search_path (SET search_path TO test_abc123)
4. Test uruchamia Flyway migrations w tej schema
5. Test seeduje dane bezpośrednio w tej schema
6. Test asercjonuje results
7. Test kończy się
8. Test dropuje schema (DROP SCHEMA test_abc123 CASCADE)
9. Następny test powtarza kroki 2-8 z nową schema
```

## 6. Learning Delta — Co Nowe vs Lessons 06-10

| Temat | Lesson 06-10 | Lesson 11 |
|---|---|---|
| Test data strategy | Per-test creation (HTTP) | 5 strategies (per-test, schema-per-test, truncation, @Sql, Flyway) |
| Isolation level | Test-level (każdy test tworzy własne dane) | Schema-level (każdy test ma własną schema) |
| Cleanup | Brak (dane pozostają) | TRUNCATE, DROP SCHEMA, @Sql cleanup |
| Performance | Wolne (HTTP calls) | Szybkie (TRUNCATE, pre-loaded data) |
| Flexibility | Wysoka (dane tworzone dynamicznie) | Niska (fixed data w scripts) |

## 7. Typowe Błędy

1. **TRUNCATE bez CASCADE.** Jeśli tabela ma foreign keys, TRUNCATE failuje. Użyj `TRUNCATE ... CASCADE`.
2. **Schema-per-test bez cleanup.** Jeśli test nie dropuje schema, database rośnie. Zawsze `DROP SCHEMA ... CASCADE` w @AfterEach.
3. **@Sql scripts z hardcoded IDs.** Jeśli wiele testów używa tego samego ID, interferują. Użyj UUID lub unique prefixes.
4. **Flyway test migrations w production.** Test migrations powinny być w `src/test/resources`, nie `src/main/resources`.
5. **Per-test creation dla large suites.** Jeśli masz 200 testów, per-test creation jest zbyt wolne. Rozważ truncation lub schema-per-test.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Porównaj 5 test data isolation strategies (trade-offs) | 30 min |
| 2 | Napisz @BeforeEach z TRUNCATE CASCADE dla payment_orders, merchants, idempotency_records | 20 min |
| 3 | Napisz @Sql script seedujący 3 merchants i 5 payment orders | 30 min |
| 4 | Wyjaśnij kiedy schema-per-test jest wartościowe | 15 min |
| 5 | Napisz Flyway test migration dla reference data (np. currencies) | 20 min |
| 6 | Zmierz czas wykonania 10 testów z per-test creation vs truncation | 30 min |

## 9. Pytania

1. Dlaczego per-test creation jest wolne?
2. Jak TRUNCATE różni się od DELETE?
3. Dlaczego CASCADE jest ważne dla TRUNCATE?
4. Kiedy schema-per-test jest overkill?
5. Jak @Sql scripts różnią się od Flyway migrations?
6. Dlaczego Flyway test migrations powinny być w `src/test/resources`?
7. Jak testować, że test data isolation działa?
8. Czy Testcontainers PostgreSQL wspiera schema-per-test?
9. Jak parallel test execution wpływa na test data isolation?
10. Kiedy używać per-test creation vs truncation vs schema-per-test?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `truncationCleansAllTables` | TRUNCATE CASCADE works |
| `sqlScriptSeedsData` | @Sql loads test data |
| `schemaPerTestIsolatesData` | Schema isolation works |
| `flywayTestMigrationRunsAutomatically` | Test migrations work |

## 11. Powiązane Notatki

- [[Lesson 06D - SQL and Flyway Constraints for Payment Orders]]
- [[Lesson 08 - GROUP BY COUNT SUM Null Semantics in Aggregation Queries]]
- [[Lesson 11 - REST Assured Framework Architecture and Test Organization]]
- [[Senior SDET Competency Coverage Matrix]]
