---
type: lesson
status: ready
area: PostgreSQL and SQL From Zero
project: Payment Quality Engineering Lab
phase: 2
lesson: 06D
date: 2026-05-29
tags:
  - sql
  - postgresql
  - flyway
  - lesson-06
  - payment-order
  - database-verification
  - constraints
  - senior-sdet
---

# Lesson 06D - SQL and Flyway Constraints for Payment Orders

> **Scope:** beginner SQL for Lesson 6 using the real local PostgreSQL database and the current `merchants`, `payment_orders`, `idempotency_records`, and `payment_order_status_history` tables.
>
> **Do not expand yet:** CTEs, window functions, advanced `EXPLAIN`, locking experiments, RLS, lifecycle status transitions, PSP data, Kafka, refunds, settlement.
>
> **Goal:** understand what the Lesson 6 REST tests create in PostgreSQL and why Flyway constraints are part of the quality contract.

## 1. Mental Model For A Beginner

Think about PostgreSQL like a school office with strict paper forms.

| Database idea | Simple analogy | Payment lab example |
|---|---|---|
| Database | A filing cabinet | `payment_quality_lab` |
| Table | One drawer for one kind of document | `payment_orders` |
| Row | One filled form | one payment order |
| Column | One field on the form | `amount_minor`, `currency` |
| Primary key | Student ID number | `payment_order_id` |
| Foreign key | Link to another drawer | `payment_orders.merchant_id` points to `merchants.merchant_id` |
| Constraint | Office rule that rejects bad forms | currency must be `PLN`, `EUR`, or `USD` |
| Index | Alphabetical card catalog | faster lookup by `merchant_id` |
| Migration | Official form template version | `V2__create_payment_orders.sql` |

SQL is the language you use to ask the office questions:

```sql
select payment_order_id, amount_minor, currency
from payment_orders;
```

Read it like English:

> Show me these columns from the payment orders drawer.

## 2. How To Connect

From repository root:

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d payment-quality-postgres
```

Start the backend once so Flyway creates the tables:

```bash
cd apps/backend
./mvnw spring-boot:run
```

Open `psql` from repository root:

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml exec payment-quality-postgres psql -U payment_quality -d payment_quality_lab
```

Useful `psql` commands:

```sql
\dt
\d payment_orders
\d idempotency_records
\d payment_order_status_history
\di
\q
```

What they mean:

| Command | Meaning |
|---|---|
| `\dt` | list tables |
| `\d table_name` | describe columns, constraints, indexes |
| `\di` | list indexes |
| `\q` | quit `psql` |

## 3. The Lesson 6 Tables

Current tables after Flyway:

```text
merchants
payment_orders
idempotency_records
payment_order_status_history
flyway_schema_history
```

What each table is responsible for:

| Table | Responsibility | QA question |
|---|---|---|
| `merchants` | who can own payment orders | Is the merchant active and eligible? |
| `payment_orders` | the payment order resource returned by the API | Was the order created with correct amount, currency, status? |
| `idempotency_records` | retry safety for `POST` create | Did retry create a duplicate or reuse the original order? |
| `payment_order_status_history` | audit trail | Can we explain who created the order and with which correlation id? |
| `flyway_schema_history` | migration history | Which schema versions were applied? |

## 4. SQL Style Rules For This Project

Use these habits from the beginning:

| Rule | Why |
|---|---|
| Write SQL keywords lowercase in learning notes | Matches current repo style and is easy to read |
| Name columns explicitly instead of `select *` in real checks | Avoids noisy output and accidental dependency on column order |
| Use `order by` when comparing results | Databases do not promise stable order without it |
| Use `limit` during exploration | Prevents huge result sets later |
| Use aliases in joins | Keeps multi-table queries readable |
| Do not `update` or `delete` without `where` | One missing condition can change every row |
| Prefer API-created data for business flows | The API applies security, validation, service logic, and audit behavior |
| Use direct SQL when learning constraints or debugging | DB probes are diagnostic tools, not a replacement for API tests |

Good exploration query:

```sql
select
  payment_order_id,
  merchant_id,
  amount_minor,
  currency,
  status,
  created_at
from payment_orders
order by created_at desc
limit 10;
```

