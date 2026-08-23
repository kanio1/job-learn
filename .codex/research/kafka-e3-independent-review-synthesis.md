# Synteza niezależnego review E3 Event Lab (5 perspektyw) + plan adaptacji

Status: SUPERSEDED_IN_PART (2026-08-23)
Date: 2026-08-23
User decision: ADR 0002 **ACCEPTED** with a **thinner** plan than this panel. Adopted: split RBAC, DLT terminology, read-model metadata for telescope, no `@Externalized` in shared. **Rejected as product:** E6 observability/ECharts, LIVE/PAUSED follow, Event Lab as Kafka console. Lenses is the telescope. Canonical plan: `status/roadmaps/kafka-event-streaming-lab/`.
Reviewed input: niezależny review z 2026-08-22 na `origin/001-project-foundation` @ `fd47176` (E3 Event Lab, katalog testów `docs/testing/event-streaming-lab/`, Ops Feed, ADR 0002)
Panel: tech-lead, product manager, UI/UX expert, Kafka expert, Nuxt frontend / SDET (Playwright)
Powiązane: [kafka-event-streaming-proposal.md](kafka-event-streaming-proposal.md), [kafka-event-streaming-proposal-review.md](kafka-event-streaming-proposal-review.md), [kafka-payment-business-cases-v2.md](kafka-payment-business-cases-v2.md), [ADR 0002](../adr/0002-kafka-event-streaming.md)

---

## 1. Werdykt ogólny

Niezależny review jest rzetelny (**~90% trafności**) — każdy kluczowy zarzut potwierdzono w plikach źródłowych na commitcie `fd47176`. Adopcja jego kierunku ulepszy wszystkie cztery żądane obszary: business flows (tryby Live/Learn, scenariusze z oczekiwanym rezultatem), UX (LIVE/PAUSED + świadome Follow, poprawna semantyka komponentów Nuxt UI), naukę Kafki (prawdziwe metryki konsumenta, lag per partycja, replay na własnych oczach) oraz testowanie Kafki z backendu i Playwrighta (nowe lekcje E2E bez mocków; Java+Testcontainers pozostaje oracle brokera).

Wymaga **czterech korekt merytorycznych** (§3) oraz uwzględnienia **ośmiu ustaleń**, które wyszły dopiero we własnej weryfikacji repo i których review nie widziało (§5). Stan implementacji: faza DESIGNED_NOT_STARTED (brak modułu `eventlab`, brak Kafki w `pom.xml`, brak `/admin/event-lab` i `server/api/event-lab`) — cały pakiet adaptacji jest **docs-only**; kod pozostaje zamrożony do akceptacji ADR 0002 przez użytkownika.

## 2. Weryfikacja faktów (recenzja vs rzeczywistość)

