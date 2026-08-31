# GOAL — Playwright POM reference architecture

Status: **READY**  
Review date: **2026-08-27**  
Review fixed point: branch `001-project-foundation`, `HEAD`
`2360dd430ad042184043363779c864b211fa8b0c`  
Reviewed tree: bieżący dirty worktree względem tego `HEAD`; nie wolno odwracać
ani nadpisywać istniejących zmian użytkownika.

## Objective

Doprowadź `apps/frontend/tests-pom` do wzorcowej, nadal prostej architektury
Playwright 1.61 + TypeScript 6:

- POM i Component Objects kapsułkują sposób obsługi UI;
- specs zachowują biznesowe oracles;
- fixture są composition root i właścicielem lifecycle zasobów;
- BFF adapter ma bezpieczny runtime contract i małe moduły domenowe;
- TypeScript modeluje możliwe stany bez rzutowań i `!` na granicach;
- testy pozostają izolowane, równoległe i diagnostyczne;
- żadna abstrakcja nie jest dodawana bez istniejącego kosztu, który usuwa.

Zatrzymaj goal dopiero, gdy wszystkie P1 i zaakceptowane P2 mają świeży dowód,
pełna wymagana walidacja jest zielona, a końcowy review nie ma otwartego P0/P1.

## Read first

1. `AGENTS.md`;
2. `.agents/skills/README.md`;
3. `.agents/skills/playwright-pom/SKILL.md` i `patterns.md`;
4. `.agents/skills/codebase-design/SKILL.md`;
5. `.agents/skills/tdd/SKILL.md`;
6. `.agents/skills/code-review/SKILL.md`;
7. `.agents/skills/playwright-sdet-review/SKILL.md`;
8. `apps/frontend/tests-pom/README.md`;
9. `.codex/research/playwright-1.61-pom-reference-architecture.md`;
10. ten dokument w całości.

Przed edycją ponownie przypnij `git rev-parse HEAD`, `git status --short` i
baseline metryk. Liczby poniżej opisują review, nie są wiecznym targetem.

## Review verdict

**REQUEST_CHANGES, bez P0.** Framework jest zdrowy funkcjonalnie i statycznie,
ale nie jest jeszcze wzorcowym modelem referencyjnym. Największe ryzyko nie leży
w locatorach, lecz na granicy typowanego HTTP i we własności zasobów.

### Fresh baseline

- `corepack pnpm typecheck:pom`: **GREEN**;
- `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom`: **GREEN**, 0;
- główne discovery: **311 testów / 74 pliki**;
- `git diff --check` dla POM/config: **GREEN**;
- około **13 154** linii TypeScript w `tests-pom`;
- `api/bff-client.ts`: **860** linii, **50** schematów, **65** metod async;
- `App.ts`: 110 linii i jeden facade dla około 33 obiektów;
- **43** ręczne `browser.newContext()` w specs/fixtures;
- **178** wywołań `requireApi(...)`;
- **60** metod `expect*` w pages/components;
- **94** tekstowe non-null assertions i **56** type assertions;
- **15** użyć `page.evaluate`, z czego część jest tylko obsługą Web Storage;
- specs nie tworzą już bezpośrednio locatorów UI przez `page`/`app.page` — ten
  dobry efekt poprzedniego refactoru trzeba zachować.

`--list` jest wyłącznie discovery, nie PASS runtime.

## Architecture map found

```text
Playwright config/projects
        |
        v
fixtures/index.ts  -------- auth setup + storageState
   |        |
   |        +-------- BffClient (65-method flat adapter)
   |
   +----------------- App facade (one Page)
                         |
                         +-- Page Objects
                         |      +-- Component Objects
                         |
specs -------------------+-- locators/actions
  |                         business assertions
  +---------------------- BFF preconditions/postconditions
  +---------------------- ad-hoc extra BrowserContexts for actors
```

Docelowo fixture pozostają jedynym composition root. `App` jest facade jednej
strony, Actor Factory jest właścicielem dodatkowych kontekstów, a `BffClient`
składa klientów domenowych nad wspólnym transportem.

## What is already exemplary — preserve

- live-stack suite bez `page.route` / `route.fulfill`;
- role/label/test-id locators i scoped locator composition;
- cienki `BasePage` oraz dziedziczenie ograniczone do wspólnego `goto` i load
  contract;
- `App` facade jako wygodny test DSL dla jednej `Page`;
- Component Objects złożone w strony, np. filters/views/confirm/kanban;
- fixture DI, setup projects i worker-scoped merchant worlds;
- realne storage state ignorowane przez Git i hasła wyłącznie z env;
- API arrange + UI act + API/network/browser oracle;
- unikalne fabryki danych oparte o `TestInfo` i UUID;
- Zod na zewnętrznej granicy JSON;
- `strict`, `noUncheckedIndexedAccess`, `noImplicitOverride` i osobny
  `typecheck:pom`;
- `retries: 0`, cap workerów oraz jawne projekty serialne tam, gdzie domena ma
  współdzielony stan;
- trace/screenshot/video na failure;
- metody `waitForBffRequest/Response` z dokładnym pathname i query;
- jawne zakazy mocków, seed-learning oraz sekretnych fallbacków.

## Findings

### P1-1 — wynik HTTP jest typowo niesoundny

Evidence:

- `api/bff-client.ts:20-24`: `RawResult<T>` ma opcjonalne `body` i `headers`;
- `api/bff-client.ts:44-55`: `requireStatus` zwraca `body as T` bez sprawdzenia,
  że body istnieje i jest wariantem sukcesu;
- `api/bff-client.ts:66-81`: overload `expectStatus` może zawęzić typ wyłącznie na
  podstawie liczby statusu;
