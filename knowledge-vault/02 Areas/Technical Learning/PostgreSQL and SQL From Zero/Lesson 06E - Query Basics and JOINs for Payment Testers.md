---
type: lesson
status: ready
area: PostgreSQL and SQL From Zero
project: Payment Quality Engineering Lab
phase: 2
lesson: 06E
date: 2026-06-01
tags:
  - sql
  - postgresql
  - joins
  - flyway
  - lesson-06
  - payment-order
  - database-verification
  - tester-sql
  - senior-sdet
---

# Lesson 06E - Query Basics and JOINs for Payment Testers

> **Scope:** podstawy budowania zapytań SQL, `JOIN` i typy joinów na realnych tabelach `merchants`, `payment_orders`, `idempotency_records`, `payment_order_status_history`.
>
> **Prerequisite:** [[Lesson 06D - SQL and Flyway Constraints for Payment Orders]].
>
> **Goal:** umieć czytać migrację Flyway jak mapę danych i na jej podstawie pisać proste, bezpieczne zapytania diagnostyczne testera.

## 1. Najprostszy Model W Głowie

Wyobraź sobie bazę danych jak kilka arkuszy Excela w jednym pliku.

`merchants` to arkusz z firmami/merchantami.

| merchant_id | normalized_reference | status |
|---|---|---|
| `m1` | `MERCH-ABC` | `ACTIVE` |
| `m2` | `MERCH-XYZ` | `DRAFT` |

`payment_orders` to arkusz z zamówieniami płatności.

| payment_order_id | merchant_id | amount_minor | currency | status |
|---|---|---:|---|---|
| `p1` | `m1` | `12500` | `PLN` | `CREATED` |
| `p2` | `m1` | `5000` | `EUR` | `CREATED` |

Wspólna kolumna to `merchant_id`.

To jest klucz do zrozumienia `JOIN`:

> `JOIN` łączy wiersze z dwóch tabel, gdy wartości w wybranych kolumnach do siebie pasują.

W naszym projekcie ta zależność jest zapisana w migracji:

```sql
constraint fk_payment_orders_merchant
    foreign key (merchant_id) references merchants (merchant_id)
```

Czytaj to tak:

> `payment_orders.merchant_id` wskazuje na `merchants.merchant_id`.

## 2. Podstawowa Kolejność Pisania Query

Najczęściej budujesz query w tej kolejności myślenia:

1. Z której tabeli startuję?
2. Jakie kolumny chcę zobaczyć?
3. Czy potrzebuję danych z drugiej tabeli?
4. Jak tabele są połączone?
5. Czy filtruję dane?
6. Czy wynik ma mieć stabilną kolejność?
7. Czy ograniczam liczbę wierszy?

Szablon:

```sql
select
  alias.column_name,
  alias.other_column
from table_name alias
where condition
order by alias.created_at desc
limit 20;
```

Przykład z `payment_orders`:

```sql
select
  po.payment_order_id,
  po.client_order_reference,
  po.amount_minor,
  po.currency,
  po.status,
  po.created_at
from payment_orders po
order by po.created_at desc
limit 10;
```

## 3. Minimalne Polecenia SQL, Które Musisz Znać

| Polecenie | Proste znaczenie | Przykład testerski |
|---|---|---|
| `select` | wybierz kolumny do pokazania | pokaż ID, amount, status |
| `from` | z której tabeli czytasz | czytaj z `payment_orders` |
| `where` | filtruj wiersze | tylko `currency = 'PLN'` |
| `and` / `or` | łącz warunki | aktywny merchant i PLN |
| `order by` | sortuj wynik | najnowsze ordery pierwsze |
| `limit` | ogranicz liczbę wierszy | pokaż tylko 10 |
| `join` | połącz tabele | order + merchant |
| `left join` | pokaż wszystko z lewej tabeli i dopasowania z prawej | merchantci nawet bez orderów |
| `is null` | szukaj braku wartości | merchant bez ordera po `left join` |
| `as` | nadaj czytelną nazwę kolumnie | `po.status as payment_status` |
| `count(*)` | policz wiersze | ile orderów jest w tabeli |
| `group by` | grupuj do agregacji | ile orderów per waluta |

Nie musisz od razu znać całego SQL. Na Lesson 6 wystarczy umieć czytać i pisać te elementy.

## 4. Zasady Łatwe Do Zapamiętania

### Zasada 1: `select` mówi, co chcesz zobaczyć

```sql
select
  payment_order_id,
  amount_minor,
  currency
from payment_orders;
```

Pytanie po ludzku:

> Jakie ID, kwoty i waluty mają payment ordery?

### Zasada 2: `from` mówi, od której tabeli zaczynasz

```sql
from payment_orders po
```