Avoid this as a long-term habit:

```sql
select * from payment_orders;
```

`select *` is acceptable only for quick exploration while learning.

## 5. Reading Flyway Like A Tester

Flyway migration:

```text
apps/backend/src/main/resources/db/migration/payment/V2__create_payment_orders.sql
```

The migration is not just database setup. It is a testable requirement.

### `create table`

```sql
create table payment_orders (...);
```

Meaning:

> Create a new drawer in the filing cabinet for payment order forms.

QA meaning:

> Every payment order must fit this structure. If Java tries to save bad data, PostgreSQL should reject it.

### `not null`

Example:

```sql
merchant_id uuid not null
```

Meaning:

> This field cannot be empty.

Payment meaning:

> A payment order without a merchant owner is invalid.

### `primary key`

Example:

```sql
payment_order_id uuid primary key
```

Meaning:

> This is the unique identity of one row.

QA risk:

> If IDs were not unique, `GET /payment-orders/{id}` could return ambiguous data.

### `foreign key`

Example:

```sql
constraint fk_payment_orders_merchant
  foreign key (merchant_id) references merchants (merchant_id)
```

Meaning:

> You cannot create a payment order for a merchant that does not exist.

Analogy:

> You cannot file a student's exam under a student ID that is not in the school registry.

### `check`

Examples:

```sql
check (amount_minor between 1 and 100000000)
check (currency in ('PLN', 'EUR', 'USD'))
check (status in ('CREATED'))
```

Meaning:

> PostgreSQL rejects rows that break these rules.

QA meaning:

> Java validation is friendly, but DB constraints are the last line of defense.

### `unique`

Example:

```sql
unique (merchant_id, idempotency_key_hash)
```

Meaning:

> The same merchant cannot use the same idempotency key hash for two different records.

Payment meaning:

> Retry with the same `Idempotency-Key` must not create duplicate payment orders.

### `default now()`

Example:

```sql
created_at timestamptz not null default now()
```

Meaning:

> If the app does not provide a timestamp, PostgreSQL fills the current time.

QA risk:

> Tests should not assert exact timestamps unless the exact value is controlled.

### `create index`

Example:

```sql
create index idx_payment_orders_merchant_created
  on payment_orders (merchant_id, created_at desc, payment_order_id asc);
```

Meaning:

> Build a lookup helper for common searches.

Payment meaning:

> Listing orders for one merchant by newest first should not require scanning everything forever.

## 6. Scenario 1 - Inspect The Schema

Goal: learn what the database actually contains.

```sql
\dt
\d merchants
\d payment_orders
\d idempotency_records
\d payment_order_status_history
```

Questions to answer:

| Question | Where to look |
|---|---|
| Which column is the primary key of `payment_orders`? | `\d payment_orders` |
| Which table owns merchant data? | `\dt`, `\d merchants` |
| Which constraints protect currency? | `\d payment_orders` |
| Which constraints protect idempotency? | `\d idempotency_records` |
| Which index supports merchant order listing? | `\di` |

## 7. Scenario 2 - Read Data Created By The App

Run a REST test or create data through the app first. Then inspect the DB.

```sql
select
  merchant_id,
  normalized_reference,
  display_name,
  status,
  created_at
from merchants
order by created_at desc
limit 10;
```

```sql
select
  payment_order_id,
  merchant_id,
  client_order_reference,
  amount_minor,
  currency,
  status,
  created_at,
  updated_at,
  version
from payment_orders
order by created_at desc
limit 10;
```

What to notice:

| Column | Why it matters |
|---|---|
| `amount_minor` | money is stored as integer minor units, not `double` |
| `currency` | only allowed values are `PLN`, `EUR`, `USD` |
| `status` | Lesson 6 only allows `CREATED` |
| `version` | prepares for future optimistic locking, but lifecycle is deferred |
| `created_at`, `updated_at` | useful for debugging order and freshness |

## 8. Scenario 3 - Join Merchant And Payment Order

`join` means: combine rows from two tables using a relationship.

Analogy:

> You have one drawer with students and another drawer with exams. A join lets you show exam + student name together.