- endpointy parsują unie success/problem, więc błędny `ProblemDetails` z 2xx nadal
  przechodzi schemat unii;
- 31 negatywnych statusów używa tego samego ogólnego `expectStatus`;
- opcjonalne pola schematów wymuszają `?.`, `??` i `!` nawet po oczekiwanym 2xx.

Impact: kompilator może zaakceptować stan, którego runtime nie udowodnił. To jest
fałszywe bezpieczeństwo dokładnie na granicy, na której test kontraktowy ma być
najbardziej rygorystyczny.

Required design:

```ts
type JsonResult<TSuccess, TError> =
  | { kind: 'success'; status: number; body: TSuccess; headers: HttpHeaders }
  | { kind: 'error'; status: number; body: TError; headers: HttpHeaders }

type EmptyResult = {
  kind: 'empty'
  status: number
  headers: HttpHeaders
}
```

Nazwy mogą się różnić, ale muszą istnieć trzy jawne stany: JSON success, JSON
error i odpowiedź bez body. Transport wybiera schemat na podstawie dozwolonych
statusów endpointu i waliduje dokładnie raz. Nie wolno użyć `as T`, by „pomóc”
kompilatorowi.

Acceptance:

- brak `body?: T` w publicznym wyniku JSON;
- status 2xx z body problemu i status błędu z body sukcesu failują z metodą,
  endpointem i błędem Zod;
- 204/304 nie udają wyniku JSON;
- success helper zawęża po `kind` i statusie bez cast;
- error helper obsługuje zarówno `ProblemDetails`, jak i istniejący merchant
  `ErrorResponse`;
- schematy mają required fields dla danych używanych po sukcesie;
- typy DTO pochodzą z `z.infer<typeof schema>` zamiast duplikować schema i type;
- publiczne metody klientów domenowych mają jawne return types.

### P1-2 — `BffClient` jest płaskim adapterem wielu domen

Evidence: jeden plik 860 linii zawiera transport, około 50 schematów i 65 metod
dla merchants, payment orders/lifecycle, support, users, audit, RLS, Event Lab,
ops feed, notifications i saved views.

Impact: każda zmiana zwiększa powierzchnię konfliktu, utrudnia discovery i łączy
niezależne kontrakty. Sam adapter jest potrzebny; jego obecna głębokość nie jest.

Required design:

```text
tests-pom/api/
  BffClient.ts             public fixture-facing composition root
  BffTransport.ts          request, headers, status/body parsing, dispose
  contracts/               shared result + truly shared schemas only
  merchants/MerchantsClient.ts
  payments/PaymentsClient.ts
  operations/OperationsClient.ts
  identity/IdentityClient.ts
  labs/LabsClient.ts
```

Dokładne grupy ustal na podstawie call sites. Nie twórz pliku dla jednego
endpointu. Preferowany publiczny DSL:

```ts
api.merchants.create(...)
api.payments.createOrder(...)
api.payments.authorize(...)
api.operations.support.createCase(...)
api.identity.users.list(...)
api.labs.eventLab.list(...)
```

`BffClient` pozostaje właścicielem `APIRequestContext` i publicznym adapterem
fixture. Migracja jest pionowa domenami; nie utrzymuj przez długi czas dwóch
pełnych API. Transport nie może zawierać endpointów domenowych.

Acceptance:

- żaden klient domenowy nie zna Playwright fixture ani `App`;
- każdy schemat ma jednego właściciela blisko endpointu;
- brak cyklicznych importów;
- nie ma forwardera 1:1 dla wszystkich 65 starych metod;
- `BffClient.dispose()` zwalnia dokładnie jeden context;
- testy kontraktowe i E2E używają tego samego publicznego adaptera.

### P1-3 — fixture nie są właścicielem wszystkich aktorów i API

Evidence:

- `fixtures/index.ts:14-20` modeluje `api` jako `BffClient | undefined`;
- `fixtures/index.ts:50-59` nie tworzy API dla guest;
- 178 call sites wykonuje `requireApi(api)`;
- 43 miejsca ręcznie tworzą `browser.newContext()`;
- wiele specs ręcznie tworzy `new App(page)` i `try/finally close()`;
- `fixtures/multi-user.fixture.ts` jest helperem zwracającym bag zasobów, a nie
  fixture z automatycznym teardown.

Impact: typ fixture nie opisuje faktycznego środowiska testu, cleanup jest
rozproszony, a każdy nowy test roli powtarza ten sam lifecycle.

Required design:

1. `api` jest zawsze `BffClient`, także dla pustego storage state. `create`
   przyjmuje prawdziwy typ Playwright storage state (path albo obiekt), nie
   ręcznie skopiowaną strukturę.
2. Usuń `requireApi` i nie zastępuj go innym guardem w każdym teście.
3. Dodaj fixture `actors` lub `actorFactory`, która:
   - przyjmuje zamkniętą `Persona`;
   - tworzy context + page + `App` i opcjonalnie domenowy BFF adapter;
   - ustawia canonical `baseURL`;
   - śledzi utworzone zasoby;
   - zamyka je automatycznie w teardown także po failure;
   - pozwala otworzyć dwie instancje tej samej persony dla concurrency/session.
4. Nie zmieniaj popupów w aktorów, jeżeli ich lifecycle wynika z
   `page.waitForEvent('popup')`.

Acceptance:

- 0 `requireApi`;
- ręczny `browser.newContext()` w specs pozostaje tylko w testach, w których sam
  surowy lifecycle contextu jest przedmiotem oracle; każda reszta używa fixture;
- brak ręcznego `new App(page)` dla zwykłego testowania person;
- teardown ma regresyjny test/proof dla exception path;
- guest API zachowuje 401 i nie dostaje credential fallbacku.

