# Data Learning Dataset — jak korzystać i jak się uczyć przed interview

Masz dwa światy w tej samej bazie OLTP. Ten poradnik mówi, **którego używać kiedy**, **co z trzech ticketów jest materiałem na rozmowę**, i **jak ćwiczyć tydzień po tygodniu** — nie jak czytać Spring od deski do deski.

Źródła w repo: spec `.codex/specs/data-learning-dataset.md`, ADR `.codex/adr/0001-data-learning-dataset.md`, słownik `.codex/CONTEXT.md`.

---

## 1. Co masz w ręku

| Świat | Endpoint | Po co | Rozmiar |
|---|---|---|---|
| `DeterministicDataset` | `POST /api/test/seed` | kontrakty HTTP, REST Assured, Playwright, UI | 104 płatności; historia prawie zawsze tylko `CREATED` |
| `DataLearningDataset` (`SMALL`) | `POST /api/test/seed-learning` | SQL, jakość danych, rekonsyliacja, skew, protocol tables | 10 000 płatności + checkout + audit + event_publication |
| Payment ETL lab | `POST /api/test/etl/payments/full` (plus incremental/rebuild) | staging, watermark, upsert, source-to-target recon | 10 000 `fact_payment` po full load |

Ćwiczenia ETL: [docs/data-learning/etl-migration/](../../docs/data-learning/etl-migration/01-source-target-map.md). Najpierw `seed-learning`, potem full load. Nie Spark, nie Airflow.

Zasady, których nie wolno złamać w głowie ani w bazie:

1. **Replace, nie overlay.** Learning seed czyści tabele biznesowe i ładuje populację. Nie dokleja 10 000 wierszy do 104.
2. **Nigdy nie mieszaj światów.** Po learningu `/seed` albo `/reset` przywraca świat 104 i czyści satelity (checkout, audit, event_publication). Jeśli zostawisz mixed world, liczby kłamią.
3. **Nie ładuje się sam.** Ani `dev` boot, ani Flyway, ani prod. Tylko świadomy POST.
4. **Oracle to `DataLearningTruth`.** Oczekiwane minus faktyczne, nie „mniej więcej 6000 captured”.

To jest materiał **SDET / QA engineer / backend z SQL**, nie kurs warehouse’u. Iceberg, Kafka, Spark, `MEDIUM`/`SCALE` i technicznie nielegalne wiersze OLTP są poza zakresem.

---

## 2. Jak załadować (lokalnie)

Domyślnie `app.testing.enabled=false` nawet na profilu `dev`. Bez flagi endpoint nie istnieje (404), nie 401.

### Infra + backend