`po` to alias, czyli krótka nazwa tabeli.

### Zasada 3: `where` zmniejsza wynik

```sql
select
  po.payment_order_id,
  po.amount_minor,
  po.currency
from payment_orders po
where po.currency = 'PLN';
```

Pytanie:

> Pokaż tylko płatności w PLN.

### Zasada 4: `order by` daje przewidywalną kolejność

Bez `order by` baza nie obiecuje kolejności.

```sql
select
  po.payment_order_id,
  po.created_at
from payment_orders po
order by po.created_at desc;
```

`desc` znaczy malejąco, czyli najnowsze najpierw.

`asc` znaczy rosnąco.

### Zasada 5: `limit` chroni Cię przed zalaniem wynikiem

```sql
select
  po.payment_order_id,
  po.created_at
from payment_orders po
order by po.created_at desc
limit 10;
```

Dla testera to bezpieczny nawyk eksploracyjny.

### Zasada 6: `join` potrzebuje warunku `on`

```sql
join merchants m on m.merchant_id = po.merchant_id
```

`on` mówi bazie:

> Połącz te wiersze, gdzie `merchant_id` jest taki sam.

### Zasada 7: jeśli dwie tabele mają podobne kolumny, używaj aliasów

Obie tabele mają `status`:

```sql
po.status as payment_status,
m.status as merchant_status
```

Bez tego łatwo pomylić status payment ordera ze statusem merchanta.

## 5. INNER JOIN Czyli Zwykły JOIN

`join` albo `inner join` pokazuje tylko pary, które istnieją po obu stronach.

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

Czytaj po ludzku:

> Pokaż payment ordery razem z merchantem, do którego należą.

Dla testera:

> Po teście REST `createPaymentOrderReturns201WithHeaders` możesz sprawdzić, czy order naprawdę należy do właściwego merchanta.

## 6. LEFT JOIN Czyli Pokaż Też Braki

`left join` pokazuje wszystkie wiersze z lewej tabeli, nawet jeśli nie ma dopasowania w prawej.

```sql
select
  m.merchant_id,
  m.normalized_reference,
  m.status as merchant_status,
  po.payment_order_id,
  po.client_order_reference,
  po.amount_minor,
  po.currency
from merchants m
left join payment_orders po on po.merchant_id = m.merchant_id
order by m.created_at desc
limit 20;
```

Czytaj po ludzku:

> Pokaż merchantów. Jeśli mają ordery, pokaż je obok. Jeśli nie mają, pokaż `null` po stronie ordera.

Przydatne query testerskie:

```sql
select
  m.merchant_id,
  m.normalized_reference,
  m.status
from merchants m
left join payment_orders po on po.merchant_id = m.merchant_id
where po.payment_order_id is null
order by m.created_at desc
limit 20;
```

Pytanie:

> Którzy merchantci nie mają żadnych payment orderów?

To jest bardzo ważny wzorzec:

```sql
left join ... where right_table.id is null
```

Znaczy:

> Znajdź rekordy z lewej tabeli bez dopasowania w prawej tabeli.

## 7. RIGHT JOIN I FULL OUTER JOIN

`right join` zachowuje wszystko z prawej tabeli.

W praktyce dla początkującego lepiej go unikać i przepisać jako `left join`, bo jest czytelniej.

Mniej czytelnie:

```sql
select
  m.normalized_reference,
  po.payment_order_id
from payment_orders po
right join merchants m on m.merchant_id = po.merchant_id;
```

Czytelniej:

```sql
select
  m.normalized_reference,
  po.payment_order_id
from merchants m
left join payment_orders po on po.merchant_id = m.merchant_id;
```

`full outer join` pokazuje wszystko z obu tabel, nawet bez pary. W Lesson 6 rzadko go potrzebujesz, bo foreign key pilnuje, że payment order nie istnieje bez merchanta.

```sql
select
  m.merchant_id,
  m.normalized_reference,
  po.payment_order_id,
  po.merchant_id as payment_merchant_id
from merchants m
full outer join payment_orders po on po.merchant_id = m.merchant_id;
```

Tester używa `full outer join` raczej przy audycie lub porównywaniu dwóch źródeł danych. To nie jest priorytet Lesson 6.

## 8. JOIN Z Idempotency Records

Migracja mówi:

```sql
constraint fk_idempotency_records_payment_order
    foreign key (payment_order_id) references payment_orders (payment_order_id)
```

To znaczy:

> `idempotency_records.payment_order_id` wskazuje na `payment_orders.payment_order_id`.

Query:

```sql
select
  po.payment_order_id,
  po.client_order_reference,
  po.amount_minor,
  po.currency,
  ir.idempotency_key_hash,
  ir.request_fingerprint_hash,
  ir.completed_at
from payment_orders po
join idempotency_records ir on ir.payment_order_id = po.payment_order_id
order by ir.created_at desc
limit 10;
```