### P1-4 — lokalna granica POM/oracle jest nadal niespójna

Oficjalny Playwright dopuszcza assertions w POM. Ten projekt przyjął jednak
ostrzejszą i sensowną dla laboratorium regułę: POM odpowiada „jak”, a spec „co
oznacza wynik”. Poprzedni goal pozostawił wyjątki większe niż dokumentuje README.

Evidence:

- `MerchantsListPage.expectRowAbsent`, `expectRiskBadgeFor`,
  `expectRowVisible`, `expectCreateFieldError`;
- `OpsFeedComponent.expectConnected/expectDisconnected`;
- `EvidenceCarouselComponent.expectIndex`;
- `ProblemDetailsCard.expectError`;
- `HostedCheckoutPage.expectExpired/expectOutcome`;
- `SupportPage.expectProblem/expectResults`;
- `CheckoutLabWidgetPage.expectApprovedInFrame`;
- część metod akcji jednocześnie klika i asertuje biznesowy rezultat.

Required rule:

- POM może mieć tylko load/access/open/closed oracles niezbędne do bezpiecznego
  użycia obiektu;
- akcja może czekać na bezpośredni stan gotowości akcji, ale nie deklaruje
  biznesowego sukcesu;
- biznesowy status, wartość, liczba, obecność danych i security outcome są
  asercją spec;
- komponent udostępnia nazwany locator lub obserwowalny snapshot, nie metodę
  `expectBusinessOutcome`.

Acceptance: README wymienia dokładne wyjątki, a statyczny scan nie znajduje
innych metod `expect*` w pages/components.

## P2 improvements

### P2-1 — natywne Web Storage z Playwright 1.61

Zastąp `page.evaluate` tylko tam, gdzie kod robi wyłącznie `localStorage` lub
`sessionStorage`, przez `page.localStorage` / `page.sessionStorage`. Pozostaw
`evaluate` dla zachowania strony, DOM geometry, custom browser events i fetch w
tym samym origin, jeśli stanowią właściwy oracle.

### P2-2 — typed method data

Tabele z `readonly Row[]` migruj do `as const satisfies readonly Row[]`, gdy
literalne `id`, action, status i expected outcome powinny zostać zachowane.
Nie generuj wszystkich unii z danych, jeśli jawny domain type czyta się lepiej.

### P2-3 — locator debt

Przejrzyj pozostałe `.locator()`, `.first()` i fallback selectors. Usuń
`.first()` maskujące wieloznaczność. Każdy CSS pozostaje tylko dla:

- third-party/Nuxt UI bez semantycznego kontraktu;
- contenteditable bez roli;
- security oracle badającego węzły DOM;
- celowego sprawdzenia implementation contract.

Każdy wyjątek wymaga krótkiego komentarza „dlaczego nie role/label/test-id”. Nie
zmieniaj produkcji tylko dla kosmetyki; minimalny a11y/test hook wymaga osobnego
uzasadnienia w diffie.

### P2-4 — Network Observer zamiast rozproszonych listenerów

Dla powtarzających się `page.on('request'|'response'|'websocket')` dodaj mały,
resource-safe observer/probe, który rejestruje, wykonuje przekazaną akcję i zawsze
odłącza listener w `finally`. Nie implementuj ogólnego event busa. Negatywny
oracle powinien kończyć się na konkretnym stanie UI/akcji, nie na arbitralnym
4–5 sekundowym `setTimeout`, o ile kontrakt daje taki stan.

### P2-5 — czytelne `test.step`

- usuń nested steps; obecny README ich zabrania;
- 2–4 kroki tylko dla wielofazowej podróży;
- `test.step(row.id)` dla tabel;
- bez dekoratorów `@step` na metodach POM;
- krok może zwracać przygotowane dane, zamiast mutować zewnętrzne `let`;
- nazwa opisuje aktora i outcome.

### P2-6 — config jako dane, nie kopia-wklej

Dziewięć niemal identycznych setup projects zbuduj małą czystą funkcją/config
factory i nazwanymi stałymi zależności. Nie twórz DSL do całego Playwright config.
Zachowaj jawne projekty produktowe, bo ich różnice są znaczące.

### P2-7 — zbyt duże spec files

Podziel tylko pliki 300+ linii, gdy zawierają kilka niezależnych powodów zmiany:

- `event-lab.spec.ts` — API/security/UI;
- `merchants-table.spec.ts` — filters/sort/bulk/inline edit;
- `payments-views.spec.ts` — storage/security/API/UI;
- `merchants.spec.ts` — create/BVA/lifecycle/summary.

Nie rozbijaj jednej spójnej tabeli danych tylko dla limitu linii. Po podziale
aktualizuj project `testMatch` bez utraty discovery.

### P2-8 — dodatkowe flagi TypeScript

W osobnym checkpoint uruchom próbnie:

```json
{
  "exactOptionalPropertyTypes": true,
  "noImplicitReturns": true,
  "noFallthroughCasesInSwitch": true
}
```

Włącz je tylko po naprawie wszystkich trafień w `tests-pom`; nie dodawaj
suppression. `noUnused*` może pozostać odpowiedzialnością Oxlint.

## Pattern decision record

