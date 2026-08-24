# GOAL — Naprawa Event Streaming Lab wg expert review (REQUEST_CHANGES)

Repozytorium: `/home/suso/job-learn`, branch `001-project-foundation`, HEAD `7f0e54d` (Commit 2), BASE `6ee68272`, Commit 1 `364f520`.
Dokument nadrzędny: `docs/testing/event-streaming-lab/expert-review-results.md` (9xP1, 6xP2, 2xP3).
Plan szczegółowy (Z1..Z19) w pliku plan-mode tej sesji (zawiera też ten prompt).

Ten plik to gotowy argument dla `grok /goal <tekst>` (tryb goal) albo dla headless
`grok -p "$(cat .codex/goals/event-lab-review-fix.md)" --yolo`.

## Cel

Wykonaj plan naprawy Event Streaming Lab zgodnie z werdyktem REQUEST_CHANGES w
`docs/testing/event-streaming-lab/expert-review-results.md`: zamknij wszystkie findings
P1 (9), P2 (6), P3 (2) z testowymi oracle, świeżymi zielonymi wynikami i dowodami w
`status/evidence/`. Stosuj best practices i design patterns dla Java i TypeScript
oraz skille wymienione niżej.

## Zasady (nigdy nie łam)

1. Każde zadanie realizowane z właściwym skillem: `spring-modulith`, `eventlab-kafka`,
   `java-spring-review`, `rest-api-test-design`, `tdd`, `nuxt-frontend`,
   `playwright-pom`, `playwright-sdet-review`, `ponytail-review`, `code-review`.
2. Zamknięcie findingu = świeżo zielony oracle (komenda + SHA + log w
   `status/evidence/`). `NOT_RUN` / `ENVIRONMENT_BLOCKED` nie są PASS — status
   `IMPLEMENTED_NOT_EXECUTED` do czasu zielonego.
3. Nie uruchamiaj `restkit/**` ani `paymentsupport/**`; nie edytuj `.kiro/**`;
   zakaz `page.route`, `routeWebSocket`, `Thread.sleep`, `kafkajs` w frontendzie.
4. Commit hygiene: commity rozdzielone tematycznie — (a) produkt backend,
   (b) frontend/BFF/POM, (c) testy/oracle, (d) skill governance, (e) evidence/status.
   Zamyka P1-2.
5. Zmiany BFF/UI weryfikuj w przeglądarce (playwright-cli + narzędzia przeglądarki).

## Zakres zadań (odpowiedź na P1..P3)

- P1-1: cofnij przeszacowane PASS/DONE do `PARTIAL`/`IMPLEMENTED_NOT_EXECUTED`
  w `status/roadmaps/.../task-board.md`, `status/index.md`, `.codex/current-state.md`,
  `docs/testing/event-streaming-lab/01-acceptance-cases.md`; usuń deklaracje
  „25/25” i „31/31” do czasu świeżych zielonych wyników.
- P1-3 (BFF): trasa `server/api/event-lab/index.get.ts` ma zwracać `backendApi(...)`
  (jest) + testy tras: 200 + body-tablica; 404/403/5xx problem; `X-Correlation-ID`
  forward; nieznany query → 400.
- P1-4 (Surefire): `EventLabRestAssuredTest` → `EventLabRestAssuredKafkaIT`
  (Failsafe `*IT`); `./mvnw test` bez startu kontenera Kafka.
- P1-5 (inject; główny): nowy beam `EventLabKafkaPublisher`
  (`eventlab/internal/application`, @Profile kafka + @ConditionalOnProperty)
  z `KafkaTemplate<String,byte[]>`; `inject/duplicate` i `inject/poison` publikują
  przez prawdziwy pipeline (poison → retry → DLT → DEAD), **bez** bezpośredniego
  `setStatus("DEAD")`/save w controllera.
- P1-6 (KafkaIT): realne oracles — flag-off zero connections (spy factory, bez
  topics+KafkaConsumer), ordering przez broker offset, prawdziwe lifecycle
  REST authorize/capture/refund, replay przez restart/seek, purge tylko starych,
  rebalance 2 realne kontenery; nie połykać fault.
- P1-7 (Playwright): rozdziel empty/forbidden; no-POST przez `waitForRequest`;
  payment detail page → karta delivery `PROCESSED`; poison przez real publish;
  SEC-003 request-level bez `Authorization`/kafka URL; bez `page.route`.
- P1-8 + P1-9 (retry+DLT): decyzja w ADR — ograniczenie orderingu do main-topic
  (non-blocking) albo blocking; test same-key fail-first; `@DltHandler` → rethrow
  (usunąć catch-all, zostaje wąski unique-constraint).
- P2: topic manifest = dokładnie 3 topice (usunąć default DLT); usunąć spring-retry
  + @EnableRetry; runbook „attempts=3 = entry + 2 retries, 500 ms”; root
  `META-INF/...AutoConfiguration.imports` usunąć; naprawić broken symlink skills;
  usunąć root `pnpm-lock.yaml`.
- P3: trailing whitespace w `02-lenses-telescope.md`; „When Not to Use” na zmienianych
  skille (validator 0 warnings).

## MCP

Używaj `context7` (Spring Kafka 4.0 RetryableTopic/BackOff/attempts, Spring Boot 4,
Modulith events, Nuxt 4/Nitro BFF, spring-retry) i `firecrawl` (docs spring-kafka
4.0.x, kafka.apache 40, Boot 4.0) dla każdych wersyjnych wniosków; notka research
do `status/evidence/`.

## DONE-gate

Po wykonaniu wszystkich zadań:
- backend: `./mvnw test` (bez brokera) + `./mvnw verify` Failsafe (bez restrict/pay);
- frontend: typecheck + lint + unit + live POM (jeśli stack wstanie);
- `git diff --check` GREEN; Modulit; skill validator; final code-review;
- status/evidence + task-board zawierają świeże SHA i liczby;
- raport ponytail net-minus.

Zakończ PIN wynikiem: co zielone, co NIE_RUN, SHA, komendy, evidence paths; nie
deklaruj pełnego DONE dopóki P1-5 i oraci nie są realnie zielone.

---

# Zadania (Z1..Z19) — źródło planu

Z1 baseline+split, Z2 świeże evidence, Z3 cofnij PASS/DONE, Z4 Surefire→Failsafe,
Z5 BFF testy, Z6 inject przez prawdziwa Kafke, Z7 KafkaIT real oracles,
Z8 Playwright, Z9 retry/order+DLT, Z10 topic manifest 3 top, Z11 spring-retry usun,
Z12 runbook count, Z13 META-INF, Z14 symlink skills, Z15 root pnpm-lock, Z16 WS,
Z17 skills warnings, Z18 final gate+evidence, Z19 ponytail sweep.
Kolejność: Z1 → Z2+Z3 → (Z4||Z5) → Z6 → Z7 → Z8 → Z9 → Z10..Z13 → Z14/15 → Z16 → Z17 → Z18 → Z19.

## Zakres-nie

Bez nowych produkt features: dashboardy, ECharts, Schema Registry/Streams/EOS,
Kafka UI/AKQ; Lenses = teleskop; bez modyfikacji `.kiro/**`.