Pytanie testera:

> Czy retry-safe create ma jeden order i jeden powiązany rekord idempotency?

## 9. JOIN Z Audit History

Migracja mówi:

```sql
constraint fk_payment_order_status_history_order
    foreign key (payment_order_id) references payment_orders (payment_order_id)
```

Query:

```sql
select
  po.payment_order_id,
  po.client_order_reference,
  po.status as current_status,
  h.from_status,
  h.to_status,
  h.actor_subject,
  h.correlation_id,
  h.created_at as history_created_at
from payment_orders po
join payment_order_status_history h on h.payment_order_id = po.payment_order_id
order by h.created_at desc
limit 10;
```

Pytanie testera:

> Czy utworzenie payment ordera zostawiło ślad audytowy z aktorem i correlation id?

W Lesson 6 zwykle zobaczysz:

```text
from_status = null
to_status = CREATED
```

To jest normalne, bo utworzenie ordera nie ma poprzedniego statusu.

## 10. Podstawowe Agregacje Do Diagnostyki

`count(*)` liczy wiersze.

```sql
select count(*) as payment_order_count
from payment_orders;
```

`group by` grupuje dane.

```sql
select
  po.currency,
  count(*) as order_count
from payment_orders po
group by po.currency
order by po.currency asc;
```

Pytanie:

> Ile orderów mam per waluta?

`sum` sumuje wartości liczbowe.

```sql
select
  po.currency,
  count(*) as order_count,
  sum(po.amount_minor) as total_amount_minor
from payment_orders po
group by po.currency
order by po.currency asc;
```

Na Lesson 6 wystarczy rozumieć te query diagnostycznie. Głębsze agregacje są w Lesson 08.

## 11. Jak Migracja Flyway Pomaga Rozgryźć Query

Flyway migration jest mapą. Query jest trasą po tej mapie.

### Przykład: primary key

Migracja:

```sql
payment_order_id UUID PRIMARY KEY
```

Wniosek:

> `payment_order_id` jednoznacznie identyfikuje payment order.

Query:

```sql
select
  po.payment_order_id,
  po.status
from payment_orders po
where po.payment_order_id = '<payment-order-uuid>';
```

### Przykład: foreign key

Migracja:

```sql
foreign key (merchant_id) references merchants (merchant_id)
```

Wniosek:

> Te dwie tabele można połączyć po `merchant_id`.

Query:

```sql
from payment_orders po
join merchants m on m.merchant_id = po.merchant_id
```

### Przykład: check constraint

Migracja:

```sql
check (currency in ('PLN', 'EUR', 'USD'))
```

Wniosek:

> Sensowne filtry waluty to tylko `PLN`, `EUR`, `USD`.

Query:

```sql
select
  po.payment_order_id,
  po.currency
from payment_orders po
where po.currency = 'PLN';
```

### Przykład: index

Migracja:

```sql
create index idx_payment_orders_merchant_created
    on payment_orders (merchant_id, created_at desc, payment_order_id asc);
```

Wniosek:

> Częsty i ważny dostęp to: ordery jednego merchanta, najnowsze najpierw.

Query:

```sql
select
  po.payment_order_id,
  po.client_order_reference,
  po.created_at
from payment_orders po
where po.merchant_id = '<merchant-uuid>'
order by po.created_at desc, po.payment_order_id asc
limit 20;
```

## 12. Tips And Hints Od Testera

1. Jeśli widzisz `primary key`, znalazłeś kolumnę do precyzyjnego `where id = ...`.
2. Jeśli widzisz `foreign key`, znalazłeś naturalny warunek `join ... on ...`.
3. Jeśli constraint nazywa się `fk_*`, szukaj relacji między tabelami.
4. Jeśli constraint nazywa się `uk_*`, szukaj reguły unikalności i potencjalnego testu duplikatu.
5. Jeśli constraint nazywa się `chk_*`, szukaj dozwolonych i niedozwolonych wartości do testów negatywnych.
6. Jeśli kolumna ma `not null`, testuj czy API nie pozwala utworzyć zasobu bez tej wartości.
7. Jeśli kolumna ma `default`, sprawdź czy aplikacja jawnie ustawia wartość, czy polega na bazie.
8. Jeśli widzisz `created_at`, prawie zawsze dodaj `order by created_at desc` podczas debugowania najnowszych danych.
9. Jeśli widzisz indeks wielokolumnowy, kolejność kolumn podpowiada typowe query biznesowe.
10. Jeśli dwie tabele mają kolumnę `status`, zawsze używaj aliasów i `as`, np. `payment_status`, `merchant_status`.
11. Jeśli query ma zwrócić „braki”, myśl o `left join` plus `where right_table.id is null`.
12. Jeśli debugujesz REST test create, zacznij od tabeli tworzonego zasobu, czyli `payment_orders`.
13. Jeśli debugujesz retry/idempotency, od razu dołącz `idempotency_records`.
14. Jeśli debugujesz audit/correlation id, od razu dołącz `payment_order_status_history`.
15. Jeśli query ma być stabilne w teście lub notatce, użyj `order by` i najlepiej drugiego tie-breakera, np. `payment_order_id asc`.