| Pattern | Decision | Reason |
|---|---|---|
| Page Object | KEEP | stabilny intent API nad stroną |
| Component Object | KEEP/EXPAND LOCALLY | odpowiada faktycznie współdzielonym widgetom |
| Facade / Composition Root (`App`) | KEEP | wygodny DSL jednej `Page`; konstrukcja w fixture |
| Adapter (`BffClient`) | KEEP, DECOMPOSE | właściwa granica BFF, lecz za szeroka implementacja |
| Domain Service Clients | ADOPT | grupują endpointy i schematy jednym powodem zmiany |
| Discriminated Union / Result ADT | ADOPT | modeluje success/error/empty bez cast |
| Abstract Factory + fixture teardown | ADOPT AS `ActorFactory` | usuwa 43 ręczne context lifecycles |
| Factory dla unikalnych danych | KEEP | mała i wystarczająca |
| Test Data Builder | DEFER | dodać dopiero przy wielu wariantach high-arity payloadu |
| Strategy | REJECT NOW | brak wymiennych algorytmów; persona to dane/config |
| Template Method | REJECT EXPANSION | `BasePage` ma pozostać cienki |
| Repository | REJECT | BFF client nie jest atrapą persistence |
| Singleton/global state | REJECT | niszczy izolację i równoległość |
| Fluent async chain | REJECT | łatwo ukrywa brakujące `await` i kolejność |
| Screenplay | REJECT | nieproporcjonalne do repo i jawnie poza lokalnym szkieletem |
| Automatic `@step` decorators | REJECT | zaszumione raporty; steps należą do podróży |

## Implementation program

Każdy checkpoint ma własny red/green oracle i review. Nie wykonuj wielkiego
rewrite.

### G0 — Rebaseline and characterization

Allowed writes: wyłącznie nowe testy charakterystyczne i aktualizacja tego goalu
o świeże liczby.

1. Zapisz bieżący HEAD/status, count i discovery.
2. Dodaj najmniejszy test transport/result dla:
   - success JSON;
   - problem JSON;
   - empty 204/304;
   - malformed JSON;
   - schema mismatch;
   - success status z problem body.
3. Zapisz obecne zachowanie guest/auth API i Actor contexts.

Stop gate: charakterystyka failuje z oczekiwanego powodu przed nowym modelem.

### G1 — Sound HTTP result core

Owned paths:

- `apps/frontend/tests-pom/api/contracts/**`;
- `apps/frontend/tests-pom/api/BffTransport.ts`;
- testy G0;
- minimalne importy w `bff-client.ts`.

Implementuj Result ADT, schema-by-status, shared headers i empty response. Usuń
`requireStatus` cast i niesoundny overload. Najpierw zmigruj jedną małą domenę
end-to-end jako tracer bullet.

Stop gate: wszystkie przypadki G0 green, brak `as T` w result core.

### G2 — Domain-decompose BFF adapter

Migruj w kolejności zależnej od częstotliwości i ryzyka:

1. merchants;
2. payments + lifecycle;
3. support/operations;
4. identity/settings/audit;
5. labs/event-lab/ops-feed/notifications/views.

Po każdej domenie:

- usuń stare metody i schemas z monolitu;
- zmigruj wszystkie call sites;
- uruchom typecheck/lint/discovery;
- uruchom co najmniej jeden success, jeden expected error i jeden E2E używający
  tej domeny.

Stop gate: `bff-client.ts` jest małym composition root, nie ma równoległych DTO i
schemas, publiczne metody mają jawne wyniki.

### G3 — Required API fixture and Actor Factory

1. Rozszerz BFF create o path/object storage state.
2. Zmień `api` na wymagany `BffClient`; usuń 178 `requireApi`.
3. Zaimplementuj resource-owning Actor Factory fixture.
4. Migruj najpierw `merchants-rbac-columns.spec.ts` jako tracer bullet, potem
   `payments-pin`, `tenant-scope`, `session`, concurrency i pozostałe contexts.
5. Zostaw popup-specific pages lokalnie.

Stop gate: guest 401 green, multi-role green, exception teardown green, brak
credential fallbacku.

### G4 — POM/oracle and locator convergence

1. Zamień biznesowe `expect*` na locators/snapshots.
2. Przenieś oracles do specs z custom expect messages tam, gdzie zwiększają
   diagnostykę.
3. Sklasyfikuj każdy CSS/`.first()` jako removed albo justified.
4. Zachowaj load/access/open/closed methods.

Stop gate: statyczny scan spełnia README, zmienione journeys live green.

### G5 — Idiomatic TS and reporting

1. `as const satisfies` dla method data.
2. Web Storage API 1.61 dla prostych storage operations.
3. Network Observer dla co najmniej trzech realnych call sites albo nie dodawaj
   helpera.
4. Usuń nested steps i zewnętrzne mutable `let` używane tylko do przekazania
   danych między krokami.
5. Skompresuj setup project repetition małą factory.
6. Spróbuj dodatkowych flag TS i włącz tylko na czysto.

Stop gate: zero suppression, zero mechanicznych stepów, każdy helper ma co
najmniej trzy spójne call sites albo mocny resource-safety powód.

### G6 — Spec cohesion and final review

Podziel wskazane 300+ line specs tylko po ustabilizowaniu fixture/API, aby nie
generować konfliktów. Zaktualizuj config/discovery. Przeprowadź końcowy review
osi Standards vs Goal oraz Playwright/SDET review.

Stop gate: brak P0/P1, wszystkie P2 closed lub jawnie `DEFERRED` z uzasadnieniem
i bez deklaracji completion dla odłożonej pracy.

## TDD seams

- Pure transport/result seam: response text + status + Zod schema → typed result.
- Fixture seam: guest/auth storage state → zawsze zdefiniowany client.
- Resource seam: Actor Factory zamyka context i API także po wyjątku.
- POM seam: statyczny contract methods + targeted live journey.
- Network seam: observer zawsze odłącza listener.
- Config seam: `--list` zachowuje zestaw projektów/testów.

Nie pisz testów implementacji, które tylko odtwarzają strukturę klas. Testuj
kontrakt, narrowing, lifecycle i obserwowalne zachowanie.