Z katalogu repo:

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
```

Z `apps/backend` — profil `dev` **i** testowe endpointy:

```bash
SPRING_PROFILES_ACTIVE=dev APP_TESTING_ENABLED=true ./mvnw spring-boot:run
```

`APP_TESTING_ENABLED` mapuje się na `app.testing.enabled`.

### Seed

```bash
curl -sS -X POST 'http://localhost:8080/api/test/seed-learning' | jq
```

Opcjonalnie `?profile=SMALL` (to samo). Inny profil → **400** `application/problem+json`, detail `Learning seed profile must be SMALL`.

Oczekiwane 200:

- `operation`: `seed-learning`
- `status`: `completed`
- `truth`: dokładne liczby z tabeli poniżej (w JSON pole incomplete publications nazywa się `failedPublications`)

### SQL

```bash
psql 'postgresql://payment_quality:payment_quality_dev@localhost:5432/payment_quality_lab'
```

Pierwsze trzy zapytania — czy w ogóle jesteś w świecie learning:

```sql
SELECT COUNT(*) FROM payment_orders;                 -- 10000, nie 104
SELECT COUNT(*) FROM payment_order_status_history;   -- 28000
SELECT COUNT(*) FROM checkout_session;               -- 2000
```

Jeśli `payment_orders = 104` i checkout = 0, jesteś w świecie kontraktowym. Zrób `seed-learning` jeszcze raz.

### Powrót do testów HTTP/UI

```bash
curl -sS -X POST 'http://localhost:8080/api/test/seed'
```

Potem znowu 104 płatności i puste satelity. Dashboard po learningu jest pełny (głównie `TENANT_ALPHA`) i **nie** zgadza się z oczekiwaniami Playwright.

### Testy w Mavenie (bez ręcznego curl)

Z `apps/backend`:

```bash
./mvnw -Dtest=DataLearningDatasetIT,TestEndpointsEnabledIT,TestEndpointsDisabledIT,TestEndpointsProdSafetyIT test
./mvnw -Dtest=PaymentEtlIT,MigrationValidationIT test
./mvnw -Dit.test=PaymentEtlIT,MigrationValidationIT verify
```

Surefire (`./mvnw test`) includes only `*Test.java`. `*IT.java` is Failsafe (`./mvnw verify`). `-Dtest=PaymentEtlIT` still runs that named class on Surefire.

To jest regresja seeder’a i ETL, nie twój program SQL. Najpierw ładuj bazę i pisz zapytania ręcznie.

Po `seed-learning`:

```bash
curl -sS -X POST 'http://localhost:8080/api/test/etl/payments/full' | jq
curl -sS -X POST 'http://localhost:8080/api/test/etl/payments/incremental' | jq   # 409 bez prior SUCCEEDED
curl -sS -X POST 'http://localhost:8080/api/test/etl/payments/rebuild' | jq
```

---

## 3. Oracle — tabela prawdy SMALL

Po udanym seedzie te liczby **muszą** wyjść 1:1. Jeśli nie, albo mieszasz światy, albo patrzysz w złą tabelę.

| Metryka | Oczekiwane |
|---|---|
| tenants / merchants / payments / history | 5 / 20 / 10 000 / 28 000 |
| CAPTURED / REFUNDED / CANCELLED / AUTHORIZED / EXPIRED / CREATED | 6000 / 1200 / 800 / 800 / 400 / 800 |
| płatności `TENANT_ALPHA` i `MERCHANT_ALPHA_001` | 5500 / 5500 |
| checkout sessions / events / fulfillments / anomalies | 2000 / 5000 / 1950 / 50 |
| audit / publications / incomplete (`completion_date IS NULL`) | 10 000 / 10 000 / 100 |

Skew tenantów (płatności i audit): Alpha 55%, Platform 20%, `LEARN_TENANT_C` 15%, `LEARN_TENANT_D` 8%, `PLACEHOLDER_TENANT_ID` 2%.

Daty: `2025-01-01` … `2026-08-15`. Waluty tylko `PLN` / `EUR` / `USD`. Brak `payment_refund_approvals`.

Legalne historie płatności (i tylko te):

- `CREATED`
- `CREATED → AUTHORIZED`
- `CREATED → AUTHORIZED → CAPTURED`
- `CREATED → AUTHORIZED → CAPTURED → REFUNDED`
- `CREATED → CANCELLED`
- `CREATED → AUTHORIZED → EXPIRED`

Checkout: status sesji `CANCELED` (amerykańska pisownia, CHECK w schemacie); fulfillment może mieć `CANCELLED`. Pięćdziesiąt sesji bez fulfillmentu to **anomalia biznesowa**, nie błąd bazy.

---

## 4. Co z trzech ticketów jest materiałem na interview

Prompt implementacyjny nie był tutorialem. Był specyfikacją produktu. Na rozmowie recytujesz **decyzje i konsekwencje**, nie listę klas.

### Ticket 01 — HTTP + świat płatności

| Temat rozmowy | Co umieć powiedzieć własnymi słowami |
|---|---|
| Dwa oracłe w jednym labie | 104 wiersze = kontrakt API. 10k = SQL/DQ. Zmiana historii 104 złamałaby REST Assured/Playwright. |
| Replace vs overlay | Overlay 10k na 104 psuje testy, jeśli ktoś odpalí zły seed. Replace jest jawny. |
| JDBC z modułu testing, nie domain services | Authorize/capture 10k razy byłoby wolne, niedeterministyczne czasowo i rozszerzałoby publiczny seed API o concern nauczania. |
| `PaymentSeedCapability` nietknięty | Spring Modulith: learning adapter nie puchnie publicznego API payment. |
| Determinizm | `UUID.nameUUIDFromBytes`, zero `new Random()`, stały epoch + index. Seed N = seed N. |
| Safety 404 vs 401 | Gdy bean kontrolera nie istnieje (`testing=false` albo `prod`), łańcuch security zwraca **404**, nie 401. To świadomy test. |
| RFC 7807 | Zły `profile` → 400 `application/problem+json`, nie goły string. |
| Known doors vs synthetic crowd | `TENANT_ALPHA` / `MERCHANT_ALPHA_001` zostają, żeby Keycloak i dashboard działały. `LEARN_*` służą do skośności. |
| Skew | Nierówny rozkład to feature: agregaty i plany zapytań zachowują się inaczej niż na danych „ładnych”. |
| Stale `AUTHORIZED` | 800 wierszy w `AUTHORIZED` to legalny stan biznesowy, nie bug. |

### Ticket 02 — checkout jako protokół, nie jako HTTP

| Temat | Formułka |
|---|---|
| Seed JDBC ≠ checkout HTTP | Żeby ćwiczyć rekonsyliację, nie odpalasz pollerów ani notify. Wiersze są wstawiane. |
| Business anomaly vs technical defect | Brak fulfillmentu przechodzi CHECK/FK. Zła waluta / zły timestamp **nie** ląduje w OLTP. |
| `CANCELED` vs `CANCELLED` | Świadomy split w schemacie, nie literówka. Na rozmowie: „sprawdziłem CHECK, nie zgadywałem angielskiego”. |
| Wrong fulfillment | `COMPLETED` + `AWAITING_PAYMENT` jest legalne i **nie** jest oflagowane w `checkout_anomaly` — musisz to znaleźć SQL-em. |
| 503 ACK, retry, DUPLICATE | Stany protokołu do ćwiczenia, nie do „naprawiania seeder’a”. |

### Ticket 03 — audit JSON i niedokończona praca

| Temat | Formułka |
|---|---|
| Audit `tenant_id` to string reference | Nie UUID. Join do `tenants.tenant_reference`, nie do `tenant_id`, jeśli liczysz skew. |
| JSONB before/after | Nie każdy wiersz ma oba stany. Unnest to ćwiczenie, nie `SELECT *`. |
| `event_publication` jest syntetyczne | JDBC insert, nie `ApplicationEvent`. To **nie** jest żywy log Modulith. |
| Incomplete work | `completion_date IS NULL` = 100. JSON nazywa to `failedPublications` — nazwa API vs semantyka kolumny to klasyczne pytanie DQ. |
| Retry mix | 9000 / 700 / 200 / 100. Failure rate, retry rate, unfinished work — trzy różne mianowniki. |

### Meta-umiejętności z całego promptu (SDET senior)

To jest to, czego rekruter słucha, gdy mówi „opowiedz o testdata strategy”:

1. **Dwa oracłe, dwa endpointy, zero mieszania.**
2. **Testujesz zachowanie obserwowalne** (liczby, tożsamości, kody HTTP), nie prywatne metody generatora.
3. **Safety w produkcji:** flaga + `@Profile("!prod")` — obrona w głąb, nie jedna ifka.
4. **Granica modułu:** JDBC w `testing.internal.seed`, nie w payment domain.
5. **Słownik z grillingu:** known doors, synthetic crowd, business anomaly, technical defect, truth document.

Jeśli na rozmowie zapytają „dlaczego nie dodałeś historii do 104?” — odpowiedź jest w ADR 0001, nie w gustach.

---

## 5. Jak zacząć dziś (pierwsze 90 minut)

Nie otwieraj wszystkich klas. Zrób pętlę: **załaduj → zlicz → porównaj z truth → zapisz rozjazd**.

**0–15 min.** Compose + backend z `APP_TESTING_ENABLED=true`. `POST /api/test/seed-learning`. Zapisz JSON `truth` do pliku.

**15–40 min.** W `psql` odtwórz całą tabelę prawdy z sekcji 3. Jeśli któraś liczba nie siada, stop: mieszasz światy albo zła kolumna (`failedPublications` vs `completion_date`).

**40–70 min.** Jedno zapytanie LAG z `DataLearningDatasetIT` — nielegalne przejścia statusu. Oczekiwane: **0**. Potem celowo zepsuj warunek w głowie: „co by było, gdyby AUTHORIZED → CANCELLED było legalne?” — w tej bazie **nie jest**.

**70–90 min.** Wypisz na kartce, bez kodu:

- czym różni się `/seed` od `/seed-learning`
- dlaczego 404 a nie 401, gdy testing jest off
- jedna anomalia biznesowa vs jeden technical defect

To jest twój pitch na screening.

Dopiero potem czytaj kod, w tej kolejności:

1. `TestController.seedLearning` — HTTP i 400
2. `DataLearningTruth` + `LearningSeedResponse` — kontrakt oracła
3. `DataLearningDatasetIT` — gotowe zapytania, które wolno skopiować i zrozumieć
4. `PaymentLearningGenerator` / `CheckoutLearningGenerator` — reguły index `% n`
5. ADR 0001 — zdania na rozmowę

---

## 6. Program nauki (3 tygodnie, ~60–90 min dziennie)

Cel: umieć **zrobić i obronić** rekonsyliację na tej bazie, plus opowiedzieć decyzje testdata. Nie: przerobić cały Spring.

Każdy dzień: zapytanie, wynik, porównanie z truth albo z oczekiwaną anomalią, 3–5 zdań „co bym powiedział na interview”.

### Tydzień 1 — płatności, historia, skew (ticket 01)

**Dzień 1 — dwa światy.** Załaduj learning, zlicz, potem `/seed`, zlicz jeszcze raz (104, checkout 0). Wróć na learning. Na głos: kiedy którego seedu używasz.

**Dzień 2 — statusy i histogram.** `GROUP BY status`. Musi być 6000/1200/800/800/400/800. Dodaj `GROUP BY currency`. Tylko trzy waluty.

**Dzień 3 — okno czasu.** `MIN/MAX(created_at)` w zakresie. Histogram miesięczny (`date_trunc`). Skew w czasie jest zamierzony (index rozłożony po zakresie).

**Dzień 4 — tenant skew.** Join `payment_orders → merchants → tenants`. Alpha 5500, Platform 2000, C 1500, D 800, placeholder 200. To samo dla `MERCHANT_ALPHA_001`.

**Dzień 5 — LAG / maszyna stanów.** Zapytanie nielegalnych przejść = 0. Dopisz `ROW_NUMBER()`: ostatni `to_status` historii = `payment_orders.status`. To jest rekonsyliacja header vs log.

**Dzień 6 — długość historii.** Ile wierszy historii ma płatność w danym statusie (CREATED=1, AUTHORIZED=2, CAPTURED=3, REFUNDED=4, CANCELLED=1, EXPIRED=3). Suma 28 000.

**Dzień 7 — powtórka na głos.** ADR + tabela prawdy bez spoglądania. Dummy pytanie: „jakbyś zasiał 10k płatności w teście kontraktowym?” — odpowiedź: **nie siałbym**.

### Tydzień 2 — checkout protocol (ticket 02)

**Dzień 8 — cztery tabele.** Session / event / fulfillment / anomaly. Policz i zrób diagram na kartce: 1 sesja → N eventów → 0..1 fulfillment → 0..1 anomaly (w tym seedzie anomaly = missing fulfillment).

**Dzień 9 — anti-join.** 50 sesji bez fulfillmentu. Te same 50 w `checkout_anomaly.kind = missing_fulfillment`. To jest definicja rekonsyliacji: dwa zbiory mają być równe.

**Dzień 10 — happy vs canceled vs expired.** `COMPLETED`+`CONFIRMED`, `CANCELED`+`CANCELLED`, `EXPIRED`+`EXPIRED`. Zapamiętaj pisownię.

**Dzień 11 — protokół eventów.** `attempts > 1`, `ack_status = 503`, `process_status = 'DUPLICATE'`. Policz każdy. To nie są błędy seeder’a.

**Dzień 12 — wrong fulfillment (trudniejsze).** Sesje `COMPLETED` z fulfillmentem `AWAITING_PAYMENT`. **Nie** ma ich w `checkout_anomaly`. Napisz zapytanie, które je znajdzie. Na interview: „anomalia biznesowa nie zawsze jest oflagowana”.

**Dzień 13 — spójność kwot/walut.** Session vs event payload vs fulfillment — co da się zjoinować, czego w payloadzie JSON trzeba szukać `->>`.

**Dzień 14 — pitch checkout.** „Nie wołałem HTTP checkout, bo uczę się SQL na legalnym protokole, nie testuję pollera.”

### Tydzień 3 — audit, publication, HTTP safety, opowieść (ticket 03 + całość)

**Dzień 15 — audit skew i akcje.** Te same 55/20/15/8/2 na `audit_event.tenant_id` (string!). Distinct actions z listy merchant/payment. Outcomes tylko SUCCESS/DENIED/FAILED.

**Dzień 16 — JSONB.** Wiersze z oboma `before_state` i `after_state` (`index % 10 == 0` w generatorze — ok. 1000). `jsonb_each` / `->>` na zmianę pola `status`.

**Dzień 17 — unfinished work.** 100 incomplete. 9000 first-attempt, 700 jeden retry (`completion_attempts = 2`), 200 wiele (`>= 3`). Policz trzy wskaźniki: incomplete rate, retry rate, success-on-first-try.

**Dzień 18 — HTTP safety bez UI.** Przeczytaj `TestEndpointsDisabledIT` i `TestEndpointsProdSafetyIT`. Na głos: 404 gdy testing off; 404 na `prod` nawet gdy flaga true; 400 problem+json na złym profilu.

**Dzień 19 — granica modułu.** `DataLearningDataset` woła publiczne seed capabilities tenant/merchant do **drzwi**, JDBC do płatności i satelit. `PaymentSeedCapability` nie dostał metody `seedTenThousand`. Dlaczego.

**Dzień 20 — mixed world.** Świadomie: learning, potem `/seed`. Sprawdź, że checkout/audit/publication są puste, płatności = 104. To bug, który recenzja złapała — umiej go nazwać.

**Dzień 21 — mock interview (30–45 min).** Pytania poniżej. Odpowiadaj bez otwierania IDE. Potem sprawdź ADR i truth.

---

## 7. Ćwiczenia SQL (ściąga — rób sam, potem porównaj)

Nie wklejaj wyników z pamięci. Odpal.

**Nielegalne przejścia (oczekiwane 0):**

```sql
SELECT COUNT(*) FROM (
    SELECT to_status,
           LAG(to_status) OVER (
               PARTITION BY payment_order_id
               ORDER BY created_at ASC, status_history_id ASC
           ) AS prev
    FROM payment_order_status_history
) chained
WHERE NOT (
    (prev IS NULL AND to_status = 'CREATED')
    OR (prev = 'CREATED' AND to_status IN ('AUTHORIZED', 'CANCELLED'))
    OR (prev = 'AUTHORIZED' AND to_status IN ('CAPTURED', 'EXPIRED'))
    OR (prev = 'CAPTURED' AND to_status = 'REFUNDED')
);
```

**Header vs ostatnia historia:**

```sql
SELECT COUNT(*)
FROM payment_orders po
JOIN LATERAL (
    SELECT to_status
    FROM payment_order_status_history h
    WHERE h.payment_order_id = po.payment_order_id
    ORDER BY created_at DESC, status_history_id DESC
    LIMIT 1
) last ON last.to_status IS DISTINCT FROM po.status;
```

Oczekiwane: 0.

**Missing fulfillment = anomaly:**

```sql
SELECT COUNT(*) FROM checkout_session s
LEFT JOIN checkout_fulfillment f ON f.session_id = s.session_id
WHERE f.session_id IS NULL;
-- 50