```sql
select
  po.payment_order_id,
  po.client_order_reference,
  po.amount_minor,
  po.currency,
  po.status as payment_status,
  m.normalized_reference as merchant_reference,
  m.status as merchant_status
from payment_orders po
join merchants m on m.merchant_id = po.merchant_id
order by po.created_at desc
limit 10;
```

Best practice shown here:

| Pattern | Why |
|---|---|
| `payment_orders po` | short alias for readable joins |
| `po.status as payment_status` | avoids confusion with `m.status` |
| explicit column list | output is readable and intentional |
| `order by ... limit 10` | stable, safe exploration |

## 9. Scenario 4 - Check Idempotency Records

This query connects a payment order to the idempotency record that protected retry behavior.

```sql
select
  po.payment_order_id,
  po.client_order_reference,
  po.amount_minor,
  po.currency,
  ir.idempotency_key_hash,
  ir.request_fingerprint_hash,
  ir.created_at as idempotency_created_at,
  ir.completed_at
from payment_orders po
join idempotency_records ir on ir.payment_order_id = po.payment_order_id
order by ir.created_at desc
limit 10;
```

What you are learning:

| Observation | Meaning |
|---|---|
| key is stored as hash | safer than storing raw `Idempotency-Key` |
| fingerprint is stored as hash | backend can compare request intent without storing full body |
| `completed_at` is not null | reservation was completed with a payment order |
| one record per payment order | enforced by unique `payment_order_id` |

Lesson 6 REST connection:

| REST test | DB behavior |
|---|---|
| first create returns `201` | new `payment_orders` row + new `idempotency_records` row |
| replay returns `200` | same existing `payment_order_id` is reused |
| different body returns `409` | same key hash but different fingerprint is rejected |

## 10. Scenario 5 - Read The Audit Trail

Audit trail means the system can explain what happened.

```sql
select
  h.status_history_id,
  h.payment_order_id,
  h.from_status,
  h.to_status,
  h.actor_subject,
  h.correlation_id,
  h.created_at
from payment_order_status_history h
order by h.created_at desc
limit 10;
```

Join audit history to payment order:

```sql
select
  po.client_order_reference,
  po.status as current_status,
  h.from_status,
  h.to_status,
  h.actor_subject,
  h.correlation_id,
  h.created_at as history_created_at
from payment_order_status_history h
join payment_orders po on po.payment_order_id = h.payment_order_id
order by h.created_at desc
limit 10;
```

Why `from_status` can be null:

> Creation has no previous payment order status. The order did not exist before `CREATED`.

## 11. Scenario 6 - Try Constraint Experiments Safely

For these examples, first choose an existing merchant:

```sql
select merchant_id, normalized_reference, status
from merchants
order by created_at desc
limit 5;
```

Copy one `merchant_id` into the examples below.

### Valid insert

```sql
insert into payment_orders (
  payment_order_id,
  merchant_id,
  client_order_reference,
  amount_minor,
  currency,
  status
) values (
  gen_random_uuid(),
  '<merchant-uuid>',
  'PAY-SQL-VALID-001',
  12500,
  'PLN',
  'CREATED'
);
```

Expected result:

```text
INSERT 0 1
```

### Invalid currency

```sql
insert into payment_orders (
  payment_order_id,
  merchant_id,
  client_order_reference,
  amount_minor,
  currency,
  status
) values (
  gen_random_uuid(),
  '<merchant-uuid>',
  'PAY-SQL-BAD-CURRENCY',
  12500,
  'GBP',
  'CREATED'
);
```

Expected result:

```text
ERROR: new row for relation "payment_orders" violates check constraint "chk_payment_orders_currency"
```

### Invalid amount

```sql
insert into payment_orders (
  payment_order_id,
  merchant_id,
  client_order_reference,
  amount_minor,
  currency,
  status
) values (
  gen_random_uuid(),
  '<merchant-uuid>',
  'PAY-SQL-BAD-AMOUNT',
  0,
  'PLN',
  'CREATED'
);
```

Expected result:

```text
ERROR: new row for relation "payment_orders" violates check constraint "chk_payment_orders_amount_minor"
```

### Invalid merchant