## Scope and invariants

Allowed by default:

- `apps/frontend/tests-pom/**`;
- `apps/frontend/playwright*.config.ts` tylko gdy dany checkpoint tego wymaga;
- `apps/frontend/tests-pom/README.md`;
- bezpośrednie testy frameworka w istniejącym, uzasadnionym seam;
- `.codex/goals/**`, `.codex/research/**`, `status/evidence/**` dla evidence.

Requires explicit user approval during implementation:

- produkcyjne `apps/frontend/app/**` i `server/**` dla nowego test hook/a11y;
- dependency/version changes;
- backend, database, Keycloak realm lub infrastruktura;
- kasowanie istniejących testów lub zmiana kontraktu produktu.

Never:

- nie modyfikuj `.kiro/**`;
- nie dotykaj learner copies `My*`/`Lesson*`;
- nie zapisuj haseł, tokenów ani storage state;
- nie dodawaj password fallbacków;
- nie dodawaj `page.route`, `route.fulfill`, HAR mocks ani `waitForTimeout`;
- nie dodawaj `as any`, `@ts-ignore`, `@ts-expect-error` ani wyłączeń lint dla
  obejścia modelu;
- nie maskuj strict mode przez `.first()`/`.nth()`;
- nie ustawiaj retries tylko po to, by „zazielenić” flake;
- nie wprowadzaj Screenplay, DI container, event bus, repository ani ogólnego DSL;
- nie zmieniaj REST/product behavior w refactorze test frameworka;
- nie revertuj istniejącego dirty worktree;
- nie commituj ani nie pushuj bez osobnej instrukcji użytkownika.

## Validation ladder

Run from `apps/frontend`, cheapest first:

```bash
corepack pnpm typecheck:pom
corepack pnpm exec oxlint --config oxlint.config.ts tests-pom
corepack pnpm exec playwright test --config playwright.pom.config.ts --list
```

Następnie per checkpoint uruchom dokładny zmieniony spec/project na live stack.
Hasła pobierz wyłącznie z env zgodnie z `tests-pom/README.md`; nigdy nie zapisuj
wartości w logu, dokumencie ani komendzie śledzonej przez repo.

Po tracer bullets:

```bash
corepack pnpm exec playwright test --config playwright.pom.config.ts \
  --project=<affected-project> <exact-spec>
```

Końcowo:

```bash
corepack pnpm typecheck
corepack pnpm typecheck:pom
corepack pnpm exec oxlint --config oxlint.config.ts tests-pom
corepack pnpm exec playwright test --config playwright.pom.config.ts --list
git diff --check
```

Uruchom pełny main POM live oraz tylko dotknięte overlaye (visual/Kafka/TLS/RLS/
Mirror). `NOT_RUN`, skip i discovery nie są PASS. Nie naprawiaj niezwiązanych
błędów pełnego frontend lint; sklasyfikuj je osobno.

## Definition of Done

1. Wszystkie P1 closed z kodem i świeżym runtime evidence.
2. `BffClient` jest małym composition root nad domain clients i jednym
   transportem.
3. Wynik HTTP nie używa niesoundnego cast i modeluje empty response.
4. Schema-first types nie duplikują ręcznych DTO.
5. `api` fixture jest wymagany i działa dla guest/auth bez fallbacku.
6. Actor Factory jest właścicielem dodatkowych contextów i teardown.
7. `requireApi` count = 0.
8. Ręczne contexts pozostają wyłącznie jako udokumentowane testy lifecycle.
9. Biznesowe assertions są w specs; POM exceptions odpowiadają README.
10. Każdy pozostały CSS/`.first()` ma realne uzasadnienie.
11. Brak nowego direct UI locator construction w specs.
12. Brak nested/mechanical steps.
13. Test matrices zachowują literal types przez `satisfies`, gdzie to istotne.
14. Proste Web Storage operations używają API 1.61.
15. POM typecheck, POM Oxlint, discovery i `git diff --check` są green.
16. Zmienione live journeys są green.
17. Full main POM i wymagane overlaye są green albo goal pozostaje incomplete.
18. Końcowy `code-review` + `playwright-sdet-review` nie ma P0/P1.
19. Final report podaje before/after metrics, komendy, exit codes, counts,
    remaining justified exceptions i każde `NOT_RUN`.

## Progress log format

Po każdym checkpoint dopisz do końca tego pliku:

```markdown
### Gx — <name> — YYYY-MM-DD

- Status: RED | GREEN | BLOCKED
- Changed: <paths>
- Design decision: <one paragraph>
- Validation: `<command>` → exit N, X passed/Y failed
- Remaining: <next concrete action>
```

Nie zmieniaj historycznych wpisów; koryguj je nowym wpisem.

## Start in Codex CLI

Oficjalny format goalu to `/goal <objective>`. Z katalogu repozytorium wklej:

```text
/goal Zrealizuj .codex/goals/playwright-pom-reference-architecture.md od G0 do Definition of Done. Pracuj checkpointami, stosuj wskazane skille, nie odwracaj istniejącego dirty worktree i nie uznawaj NOT_RUN ani --list za runtime PASS.
```

Status sprawdza się przez `/goal`; sterowanie: `/goal pause`, `/goal resume`,
`/goal clear`. Jeżeli `/goal` nie jest dostępne, oficjalna dokumentacja wskazuje
`codex features enable goals` albo `features.goals = true` w `config.toml`.

Nie uruchamiaj implementation goal automatycznie w sesji review. Ten plik jest
gotowym, jawnie zatwierdzanym wejściem do osobnego wykonania.

### G0 — Rebaseline and characterization — 2026-08-27