SELECT COUNT(*) FROM checkout_anomaly a
WHERE NOT EXISTS (
    SELECT 1 FROM checkout_fulfillment f WHERE f.session_id = a.session_id
);
-- 50
```

**Incomplete publications:**

```sql
SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL;
-- 100
```

---

## 8. Pytania na mock interview

Odpowiedź ma mieć **zachowanie, decyzję, konsekwencję**.

1. Po co dwa seed’y zamiast jednego większego?
2. Co się stanie, jeśli po learningu odpalisz testy Playwright bez `/seed`?
3. Dlaczego learning nie idzie przez `authorize()` / `capture()`?
4. Czym różni się business anomaly od technical defect w tej bazie? Podaj przykład każdego.
5. Endpoint nie istnieje — 401 czy 404? Kiedy które?
6. Klient poda `profile=MEDIUM`. Co wraca i w jakim `Content-Type`?
7. JSON mówi `failedPublications: 100`. Czy to „failed” w sensie biznesowym?
8. Dlaczego `CANCELED` na sesji i `CANCELLED` na fulfillment?
9. Czy wiersze w `event_publication` oznaczają, że Spring Modulith naprawdę opublikował event?
10. Jak sprawdzisz, że seed jest deterministyczny?
11. Tenant `PLACEHOLDER` jest SUSPENDED i ma 2% płatności. To błąd danych?
12. Gdzie w kodzie jest granica, której nie wolno przekroczyć, żeby nie zepsuć kontraktu 104?

Wzorce dobrych odpowiedzi są w ADR 0001 i w słowniku `.codex/CONTEXT.md`.

---

## 9. Czego nie robić

- Nie ucz się warehouse’u na tej bazie (Iceberg, Spark, Kafka — out of spec).
- Nie wstawiaj technicznie nielegalnych wierszy „dla treningu DQ” — CHECK cię wywali, a to nie jest staging.
- Nie zmieniaj 104 fixture’ów, żeby historia była pełniejsza.
- Nie używaj learning seed jako setupu REST Assured / Playwright.
- Nie opowiadaj na rozmowie, że „wygenerowaliśmy eventy przez aplikację” — to JDBC.
- Nie myl `app.testing.enabled` z profilem `dev`. Dev bez flagi = 404.

---

## 10. Mapa plików (gdy już umiesz SQL)

| Plik | Po co czytać |
|---|---|
| `testing/internal/web/TestController.java` | HTTP, 400, bramki |
| `testing/internal/seed/DataLearningTruth.java` | oracle + `@JsonProperty("failedPublications")` |
| `testing/internal/seed/DataLearningDataset.java` | wipe, JDBC batch, kolejność insertów |
| `PaymentLearningGenerator.java` | status `i % 100`, historie |
| `CheckoutLearningGenerator.java` | `index % 40` → scenariusz |
| `AuditLearningGenerator.java` / `PublicationLearningGenerator.java` | mix retry / JSON |
| `SatelliteTableWipes.java` | dlaczego `/seed` po learningu nie zostawia śmieci |
| `DataLearningDatasetIT.java` | kanoniczne zapytania |
| `TestEndpointsEnabledIT.java` | mixed-world, problem+json |
| `.codex/adr/0001-data-learning-dataset.md` | zdania na rozmowę |

Gdy utkniesz: najpierw truth, potem IT, potem generator. Nie odwrotnie.
