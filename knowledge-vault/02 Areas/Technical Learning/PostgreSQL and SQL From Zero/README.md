---
type: moc
status: draft
area: PostgreSQL and SQL From Zero
date: 2026-05-21
tags:
  - sql
  - postgresql
  - backend-testing
  - sdet
---

# PostgreSQL and SQL From Zero - MOC

Ta ścieżka uczy SQL i PostgreSQL od zera z perspektywy Senior QA Automation/SDET. Celem nie jest zostanie DBA. Celem jest umieć czytać dane, rozumieć migracje, wykrywać ryzyka integralności i projektować testy danych dla systemu płatniczego.

## Jak Ta Ścieżka Łączy Się Z Aplikacją

| Repo element | SQL concept |
|---|---|
| `merchants` table | tabela, primary key, unique constraint, optimistic locking |
| future `payment_orders` | foreign key, amount constraints, idempotency key |
| future `payment_events` | event timeline, ordering, auditability |
| repository tests | sprawdzanie constraintów i zachowania bazy |
| Flyway migrations | realne źródło prawdy schematu |

## Lekcje

| Lesson | Title | Repo connection | Senior QA skill |
|---:|---|---|---|
| 1 | Czym jest relacyjna baza danych | `merchants` | tabela, rekord, kolumna, PK |
| 2 | `SELECT` od zera | lista merchantów | czytanie danych testowych |
| 3 | `WHERE`, operatory i `NULL` | filtr statusów | precyzyjne warunki |
| 4 | `ORDER BY` i `LIMIT` | first page list | stabilna kolejność testów |
| 5 | `INSERT`, `UPDATE`, `DELETE` mental model | JPA zapisuje dane | rozumienie efektów aplikacji |
| 6 | `JOIN` | merchant + future payment | ownership i relacje |
| 7 | `GROUP BY`, `COUNT`, `SUM` | raport płatności | testy agregacji |
| 8 | Constraints | unique merchant reference | DB jako safety net |
| 9 | Foreign keys | payment belongs to merchant | integralność referencyjna |
| 10 | Indexes | payment list/filter | performance-aware testing |
| 11 | Transactions | create payment + event | atomiczność |
| 12 | Isolation and concurrency | duplicate idempotency | race conditions |
| 13 | Optimistic locking | status updates | concurrent modification |
| 14 | Idempotency keys | payment order creation | duplicate charge prevention |
| 15 | SQL for test data setup | fixtures/Testcontainers | stabilne dane testowe |
| 16 | SQL review checklist | każda migracja | review DB zmian |

## Lesson 6 Project Bridge

| Lesson | Note | Scope |
|---|---|---|
| 06D | [[Lesson 06D - SQL and Flyway Constraints for Payment Orders]] | beginner SQL on real Lesson 6 tables, Flyway constraints, idempotency records, audit history |
| 08 | [[Lesson 08 - GROUP BY COUNT SUM Null Semantics in Aggregation Queries]] | GROUP BY, SUM, COUNT, COALESCE, nullable param handling, EXPLAIN thinking, projection interfaces |

## SQL To Spring/JPA Mapping

| SQL/PostgreSQL | Spring/JPA |
|---|---|
| tabela | `@Entity`, `@Table` |
| kolumna | field + `@Column` |
| primary key | `@Id` |
| UUID PK | `UUID merchantId` |
| unique constraint | Flyway SQL + czasem `@Column(unique = true)` jako dokumentacja |
| foreign key | FK w Flyway, `@ManyToOne` albo jawne `merchantId` |
| check constraint | Flyway `CHECK (...)` |
| version column | `@Version` |
| index | Flyway `CREATE INDEX` |
| transaction | `@Transactional` |
| query | Spring Data repository method / JPQL / native SQL |
| migration | `db/migration/.../V...sql` |

## Zasada SDET

Zawsze pytaj:

- Co deklaruje Java?
- Co naprawdę wymusza baza?
- Czy testujemy constraint na poziomie DB?
- Czy aplikacja i migracja mówią to samo?
- Czy dane testowe są izolowane i równoległo-bezpieczne?