- Status: RED
- Changed: `apps/frontend/tests-pom/api/bff-transport.characterization.test.ts`; ten goal.
- Design decision: karakterystyka izoluje czystą granicę JSON/Zod i dowodzi obecnej luki bez uruchamiania stacku: unia `success | ProblemDetails` akceptuje `201` z ciałem problemu. Puste `204/304`, malformed JSON i schema mismatch są osobnymi przypadkami kontraktu.
- Validation: `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0; `corepack pnpm exec playwright test --config playwright.pom.config.ts --list` → exit 0, 311 discovered in 74 files (discovery only); `corepack pnpm exec vitest run --config /tmp/pom-characterization.vitest.config.mts` → exit 1, 6 passed/1 failed (expected RED: `201` problem body was accepted).
- Remaining: G1 — replace the union-based parsing path with a discriminated transport result and make this contract green without casts.

### G1 — Sound HTTP result core — 2026-08-27

- Status: GREEN
- Changed: `apps/frontend/tests-pom/api/contracts/http-result.ts`; `apps/frontend/tests-pom/api/BffTransport.ts`; `apps/frontend/tests-pom/api/bff-transport.characterization.test.ts`; `apps/frontend/tests-pom/api/bff-client.ts`; `apps/frontend/tests-pom/specs/admin-bff.spec.ts`; `apps/frontend/tests-pom/specs/tenant-scope.spec.ts`; ten goal.
- Design decision: `decodeResponse` wybiera success/error/empty wyłącznie po statusie i waliduje wtedy dokładnie jeden schema. `BffTransport` jest właścicielem tylko request/parse/dispose, a `getMerchant` jest pionowym tracer bulletem; spec otrzymuje `expectSuccess` lub `expectError`, które sprawdzają jednocześnie status i discriminant bez rzutowania.
- Validation: `corepack pnpm exec vitest run --config /tmp/pom-characterization.vitest.config.mts` → exit 0, 7 passed/0 failed; `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom/api tests-pom/specs/admin-bff.spec.ts tests-pom/specs/tenant-scope.spec.ts` → exit 0. Targeted live `admin-bff` was attempted twice: first could not connect to the HTTP port; second reached a stale/misrouted TLS stack which returned `200` with empty body for the documented unknown-merchant BFF route. Neither is PASS runtime evidence.
- Remaining: G2 — migrate merchants fully into a domain client after restoring a canonical live-stack endpoint; then run success/error/E2E proof on that stack.

### G2 — Domain-decompose BFF adapter — 2026-08-27

- Status: STATIC_GREEN / LIVE_NOT_RUN
- Changed: `apps/frontend/tests-pom/api/bff-client.ts`; `api/contracts/assertions.ts`; domain clients under `api/merchants`, `api/payments`, `api/operations`, `api/identity`, and `api/labs`; migrated POM call sites.
- Design decision: `BffClient` is now only the one-context composition root. Each domain client owns its schemas and explicit `JsonResult`/`EmptyResult` return types; RLS and Event Lab are grouped as labs, while support/audit/feed/notifications are grouped as operations. The legacy flat forwarders and duplicated schemas were removed rather than retained as a parallel API.
- Validation: `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0; `corepack pnpm exec vitest run --config /tmp/pom-characterization.vitest.config.mts` → exit 0, 7 passed/0 failed; `git diff --check -- apps/frontend/tests-pom` → exit 0. No live success/error/E2E result is claimed: the available stack/auth states remain non-canonical for the full suite and no password environment variables are available to create correct TLS states.
- Remaining: obtain canonical live-stack/auth evidence during final validation; continue G3, which does not depend on it.

### G3 — Required API fixture and Actor Factory — 2026-08-27

- Status: IN_PROGRESS / STATIC_GREEN
- Changed: `tests-pom/fixtures/index.ts`; `fixtures/actors.ts`; `api/bff-client.ts`; removal of `fixtures/multi-user.fixture.ts`; migrated `merchants-rbac-columns`, `merchants-concurrency`, `merchants-conflict`, `payments-pin`, and guest BFF coverage.
- Design decision: `api` is required for every project, including guest state. `ActorFactory` accepts only a closed `Persona`, creates matching UI and BFF resources, and fixture teardown disposes all resources after each test; it also permits two instances of the same persona.
- Validation so far: `rg requireApi tests-pom` → no matches; `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0; characteristic Vitest remains 7/7. Guest BFF assertion now uses the required fixture, but it still needs live-stack runtime proof.
- Remaining: migrate the remaining ordinary role contexts (leave only lifecycle-oracle contexts), add teardown exception proof, and run guest/multi-role live tests on the canonical authenticated stack.

### G3 — Required API fixture and Actor Factory — 2026-08-28

- Status: IN_PROGRESS / STATIC_GREEN
- Changed: `tests-pom/fixtures/index.ts`, `fixtures/actors.ts`, `api/bff-client.ts`, `specs/session-guest.spec.ts`, plus migrated role specs.
- Design decision: the regular role cases now use the fixture-owned `ActorFactory`; the only remaining `browser.newContext()` calls are the factory itself and the worker-scoped fixture. A guest regression deliberately throws after opening an actor, then proves that both the browser page and its BFF request context are closed.
- Validation: `corepack pnpm exec playwright test --config playwright.pom.config.ts --project=chromium-guest tests-pom/specs/session-guest.spec.ts -g "Actor Factory releases guest UI and BFF resources"` → exit 0, 1 passed; `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0.
- Remaining: canonical-stack runtime evidence for guest 401 and multi-role journeys; preserve the worker fixture as its documented worker lifecycle exception.

### G4 — POM/oracle convergence — 2026-08-28