| Twierdzenie review | Dowód w repo (`fd47176`) | Werdykt |
|---|---|---|
| ADR 0002 status PROPOSED; AGENTS.md nadal „No Kafka" | `.codex/adr/0002-kafka-event-streaming.md` L1 (PROPOSED, wymaga decyzji użytkownika); `AGENTS.md` L58 | ✅ trafne |
| Read model V37 nie zawiera topic/partition/offset/key/headers/payload, a UI ma je pokazywać | Definicja **PG-2** w `.codex/research/kafka-event-streaming-proposal-review.md` L97: wyłącznie `id, consumer_group, event_id, action, target_type, target_id, tenant_ref, status, attempts, consumed_at, last_error`; tymczasem E3-S5 AC2 każe pokazywać w `USlideover` key/headers/payload i `UChip partition/offset` | ✅ **bloker potwierdzony** — kontrakt backend↔frontend realnie niespójny |
| Brak zdefiniowanego mechanizmu live update; `expect.poll` opisuje oczekiwanie testu, nie transport | E3-S5 AC6; w całym epiku brak definicji transportu; infrastruktura WS istnieje: `ops/internal/infrastructure/OpsFeedBroker.java`, `OpsFeedWebSocketHandler.java`, `OpsWebSocketConfiguration.java` | ✅ trafne |
| Jedno authority `platform:event-lab:operate` do odczytu i injectu | E3-S3 AC1 definiuje tylko `operate`; **authority odczytu nie jest zdefiniowana nigdzie** (PW-KAFKA-API-002 „forbidden bez authority" — której?) | ✅ trafne — a nawet mocniejsze niż w review |
| DLQ/DLT mieszane; `UChip` jako kontener offsetu | BC-KAFKA-05 pisze „DLQ", temat nazywa się `lab.event-lab.dlq.v1` (czyli DLT); E3-S5 AC2: „UChip partition/offset" | ✅ trafne |
| Twarde „No Kafka" rozproszone po skills poza planem E0 | Grep: `spring-modulith` (SKILL.md L54/L74 + modules.md L68), `code-review`, `rest-api-test-design`, plus `ask-engineering-flow` L41, `grilling` L28, `implementation-learning-loop` L28, `wayfinder` L62; E0-S1 AC4 planuje poprawkę tylko 3 skills | ✅ 7 plików skills, plan obejmuje 3 |

## 3. Ocena per perspektywa (z korektami)

### Tech-lead

**Tak — kierunek słuszny.** Podział „E3 = wąski proof-of-delivery, E6 = observability & scale" chroni flagship AC (BC-KAFKA-01: dowód dostarczenia per paymentOrderId ≤ 5 s) przed rozmyciem. Trzy równoległe tory (backend / BFF+Nuxt / tests-pom) pasują do sekcji *Parallel implementation* w `AGENTS.md` (max 3 delegacje, worktree'y, integrator scala). „Jedno źródło prawdy" usuwa realny dryf (7 plików skills vs 3 w planie E0).

Korekty:
1. **Sygnał live przez zdarzenie domenowe eventlab → listener w module ops → istniejący Ops WebSocket** (wzorzec jak `AuditableActionOccurred`→audit), a nie bezpośrednią krawędź konsument→WS z diagramu review. Czysty Modulith: konsument publikuje `EventLabDeliveryStatusChanged`, ops nasłuchuje i emituje ramkę.
2. Formalny punkt **zamrożenia DTO/OpenAPI** przed startem torów równoległych.
3. **Kolizja numeracji Flyway**: inny roadmap (`status/roadmaps/playwright-ops-wave-2/epics/E11-locale-workspace.md` L36) też rozważa „iam V37" — rozdzielić na task boardzie.
4. Review i E0 przegapiły **`CLAUDE.md` L145**, który również zawiera zakaz Kafki.

### Product Manager

**Tak.** Tryby **Live / Learn** + komunikat oczekiwanego rezultatu przed injectem („duplikat nie utworzy drugiego efektu") zamieniają surowe przyciski chaosu w scenariusze dydaktyczne — realna poprawa business flow operatora. Cztery skale jako *presety dydaktyczne, nie deklaracja możliwości Kafki* — uczciwe ramowanie.

Korekta zakresu: E3 z live transportem + generatorem 1–10 tys. zdarzeń puchnie ponad „operator odpowiada, czy capture dotarł". Rekomendacja: **średnia skala → E6** (odstępstwo od review, które wkłada ją w E3). Learn mode w E3 = duplicate + poison; scenariusze slow-consumer / hot-key / rebalance lądują w E6, gdzie będą metryki, które je uzasadnią.

### UI/UX

**Tak.** LIVE/PAUSED + „23 nowe zdarzenia" + świadome „Pokaż nowe/Follow" rozwiązuje najczęstszy ból tabel live (treść skacze pod kursorem). Semantyka Nuxt UI poprawna: `UChip` = wskaźnik stanu (LIVE/PAUSED), `UBadge` = status/grupa/partycja, semantyczny `<code>` = offset/eventId z akcją kopiowania, `UAlert` = DLT/degradacja połączenia.

Korekty:
1. System zakładek w `USlideover` (Summary/Journey/Kafka/Attempts/Headers) na dzień dobry może być nadmiarowy — w E3 Journey rysuje się z danych, które **już będą** (status, attempts, consumed_at + historia płatności z istniejących endpointów); pełna oś prób wymaga tabeli `eventlab_attempt` → E6.
2. Animacja Outbox→topic→partition→consumer→DB-effect: jednorazowa, stop na aktualnym etapie, `prefers-reduced-motion` — priorytet P2, opcjonalna.
3. Zachować istniejące AC sześciu stanów (loading/empty/filtered-empty/error/forbidden/not-found) oraz CSR-only.

### Kafka expert

**Tak — najsilniejsza strona review dydaktycznie.** Prawdziwe metryki konsumenta (`records-consumed-rate`, `records-lag` per partycja, `records-lag-max`) + heatmapa partycji + wykres lagu nauczą modelu pull („wolny konsument dogania"), porządku per partycja i roli klucza — dokładnie lekcji, których nie dają in-process events. Replay + unique constraint pokaże at-least-once → efektywnie once-per-grupa na własnych oczach.

Korekty:
1. Metadane Kafka w read modelu są **darmowe** — `ConsumerRecord` dostarcza topic/partition/offset/key/headers w chwili konsumpcji; rozszerzenie V37 nie kosztuje żadnej dodatkowej integracji.
2. `headers_jsonb` po whiteliście kluczy nagłówków (eventId/action/targetType/tenantRef/correlationId/occurredAt/schemaVersion).
3. `eventlab_attempt`: słuszna obserwacja, że bez niej nie da się narysować przebiegu retry — ale to E6; do proof-of-delivery wystarczą attempts + last_error.
4. Duże skale na krótkoretencyjnym `lab.event-lab.load.v1` — tak. Metryki lagu przez `AdminClient` po stronie backendu → BFF; **nigdy** przeglądarka→broker (zgodne z regułą bezpieczeństwa katalogu testów).

### Nuxt frontend / SDET (Playwright)

**Tak.** Nowe lekcje PW (dwa konteksty: obserwator otwarty przed akcją; trwałość PAUSED/FOLLOW/widoku po reload; poll metryk z miękkim oczekiwaniem + twarda asercja stanu końcowego; burst + limit DOM + zdrowie przeglądarki; HAR/trace z ramkami WS jako diagnostyka) realnie poszerzają warsztat o testowanie asynchronicznego UI **bez mocków** — spójnie z zakazami katalogu (`routeWebSocket`/`page.route`/`Thread.sleep` dalej zakazane; Java+Testcontainers pozostaje oracle brokera, retry, offsetów i rebalance).

Korekty:
1. Przed zamrożeniem tekstów zadań **zweryfikować nazwy API w release notes Playwright 1.61** (`expect.soft.poll`, `page.localStorage`/`sessionStorage`, `consoleMessages({filter})`, zapis WS w HAR) zgodnie z polityką docs-first repo.
2. Asercję limitu DOM wyrazić liczbą widocznych wierszy roli `row`, nie strukturami wewnętrznymi komponentów.
3. Architektura frontendu (refetch per `eventId`/`version` po sygnale, brak drugiego reconnect loopu, Zod-before-render) jest zdrowa i reużywa przetestowanego transportu Ops Feed — bez dublowania jego testów reconnect/malformed/offline.

## 4. Plan adaptacji (docs-only)

### A. Poprawki E0 (`status/roadmaps/kafka-event-streaming-lab/epics/E0-governance.md` + task-board)

1. AC2 rozszerzyć o **`CLAUDE.md`** (guardrails L145) — ta sama formuła „Kafka only in `eventlab` / approved overlay".
2. AC4 rozszerzyć o 4 kolejne skills: `ask-engineering-flow`, `grilling`, `implementation-learning-loop`, `wayfinder` — zamiana hardcodowanych list non-goals na odesłanie „patrz AGENTS.md / ADR 0002" (skills = *jak*, nie *co*).
3. AC5: glossary + kanoniczny termin **„dead-letter topic (DLT)"** (DLQ jako nieformalny alias, nieużywany w UI).
4. Nowe AC: dopisek do ADR 0002 (iteracja 3) — doprecyzowanie reuse Ops WebSocket: ramka `{kind, eventId, status, version}` płynie istniejącym transportem ze zdarzenia domenowego; „ops WS bez Kafki" (decyzja 7 ADR) pozostaje prawdą, bo sygnał nie pochodzi z brokera. Bez tego doprecyzowania agenci będą flagować sprzeczność.

### B. Poprawki E3 (`status/roadmaps/kafka-event-streaming-lab/epics/E3-event-lab-consumer-ui.md`)

1. **S1 — read model V37 (finalny kontrakt):** dodać `topic`, `partition_no`, `record_offset`, `record_key`, `headers_jsonb` (whitelist), `payload_jsonb`, `correlation_id`, `schema_version`, `event_timestamp` + istniejące `consumed_at`. Unique `(consumer_group, event_id)` bez zmian. RA-KAFKA-020…023 rozszerzone o asercje metadanych.
2. **S3 — split RBAC:** `platform:event-lab:read` + `platform:event-lab:operate`; aktualizacja Authorities, allowlisty konwertera, realm JSON, macierzy testów (PW-KAFKA-API-002/SEC-*).
3. **S4 — freeze:** koperta Zod/OpenAPI **v1 final** z metadanymi; zamrożenie przed fan-out torów A/B/C.
4. **Nowe S6 — live signal:** zdarzenie domenowe po commit → listener w module ops → istniejąca ramka WS; UI robi refetch per eventId/version; zero duplikacji testów reconnect/malformed Ops Feed.
5. **S5 — UI:** `UBadge`/`<code>`/`UChip` wg semantyki; LIVE/PAUSED + licznik nowych + świadome Follow; slideover minimalny (Summary + Kafka metadata + Headers/payload domyślnie zwinięte); Journey z danych istniejących; animacja P2 opcjonalna.
6. **Skala:** jawna deklaracja — E3 = mała skala (rzeczywiste lifecycle events); średnia/duża/ogromna → E6. *(Alternatywa do decyzji użytkownika: generator ≤10 tys. w E3 wg review.)*
7. Nowe lekcje PW z tabeli review dopisane do AC + katalogu testów (nowe ID po PW-KAFKA-E2E-006), **po weryfikacji API 1.61 w release notes**.

### C. Nowy epik E6 — Observability & Scale (szkielet)

Telemetry buckets/snapshots serwerowo; endpoint lagu przez `AdminClient` → BFF; tabela `eventlab_attempt` z retencją; generator deterministyczny (skala średnia); load topic `lab.event-lab.load.v1` (skala duża); buckety 1–5 s + bounded reservoir sample; ECharts **po odrębnej akceptacji zależności**; nightly performance suite; rozszerzony Learn mode. Taski robocze od `KAFKA-T19` (potwierdzić wolne numery na task-boardzie).

### D. Spójność przekrojowa

Task-board (rozdzielenie V37 vs „iam V37" z E11), `README.md` roadmapy (tabela epików + E6), `docs/testing/event-streaming-lab/README.md` (cross-ref skal i nowych PW ID), `status/index.md` (nota o adaptacji review).

## 5. Wartość dodana ponad review (z własnej weryfikacji)

1. **`CLAUDE.md` L145** — zawiera zakaz Kafki; pominięty zarówno przez review, jak i plan E0.
2. **4 dodatkowe skills z twardymi zakazami** (`ask-engineering-flow`, `grilling`, `implementation-learning-loop`, `wayfinder`) — potwierdzone liniami; E0 planuje tylko 3.
3. **Authority odczytu w ogóle niezdefiniowana** w dokumentach — silniejszy argument za splitem RBAC niż sformułowany w review.
4. **Napięcie z decyzją 7 ADR** („ops WS zostaje bez Kafki") — reuse WS wymaga jawnego doprecyzowania w ADR, inaczej agenci będą flagować sprzeczność.
5. **Kolizja numeracji Flyway V37** z roadmapem E11 („iam V37").
6. **Czysta ścieżka Modulith dla sygnału live** (zdarzenie domenowe → listener ops) zamiast krawędzi konsument→WS.
7. **Historyczne `specs/**` pozostają zamrożone** — poprawiamy wyłącznie aktywne dokumenty roadmapy (spójne z polityką `.kiro`).
8. **Dokumentacja wyprzedza implementację** — freeze DTO przed fan-out ogranicza ryzyko dalszych zmian kontraktu.

## 6. Kolejność wykonania

1. **Gate 0 (użytkownik):** decyzja co do ADR 0002 — ACCEPTED / REJECTED / zostaje PROPOSED. Bez tej decyzji nic dalej nie rusza (tak stanowi sam ADR i E0).
2. **Pakiet docs-only:** jeden zestaw zmian `.md` (E0, E3, szkielet E6, task-board, katalog testów, README roadmapy, status/index). Wykonuje go integrator sam — małe pliki o wysokim potencjale konfliktów, nie do delegowania. Walidacja: przegląd diffów, spójność ID testów, `git diff --stat` pokazuje wyłącznie `.md`.
3. **Dopiero potem:** egzekucja E0 (edycje governance) → E1 (broker overlay) → E2 (outbox→topic) → E3 wg poprawionego epiku; tory A/B/C ruszają po zamrożeniu DTO.
4. Przed napisaniem tekstów zadań PW: weryfikacja release notes Playwright 1.61 (polityka docs-first).

Ryzyka:
- Rozrost E3 → twarda granica skal; attempt-table i generator w E6.
- Flaky testy live-signal → WS traktowany jako optymalizacja; oracle = read model (`expect.poll` ≤ 5 s), Awaitility ≤ 10 s po stronie Javy.
- Dokumenty wyprzedzają kod → freeze DTO; zmiany kontraktu po starcie implementacji wymagają noty w spec.
- Deklaracje API Playwright 1.61 z review → obowiązkowa weryfikacja w źródłach przed użyciem w zadaniach.

## 7. Decyzje otwarte (dla użytkownika)

1. Akceptacja ADR 0002 — gate wszystkich dalszych prac.
2. Zgoda na utworzenie szkieletu E6 Observability & Scale.
3. Umiejscowienie skali średniej — rekomendacja panelu: E6; alternatywa: generator ≤10 tys. w E3 (wg review).
4. ECharts — odrębna zgoda na zależność, dopiero przy realizacji E6.

---

*Niniejszy dokument jest wyłącznie analizą — nie zmienia epików, katalogu testów ani governance. Implementacja (również docs-only pakiet z §4) wymaga jawnej akceptacji użytkownika.*