```sql
insert into payment_orders (
  payment_order_id,
  merchant_id,
  client_order_reference,
  amount_minor,
  currency,
  status
) values (
  gen_random_uuid(),
  '00000000-0000-0000-0000-000000000001',
  'PAY-SQL-BAD-MERCHANT',
  12500,
  'PLN',
  'CREATED'
);
```

Expected result:

```text
ERROR: insert or update on table "payment_orders" violates foreign key constraint "fk_payment_orders_merchant"
```

QA lesson:

> These errors prove PostgreSQL protects the data even if a future bug bypasses Java validation.

## 12. Scenario 7 - Read Flyway History

```sql
select
  installed_rank,
  version,
  description,
  script,
  installed_on,
  success
from flyway_schema_history
order by installed_rank;
```

Expected Lesson 6-7 shape:

| Version | Meaning |
|---|---|
| `1` | merchant table exists |
| `2` | payment order tables exist |
| `3` | payment order list indexes exist |

Why this matters:

> If a table is missing, check `flyway_schema_history` before blaming SQL. Maybe the backend did not run the migration yet.

## 13. API Test To SQL Debugging Map

Use SQL to debug, not to replace the API assertion.

| If this REST test fails | SQL to inspect | What you are checking |
|---|---|---|
| `createPaymentOrderReturns201WithHeaders` | `select ... from payment_orders order by created_at desc limit 10;` | Was a row created? |
| `idempotentReplayReturns200WithSameId` | join `payment_orders` + `idempotency_records` | Did retry reuse one order? |
| `idempotencyConflictReturns409` | `idempotency_records` by newest records | Is the same key hash protected? |
| `crossTenantReadReturns404` | join `payment_orders` + `merchants` | Which merchant owns the order? |
| repository constraint test fails | `\d payment_orders` | Does DB have the expected constraint? |

Example debug query:

```sql
select
  po.payment_order_id,
  po.merchant_id,
  m.normalized_reference,
  po.client_order_reference,
  po.amount_minor,
  po.currency,
  po.status,
  po.created_at
from payment_orders po
join merchants m on m.merchant_id = po.merchant_id
order by po.created_at desc
limit 20;
```

## 14. What To Learn Now vs Later

Learn now in Lesson 6:

| Topic | Status for now |
|---|---|
| table, row, column | learn now |
| `select`, `from`, `where`, `order by`, `limit` | learn now |
| `join` | learn now with merchant + payment order |
| `insert` for constraint experiments | learn now, carefully |
| `primary key`, `foreign key`, `not null`, `check`, `unique` | learn now through Flyway |
| `create table`, `create index` | read and understand now, do not design new schema yet |
| `flyway_schema_history` | learn now |
| CTE | later, Sprint 7b |
| window functions | later, Sprint 8 |
| advanced `explain` tuning | later, Sprint 8 |
| transaction isolation and locks | later, Sprint 8b |
| row-level security | later, Sprint 9+ |

## 15. Mini Checklist For Every SQL Query

Before running a query, ask:

1. Am I reading data or changing data?
2. If changing data, do I have a `where` clause or a safe test database?
3. Did I specify columns instead of using `select *`?
4. Do I need `order by` for deterministic results?
5. Do I need `limit` while exploring?
6. Does this query answer a real QA question?

## 16. Practice Plan For One Session

Timebox: 45-60 minutes.

1. Start PostgreSQL and backend.
2. Run `\dt`, `\d payment_orders`, `\d idempotency_records`.
3. Run the read queries from scenarios 2-5.
4. Run one valid insert and two invalid inserts.
5. Read `flyway_schema_history`.
6. Pick one REST Assured test and explain which table proves its DB side effect.

Completion evidence:

- You can explain why `payment_orders.currency = 'GBP'` is rejected.
- You can explain why `merchant_id` is a foreign key.
- You can find the newest payment order.
- You can join payment order to merchant.
- You can connect `Idempotency-Key` behavior to `idempotency_records`.

## 17. Short Interview Answer

> In Lesson 6 I used PostgreSQL not only as storage, but as a quality safety net. Flyway creates payment order tables with primary keys, foreign keys, check constraints, unique idempotency constraints and indexes. REST Assured verifies the HTTP contract, while SQL helps me debug persistence, idempotency records and audit history when the database state is part of the risk.