- Status: IN_PROGRESS / STATIC_GREEN
- Changed: `tests-pom/pages/**`, selected `specs/**`, and `tests-pom/README.md`.
- Design decision: values, counts, status and security claims moved to specs as web-first assertions. Components expose named locators; README now enumerates the narrow load/access/open/closed exceptions that remain inside POMs.
- Validation: `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0; `git diff --check` → exit 0.
- Remaining: classify the remaining CSS/`.first()` debt and obtain focused live runtime results for the changed journeys.

### G5 — Idiomatic TS and reporting — 2026-08-28

- Status: IN_PROGRESS / STATIC_GREEN
- Changed: `tests-pom/utils/storage-safety.ts`, `specs/payments-views.spec.ts`, `tests-pom/tsconfig.json`, `playwright.pom.config.ts`.
- Design decision: direct Web Storage reads/clears now use Playwright 1.61 `page.localStorage`/`page.sessionStorage`; page evaluation remains only for browser/DOM/fetch behavior. The three stricter TypeScript flags are enabled after a clean trial, and auth setup projects share a small data-like factory.
- Validation: `corepack pnpm exec tsc -p tests-pom/tsconfig.json --exactOptionalPropertyTypes --noImplicitReturns --noFallthroughCasesInSwitch` → initial exit 2, one optional-property finding; fixed; `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0. Main discovery after config refactor → exit 0, 312 tests in 74 files (discovery only, not runtime PASS).
- Remaining: network-observer decision and three call sites, literal-table review, then focused live evidence.

### G3 — Required API fixture and Actor Factory — 2026-08-28 (follow-up)

- Status: IN_PROGRESS / STATIC_GREEN
- Changed: deleted unused `tests-pom/utils/roles.ts`.
- Design decision: the orphaned helper was the only non-fixture source that still created a browser context. It had no call sites; removing it makes ActorFactory and the worker fixture the only context owners.
- Validation: `rg -n 'browser\\.newContext\\(' tests-pom --glob '*.ts'` now finds only `fixtures/actors.ts` and the documented worker fixture.
- Remaining: canonical authenticated live evidence; the local environment has neither Docker CLI nor Playwright password variables.

### G5 — Idiomatic TS and reporting — 2026-08-28 (follow-up)