## 13. Typowe Błędy Początkujących

### Błąd 1: `select *` wszędzie

Szybka eksploracja jest OK, ale w notatkach i testach lepiej wybierać kolumny jawnie.

Lepiej:

```sql
select
  po.payment_order_id,
  po.amount_minor,
  po.currency,
  po.status
from payment_orders po;
```

### Błąd 2: brak `order by`

Źle:

```sql
select
  po.payment_order_id,
  po.created_at
from payment_orders po
limit 10;
```

Lepiej:

```sql
select
  po.payment_order_id,
  po.created_at
from payment_orders po
order by po.created_at desc, po.payment_order_id asc
limit 10;
```

### Błąd 3: niejednoznaczna kolumna `status`

Źle:

```sql
select
  payment_order_id,
  status
from payment_orders
join merchants on merchants.merchant_id = payment_orders.merchant_id;
```

Lepiej:

```sql
select
  po.payment_order_id,
  po.status as payment_status,
  m.status as merchant_status
from payment_orders po
join merchants m on m.merchant_id = po.merchant_id;
```

### Błąd 4: zły kierunek `left join`

Jeśli chcesz wszystkich merchantów, zacznij od `merchants`.

```sql
from merchants m
left join payment_orders po on po.merchant_id = m.merchant_id
```

Jeśli chcesz wszystkie payment ordery, zacznij od `payment_orders`.

```sql
from payment_orders po
join merchants m on m.merchant_id = po.merchant_id
```

## 14. Query Cookbook Dla Lesson 6

### Najnowsze payment ordery

```sql
select
  po.payment_order_id,
  po.client_order_reference,
  po.amount_minor,
  po.currency,
  po.status,
  po.created_at
from payment_orders po
order by po.created_at desc, po.payment_order_id asc
limit 10;
```

### Payment order + merchant

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
order by po.created_at desc, po.payment_order_id asc
limit 10;
```

### Merchantci bez payment orderów

```sql
select
  m.merchant_id,
  m.normalized_reference,
  m.status
from merchants m
left join payment_orders po on po.merchant_id = m.merchant_id
where po.payment_order_id is null
order by m.created_at desc, m.merchant_id asc
limit 20;
```

### Payment order + idempotency

```sql
select
  po.payment_order_id,
  po.client_order_reference,
  ir.idempotency_key_hash,
  ir.request_fingerprint_hash,
  ir.completed_at
from payment_orders po
join idempotency_records ir on ir.payment_order_id = po.payment_order_id
order by ir.created_at desc, ir.idempotency_record_id asc
limit 10;
```

### Payment order + audit history

```sql
select
  po.payment_order_id,
  po.client_order_reference,
  po.status as current_status,
  h.from_status,
  h.to_status,
  h.actor_subject,
  h.correlation_id,
  h.created_at as history_created_at
from payment_orders po
join payment_order_status_history h on h.payment_order_id = po.payment_order_id
order by h.created_at desc, h.status_history_id asc
limit 10;
```

### Count by status

```sql
select
  po.status,
  count(*) as order_count
from payment_orders po
group by po.status
order by po.status asc;
```

## 15. Practice Flow

1. Otwórz `V1__create_merchants.sql` i znajdź `primary key`, `unique`, `check`, `index`.
2. Otwórz `V2__create_payment_orders.sql` i znajdź wszystkie `foreign key`.
3. Dla każdego `foreign key` zapisz jedno query z `join`.
4. Uruchom query `payment order + merchant`.
5. Uruchom query `merchantci bez payment orderów`.
6. Uruchom query `payment order + idempotency`.
7. Uruchom query `payment order + audit history`.
8. Wyjaśnij własnymi słowami, które query pomaga debugować który test REST Assured.

## 16. Krótka Odpowiedź Rekrutacyjna

> I read Flyway migrations as a data map. Primary keys tell me how to identify rows, foreign keys tell me how to join tables, check and unique constraints tell me what negative cases to test, and indexes reveal common access patterns. For Lesson 6, I use joins to connect payment orders with merchants, idempotency records and audit history, which helps me debug REST API tests without replacing API-level assertions.