- Status: IN_PROGRESS / STATIC_GREEN
- Changed: `tests-pom/utils/network-observer.ts`, `specs/merchants.spec.ts`, `specs/merchants-slideover.spec.ts`, `specs/payments-hard-controls.spec.ts`.
- Design decision: the small observer owns `page.on`/`page.off` in `try/finally` and serves three existing negative POST oracles. It deliberately records only the action window and is not a general event bus.
- Validation: `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0; `git diff --check` → exit 0.
- Remaining: focused live evidence for the migrated negative network oracles and the final locator/table audit.

### G4/G5 — locator and literal-type follow-up — 2026-08-28

- Status: IN_PROGRESS / STATIC_GREEN
- Changed: remaining POM locator sites, `OpsFeedComponent`, three affected specs, and method tables under `tests-pom/methods/**`.
- Design decision: unambiguous heading `.first()` calls were removed. Remaining CSS/collection locators state why an accessible locator is unavailable or why collection-first semantics are intentional. Literal matrices now use `as const satisfies` where rows share a complete shape; two sparse optional-field matrices retain their explicit domain array type because literal-union access would otherwise force narrowing at every call site.
- Validation: `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0; `git diff --check` → exit 0.
- Remaining: targeted live validation and the full runtime/overlay matrix; no runtime result is inferred from these static gates.

### G6 — cohesion/static contract follow-up — 2026-08-28

- Status: IN_PROGRESS / STATIC_GREEN
- Changed: `specs/event-lab.spec.ts`, `specs/merchants-conflict.spec.ts`, and `pages/components/ConflictDiffComponent.ts`.
- Design decision: a nested Event Lab reporting sequence was flattened into three sibling outcome steps. The last direct UI locators in specs moved to the existing conflict component so tests state business outcomes through the POM surface.
- Validation: `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0; `git diff --check` → exit 0; `rg -n '\\b(?:page|app\\.page)\\.(?:getBy|locator\\()' tests-pom/specs --glob '*.ts'` → no matches.
- Remaining: flatten remaining nested Event Lab steps, decide only meaningful spec splits, then final runtime matrix and review.

### G5/G6 — Event Lab reporting and network follow-up — 2026-08-28

- Status: IN_PROGRESS / STATIC_GREEN
- Changed: `tests-pom/specs/event-lab.spec.ts`.
- Design decision: all nested Event Lab steps were flattened into sibling journey phases that return prepared data. Negative duplicate and browser-leak checks now use the resource-safe observer action window, so the tests do not wait arbitrary four/five seconds for a non-event.
- Validation: `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0; `git diff --check` → exit 0; `rg -n 'setTimeout\\(' tests-pom/specs/event-lab.spec.ts` → no matches.
- Remaining: assess only meaningful file splits, then canonical live stack and overlay runtime evidence plus final reviews.

### G3 — fixture ownership follow-up — 2026-08-28

- Status: IN_PROGRESS / STATIC_GREEN
- Changed: `tests-pom/specs/event-lab.spec.ts`, `tests-pom/specs/payments-views.spec.ts`.
- Design decision: Event Lab BFF negative cases and the payment-view cross-user case now obtain alternate sessions and BFF clients through `actors`, rather than constructing/disposal-managing raw `APIRequestContext` or `BffClient` in the spec. The fixture remains the lifecycle owner.
- Validation: `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0; `git diff --check` → exit 0.
- Remaining: migrate the remaining direct `BffClient.create` legacy call sites and obtain canonical runtime evidence.

### G3 — fixture ownership completion (static) — 2026-08-28

- Status: IN_PROGRESS / STATIC_GREEN
- Changed: `specs/internal-notes.spec.ts`, `ops-feed.spec.ts`, `support-kanban.spec.ts`, `payments-refund-dual-control.spec.ts`, `locale-workspace.spec.ts`, `payments-evidence-gallery.spec.ts`, and `merchants-slideover.spec.ts`.
- Design decision: every spec-owned alternate BFF client now comes from the factory (`Persona` or a duplicate storage state). This preserves the existing authenticated actor semantics while making fixture teardown the only owner of UI and request resources.
- Validation: `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0; `git diff --check` → exit 0; `rg -n 'BffClient\\.create\\(' tests-pom/specs --glob '*.ts'` → no matches.
- Remaining: canonical live guest/multi-role proof and full main/affected-overlay runtime matrix.

### Final static validation — 2026-08-28

- Status: GREEN (static only)
- Validation: `corepack pnpm typecheck` → exit 0; `corepack pnpm typecheck:pom` → exit 0; `corepack pnpm exec oxlint --config oxlint.config.ts tests-pom` → exit 0; `git diff --check` → exit 0; main POM discovery → exit 0, 312 tests / 74 files.
- Runtime status: BLOCKED. Docker CLI is unavailable in this WSL distribution and all required `PLAYWRIGHT_*_PASSWORD` variables are absent. Discovery is explicitly not treated as runtime PASS.
- Remaining: provide/start canonical Postgres + Keycloak + backend + Nuxt stack and password environment values, then run focused guest/multi-role tests, full main POM and each affected overlay before final review.

### Runtime checkpoint — 2026-08-28

- Status: IN_PROGRESS / RUNTIME_GREEN (focused)
- Environment: canonical compose `--app`, with browser and BFF origins explicitly set to `http://127.0.0.1:3000`, matching the container's OIDC callback configuration.
- Validation: `session-guest.spec.ts` on `chromium-guest` → 9 passed; `admin-bff.spec.ts` on `chromium-admin`, including all required Keycloak storage-state setup projects → 18 passed.
- Regression fixed: domain clients kept method-prefixed endpoint labels for diagnostics but some passed those labels as request URLs. Request paths now exclude the method. The BFF `401` error schema also accepts Nuxt's documented `error: true` envelope.
- Remaining: full main POM and affected overlay runtime matrix, followed by final reviews.

### Runtime regression follow-up — 2026-08-28

- Status: IN_PROGRESS / STATIC_GREEN / RUNTIME_INCOMPLETE
- Changed: `tests-pom/api/identity/IdentityClient.ts`, `tests-pom/utils/problem.ts`, `tests-pom/pages/AuditPage.ts`, `tests-pom/pages/MerchantsListPage.ts`, `tests-pom/specs/{checkout-lab,merchants-table,merchants,payments-pin,session-lab}.spec.ts`, and domain-client endpoint invocations.
- Design decision: endpoint labels retain the HTTP verb only for diagnostics; the actual request receives a path. The PIN component verifies automatically on six digits, so its journeys assert the resulting observable state instead of trying to click a component that has already closed. The merchant BVA asserts the POST boundary-value contract, not a stale list that is not reloaded by the modal.
- Validation: focused live guest/admin/support/payments evidence is green as recorded above; the initial full runtime run was `288 passed / 18 failed / 6 not run`, then revealed and drove these regressions. A later direct BVA rerun reached the live application but exceeded its test time while the local stack was slow; it is not counted as PASS. `corepack pnpm typecheck:pom && corepack pnpm exec oxlint --config oxlint.config.ts tests-pom && git diff --check -- apps/frontend/tests-pom apps/frontend/playwright.pom.config.ts` → exit 0.
- Remaining: obtain a fresh complete full-main runtime exit after the local stack stabilizes, run only affected overlays, then perform `code-review` and `playwright-sdet-review`; no completion is claimed before that evidence exists.

### Full-main runtime attempt — 2026-08-28

- Status: IN_PROGRESS / RUNTIME_UNSTABLE
- Validation: full main POM started with all 312 tests, four workers, canonical `127.0.0.1` browser/BFF origins, and real Keycloak setup states. Setups, all guest cases, and initial admin/BFF/audit cases passed. The run was interrupted after a systemic timeout pattern: independent UI cases exceeded their 30-second budgets and `a11y-axe` reported context teardown exceeding the same budget. Failure snapshots show expected destination pages already rendered for Command Palette cases; Docker services remained healthy. This is evidence of unstable local execution, not a PASS and not a justification to loosen global timeouts/retries.
- Remaining: repeat the runtime matrix only after the local browser/container execution is stable, then collect a complete process exit and affected-overlay evidence before final review.

### Runtime diagnosis — 2026-08-28

- Status: IN_PROGRESS / RUNTIME_UNSTABLE
- Validation: a tight `chromium-admin --no-deps` reproduction of `merchants registry has no serious axe violations` → exit 1, timeout. Read-only measurements against the same live session showed BFF `GET /api/merchants?page=0&size=20` → 200 in 694 ms; three headless registry loads completed in 4.1–6.6 s; one full Axe scan completed in 4.0 s. Three bare Playwright `APIRequestContext` create/dispose cycles completed in 1–23 ms. Temporary probes were removed after measurement.
- Design decision: no product or test timeout change follows from the evidence. Neither BFF, registry render, Axe scan nor API context disposal is a deterministic standalone cause; the failure remains specific to the full runner environment and is not counted as a passing runtime result.
- Remaining: recover a stable full-run execution environment, then restart the complete matrix and required overlays from clean runtime evidence.
