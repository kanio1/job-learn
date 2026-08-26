# Playwright `tests-pom` — audyt i program quality hardening

Status: READY FOR IMPLEMENTATION
Category: enhancement
Audit date: 2026-08-24
Audited branch: `001-project-foundation`
Scope: `apps/frontend/tests-pom/**`, wszystkie aktywne `playwright*.config.ts`, powiązane skrypty pakietu i dokumentacja testowa

## Executive Summary

Live Playwright POM ma dobrą bazową architekturę: działa na realnym Keycloak/Nuxt BFF/Spring/Postgres, używa `App` facade, własnych fixtures, storage state, danych unikalnych per worker i separuje Playwright E2E od Playwright REST/BFF. Wszystkie 72 pliki specyfikacji są osiągalne przez co najmniej jedną konfigurację, a audyt nie znalazł mockowania sieci przez `page.route`, `route.fulfill`, sztucznych opóźnień `waitForTimeout`, `test.only` ani `as any`.

Program nie jest jednak gotowy na rolę niezawodnej bramki jakości. Najważniejsza luka to brak rzeczywistego typechecku dla `tests-pom`: `nuxt typecheck` wyklucza cały katalog, a Playwright jedynie transformuje TypeScript. Diagnostyczny strict compile ujawnił 198 błędów, w tym dwa pewne błędy kontraktu POM, które mogą zakończyć test w runtime. Dalsze ryzyka dotyczą typów `BffClient`, semantyki retry/trace, ponawiania mutującej akcji w `toPass`, przeciekających granic POM, haseł workerów, dużego WIP Event Lab, małej liczby czytelnych `test.step` i nieużywanych artefaktów metod testowych.

Rekomendacja: wykonać osiem zależnych ticketów w kolejności opublikowanej pod `.codex/tickets/playwright-tests-pom-quality-hardening/`. Najpierw zamknąć P0 i ustanowić zielony strict typecheck, następnie utwardzić typy/fixtures, semantykę akcji, konfigurację i bezpieczeństwo, dopiero potem wykonywać szerokie porządki czytelności. Każdy etap kończy się targeted validation, przeglądem diffu i zapisem evidence; pełny live run jest obowiązkowym finalnym oracle, ale wolno go uruchomić wyłącznie przy działającym stacku i hasłach przekazanych przez środowisko.

## Problem Statement

Jako maintainer i SDET potrzebuję, aby `tests-pom` był kompilowalnym, deterministycznym i czytelnym frameworkiem Playwright, którego zielony wynik faktycznie dowodzi jakości aplikacji. Obecnie standardowy frontend typecheck nie obejmuje frameworka POM, część błędów wychodzi dopiero podczas wykonania, niektóre abstrakcje ukrywają oracles lub ponawiają mutacje, a konfiguracja nie zawsze produkuje oczekiwane artefakty diagnostyczne.

## Solution

Wprowadzić osobną, strict bramkę TypeScript dla całego live POM, naprawić błędy kontraktów i typów bez suppressions, ujednolicić wynik HTTP i lifecycle fixtures, rozdzielić actions od business assertions, usunąć retry mutacji, ustawić spójną politykę trace, wymusić sekrety z env, uporządkować Event Lab oraz dodać `test.step` tylko na poziomie znaczących etapów biznesowych. Na koniec udowodnić kompletność przez typecheck, lint, discovery wszystkich konfiguracji, targeted live tests i jeden kontrolowany pełny live run.

## User Stories

1. Jako maintainer chcę, aby każda specyfikacja, fixture, page object, klient BFF i konfiguracja Playwright przechodziły strict TypeScript, abym wykrywał nieistniejące metody przed runtime.
2. Jako SDET chcę, aby page objects opisywały intencje użytkownika, a specyfikacje zachowywały business oracles, abym widział co i dlaczego test sprawdza.
3. Jako osoba diagnozująca failure chcę zawsze dostać użyteczny trace dla nieudanego runu, abym nie musiał odtwarzać problemu w ciemno.
4. Jako maintainer live suite chcę, aby pojedyncza mutacja była wykonywana najwyżej raz na próbę testu, abym nie maskował duplikacji, idempotency i flakiness.
5. Jako maintainer API tests chcę typowanych, zawężanych odpowiedzi BFF, abym nie opierał kontraktu na `body!`, szerokich castach i opcjonalnych polach dla wszystkich statusów.
6. Jako reviewer chcę business-level `test.step` w długich journey, abym dostawał raport odpowiadający etapom procesu, a nie surowej sekwencji kliknięć.
7. Jako operator CI chcę jednoznaczną macierz discovery i walidacji wszystkich konfiguracji, abym nie miał pustych albo nieuruchamianych projektów.
8. Jako właściciel danych testowych chcę, aby wszystkie hasła były wymagane z env, a storage state pozostawał ignorowany przez Git.
9. Jako autor metod testowych chcę, aby decision tables, BVA, state i metamorphic oracles były rzeczywiście konsumowane albo usunięte, abym nie utrzymywał martwej dokumentacji w kodzie.
10. Jako zespół chcemy zamknąć program tylko po niezależnym review i świeżych dowodach, bez utożsamiania `--list` z wykonaniem testów.

## Audit Scope and Method

Audyt obejmował strukturę 72 plików `*.spec.ts`, page objects i components, `App` facade, fixtures, auth/storage state, fabryki danych, `BffClient`, katalog `methods`, główną konfigurację POM oraz konfiguracje learner, TLS, RLS flag-off, RLS Spring-off i Mirror flag-off. Sprawdzono także skrypty `package.json`, relację z generowanym Nuxt tsconfig, statyczne anty-patterny, discovery projektów, lint i zgodność z repozytoryjnymi skills.

Zastosowane perspektywy: Standards vs Spec z `code-review`, E2E/BFF z `playwright-sdet-review`, architektura POM z `playwright-pom`, HTTP assertion/data isolation z `rest-api-test-design`, minimalizm z `ponytail` oraz dokumentacja wersjonowana Playwright 1.61.0.

Ograniczenia dowodowe:

- worktree był już brudny, szczególnie w Event Lab i głównej konfiguracji; audyt niczego nie cofał;
- live suite nie została uruchomiona, ponieważ mutuje realną bazę i wymaga jawnych haseł;
- `playwright --list` dowodzi discovery, nie zachowania;
- pełny `pnpm lint` był czerwony przez istniejący, niezwiązany błąd w produkcyjnym `app/utils/paymentViewsStorage.ts`; POM-only lint zakończył się kodem 0 z 126 warnings;
- liczby callsites są snapshotem i mogą się zmieniać wraz z równoległym WIP.

## Current Evidence

| Obszar | Wynik audytu | Interpretacja |
|---|---:|---|
| Pliki spec | 72 / 72 osiągalne | Brak osieroconej specyfikacji w aktywnych configach |
| Główna konfiguracja | 332 przypadki w 75 plikach, razem z setupami | Discovery jest szerokie, ale nie jest live PASS |
| Visual | 11 przypadków | Osobna ścieżka przez flagę |
| TLS | 10 przypadków | Osobna konfiguracja |
| RLS flag-off | 3 przypadki | Overlay wykrywa testy |
| RLS Spring-off | 2 przypadki | Overlay wykrywa testy |
| Mirror flag-off | 5 przypadków | Overlay wykrywa testy |
| Strict diagnostic compile | 198 błędów | P0: brak kompilacyjnej bramki POM |
| POM-only lint | exit 0, 126 warnings | Dług techniczny, koncentracja w Event Lab |
| `test.step` | 6 callsites w 3 plikach | Za mało dla długich journey, ale nie należy opakowywać każdego kliknięcia |
| Locator snapshot | ok. 281 `getByTestId`, 154 `getByRole`, 35 `getByLabel` | Test IDs są dominujące; nie jest to automatycznie błąd |
| Bezpośrednie działania na `page` w specs | dziesiątki callsites | Granica POM jest niejednolita |
| `BffClient` | 781 linii, ok. 65 metod | Pressure signal; wymaga poprawy kontraktów, nie automatycznego frameworka |
| Event Lab spec | 484 linie; 56 z 126 warnings w snapshotcie audytu | Największy hotspot czytelności i typów |

## Findings and Specialist Recommendations

### F-01 — P0: `tests-pom` nie jest objęty typecheckiem

Evidence: `package.json` uruchamia `nuxt typecheck`, a generowany `.nuxt/tsconfig.json` jawnie wyklucza `../tests-pom/**` i `../tests-pom-learner/**`. Playwright transformuje TypeScript, ale nie zastępuje `tsc --noEmit`.

Ryzyko: błędy interfejsów, fixture scope, imports i nullable bodies trafiają do runtime lub zostają wykryte dopiero w wybranym projekcie.

Zalecenie implementacyjne:

- dodać osobny `tests-pom/tsconfig.json`, rozszerzający ustawienia Nuxt/TypeScript, ale nadpisujący `include`/`exclude` tak, by obejmować całe `tests-pom/**` i aktywne `playwright*.config.ts`, a wykluczać learner copies i generowane `.auth`;
- wymusić co najmniej `strict`, `noEmit`, `noUncheckedIndexedAccess`, `noImplicitOverride` i spójną rozdzielczość modułów; `skipLibCheck` może pozostać zgodny z repo, ale nie może maskować błędów projektu;
- dodać `typecheck:pom`; nie włączać go pod istniejący `nuxt typecheck` w sposób ukrywający, który etap poległ;
- doprowadzić pełny check do zielonego wyniku bez `@ts-ignore`, `@ts-expect-error` bez udowodnionego zewnętrznego błędu, `as any` i non-null assertions użytych zamiast zawężenia;
- w CI wykonywać `typecheck` i `typecheck:pom` jako osobne, nazwane gates.

Acceptance point: cel nie jest spełniony, dopóki błąd w dowolnym spec/page/fixture/API/config powoduje czerwony `typecheck:pom`.

### F-02 — P0: dwa pewne błędy kontraktu POM

Evidence:

- `merchants-table.spec.ts` wywołuje `app.merchants.caption()`, którego `MerchantsListPage` nie deklaruje;
- `merchants.spec.ts` wywołuje `app.payments.runExpirationSweep()`, którego `PaymentsListPage` nie deklaruje.

Zalecenie implementacyjne:

- dodać w `MerchantsListPage` jawny locator `caption()` oparty na istniejącym `merchant-registry-caption`;
- dodać w `PaymentsListPage` intent action `runExpirationSweep()` klikający istniejący `run-expiration-sweep`; HTTP oracle zostaje w specyfikacji;
- nie obchodzić problemu przez `app.page.getByTestId(...)` w spec ani przez cast;
- uruchomić dwa dokładnie dotknięte testy po zielonym typechecku.

### F-03 — P1: niespójne typy Playwright fixtures

Evidence: `Playwright` jest importowany z `@playwright/test`, choć stabilnym źródłem typu jest `playwright`; worker-scoped fixture przekazuje `WorkerInfo`, podczas gdy helper deklaruje `TestInfo`.

Zalecenie implementacyjne:

- typ `Playwright` importować z `playwright`, a typy runnera (`Browser`, `TestInfo`, `WorkerInfo`) z `@playwright/test`;
- helper worker-scoped powinien przyjmować minimalny structural type potrzebny do obliczenia indeksu albo dokładny `WorkerInfo`; nie rozszerzać go do `TestInfo` tylko dlatego, że oba mają podobne pola;
- ograniczyć zależność helpera od runner context do `parallelIndex` i jawnie udokumentować modulo `POM_WORKER_COUNT`;
- sprawdzić teardown `BffClient.dispose()` oraz browser context w success i failure path.

### F-04 — P1: `BffClient` ma słabe kontrakty odpowiedzi

Evidence: klient liczy 781 linii i około 65 metod, zwraca liczne ad-hoc obiekty, modeluje sukces i Problem Details przez przecięcie opcjonalnych typów, używa szerokich `response.json().catch(...)`, castów oraz nullable/optional pól tam, gdzie status rozstrzyga shape.

Ryzyko: test może przejść z błędnym body, a TypeScript wymusza `body!` lub nie sprawdza status-specific invariants.

Zalecenie implementacyjne:

- wprowadzić mały wspólny `HttpResult<T>`: `status`, znormalizowane headers, `raw`, `body: unknown | T`; nie budować drugiego HTTP frameworka;
- po asercji statusu zawężać shape przez lekkie type guards lub istniejące Zod schemas na granicy szczególnie ważnych BFF contracts;
- oddzielić reusable DTO od `ProblemDetails`; nie modelować sukcesu jako `Success & ProblemDetails`;
- dodać jeden helper do bezpiecznego JSON/empty-body i używać go konsekwentnie dla 204/304/HEAD/HTML error;
- zachować business assertions w specs; klient ma transportować i parsować, nie deklarować PASS;
- podział na domenowe clients robić tylko metodą expand → migrate → contract i tylko jeśli zmniejsza rozmiar/duplikację. Preferowana minimalna ścieżka to typy + helpers w obecnym facade, nie natychmiastowe przepisywanie wszystkich callsites.

### F-05 — P1: niespójna polityka retries i trace

Evidence: aktywne configi ustawiają `retries: 0` oraz `trace: 'on-first-retry'`. Przy zerowej liczbie retry taki trace nie powstanie.

Zalecenie implementacyjne:

- dla mutującego live lab utrzymać `retries: 0` i ustawić `trace: 'retain-on-failure'` we wszystkich aktywnych konfiguracjach;
- screenshot `only-on-failure` i video `retain-on-failure` zachować;
- nie wprowadzać retry w CI, dopóki wszystkie testy nie są udowodnione jako retry-safe i nie używają run-scoped danych;
- dodać config/discovery gate dla każdej konfiguracji i kontrolę, że każdy wymagany projekt wykrywa co najmniej jeden test;
- nie abstrahować configów wspólną fabryką, jeśli zysk to tylko kilka linii i gorsza czytelność wyjątków TLS/RLS.

### F-06 — P1: mutujący click jest ponawiany przez `expect().toPass()`

Evidence: `PspRedirectSimulatorPage.approve()` umieszcza kliknięcie approve wewnątrz callbacka retry.

Ryzyko: wiele POST/redirectów w ramach jednej próby, maskowanie idempotency bug, flake zależny od timingu.

Zalecenie implementacyjne:

- zarejestrować oczekiwanie na rezultat przed akcją;
- kliknąć dokładnie raz;
- polling/retry zastosować wyłącznie do obserwacji końcowego outcome albo odpowiedzi sieciowej;
- dodać oracle, że mutujący request wystąpił dokładnie raz w obrębie próby.

### F-07 — P1: granica POM jest niejednolita, a część assertions jest ukryta

Evidence: specs wykonują liczne bezpośrednie akcje/nawigacje przez `page`/`app.page`; jednocześnie `SavedViewsComponent.setDefault()` asertuje HTTP 200 wewnątrz component object.

Zalecenie implementacyjne:

- page/component objects posiadają locators, state-readiness assertions i intent actions;
- spec posiada business assertions: status HTTP, liczba requestów, status domeny, text będący wymaganiem;
- metody takie jak `setDefault` powinny zwrócić `APIResponse`/status albo wykonać tylko akcję, a spec jawnie deklaruje expected status;
- bezpośredni `page` w spec pozostawić dla unikalnego browser primitive lub jawnego network oracle; powtarzalne UI interactions przenieść do POM;
- nie tworzyć metody POM dla pojedynczej, jednorazowej asercji, jeśli nazwa nie wnosi intencji;
- refaktoryzować per journey, nie mechanicznie cały katalog naraz.

### F-08 — P1: `event-lab.spec.ts` jest hotspotem WIP

Evidence: 484 linie, około 56 lint warnings w snapshotcie, unresolved `import('playwright').Request` types, casts, unused variables/fixture, powtarzalny pipeline create → ETag → authorize → poll i prawie brak `test.step`.

Zalecenie implementacyjne:

- najpierw zachować i zintegrować istniejący dirty diff; nie cofać równoległych napraw Event Lab;
- wyodrębnić mały typed scenario helper zwracający wyłącznie dane potrzebne testowi (merchant id, payment id, event id, ETag/status);
- pozostawić assertions w specs; helper nie może ukrywać oracles dotyczących DLT, duplicate lub tenant masking;
- usunąć casts przez prawidłowy `Request` import i bezpieczne response parsing;
- podzielić plik tylko według odrębnych odpowiedzialności/projektów, gdy testMatch nadal gwarantuje discovery; sensowny podział: BFF API, operator E2E, security/tenant;
- każdy długi journey opisać 2–5 business-level steps;
- targeted live validation musi objąć dokładnie dotknięte Event Lab projekty na stacku `--kafka`.

### F-09 — P1: worker passwords mają fallback do username

Evidence: standardowe konta używają `requiredEnv`, ale worker manager korzysta z `optionalEnv(..., username)`, mimo deklaracji „Passwords only via environment variables”.

Zalecenie implementacyjne:

- wszystkie `PLAYWRIGHT_*_PASSWORD` pobierać przez `requiredEnv`;
- usernames mogą mieć bezpieczne, publiczne defaults;
- komunikat błędu preflight ma wymieniać brakującą nazwę zmiennej, nigdy wartość;
- nie logować env ani storage state;
- potwierdzić przez `git check-ignore`, że `.auth` pozostaje ignorowane.

### F-10 — P2: raporty mają zbyt mało `test.step`

Evidence: tylko sześć statycznych callsites w trzech plikach (`command-palette`, `merchants`, `payments-create`).

Zalecenie implementacyjne:

- dodać 2–5 kroków do długich testów wieloetapowych: arrange przez API, wejście i akcja UI, obserwacja sieci/statusu, końcowy oracle;
- nazwy kroków mają opisywać zachowanie biznesowe, np. „authorize owned order with current ETag”, nie „click button”;
- nie opakowywać każdego locatora i nie używać dekoratorów `@step` masowo;
- krótkie testy jednego stanu pozostawić bez steps;
- kroki nie mogą zawierać retry mutującej akcji.

### F-11 — P2: nieużywane artefakty metod i niepewna wartość buildera

Evidence: `IdempotencyMatrix`, `MerchantAccessMatrix`, `FilterInclusion`, `CheckoutModeOutcome` i `CreateOrderJourney` nie mają żywego wejścia ze specs; część jest osiągalna tylko przez inne nieużywane combinations. `PaymentOrderDraft` ma dwa callsites.

Zalecenie implementacyjne:

- zbudować reachability od specs/configów, nie tylko prosty search importów;
- podłączyć oracle do istniejącego testu tylko wtedy, gdy redukuje duplikację i jasno pokazuje technikę;
- pozostałe martwe artefakty usunąć wraz z wpisami README/copy map;
- `PaymentOrderDraft` zachować tylko jeśli co najmniej kilka testów wykorzystuje jego warianty/defaults; w przeciwnym razie użyć prostej factory/literału;
- nie dodawać sztucznych testów wyłącznie po to, by „zużyć” klasę.

### F-12 — P2: locator strategy wymaga jakościowej, nie mechanicznej korekty

Evidence: dominują `getByTestId`; istnieje jeden XPath w `NotificationCenterComponent` oparty na przodku DOM. Jednocześnie repo ma solidne użycie roles i labels, a stabilne IDs są uzasadnione dla złożonych state widgets.

Zalecenie implementacyjne:

- dla interaktywnych controls preferować role + accessible name albo label;
- test IDs zachować dla złożonych kart, wykresów, tabel/states i miejsc bez stabilnej semantyki użytkowej;
- XPath zastąpić locator chain zakotwiczonym w kontrolowanym komponencie/role/test id;
- nie zamieniać 281 test IDs mechanicznie;
- przy dotknięciu locatora sprawdzić strictness, unikalność i accessible contract.

### F-13 — P2: BFF coverage nie dokumentuje pełnej odpowiedzialności za nagłówki

Evidence: w `tests-pom` nie ma referencji do `X-Correlation-ID` ani `Accept-Patch`; istnieją dobre testy ETag/If-Match/Idempotency, ale nie ma jawnego inventory, które headers są odpowiedzialnością BFF.

Zalecenie implementacyjne:

- sporządzić route-to-contract matrix dla Nuxt BFF: status, body, `Content-Type`, `ETag`, `Vary`, `Cache-Control`, `Idempotency-Replayed`, `X-Correlation-ID`, `Accept-Patch` tylko tam, gdzie obecny kod/spec je implementuje;
- dodać Playwright REST tests wyłącznie dla zachowania BFF: forward/preserve/transform/mask;
- nie dublować backend REST Assured matrix bez dodatkowego BFF ryzyka;
- jeśli header nie należy do bieżącego kontraktu, oznaczyć `NOT_APPLICABLE` z dowodem zamiast rozszerzać produkt.

### F-14 — P2: konfiguracja jest poprawna discovery-wise, lecz wymaga utrzymywalnej bramki

Evidence: wszystkie 72 specs są osiągalne, ale główne `testMatch` regexy i wiele overlays mogą z czasem stworzyć zero-test project lub osierocić nowy plik.

Zalecenie implementacyjne:

- utrzymywać jawną macierz komend `--list` dla main/visual/TLS/RLS/Mirror/learner;
- walidować oczekiwaną minimalną liczbę przypadków per specjalny config bez zamrażania globalnej liczby 332;
- `forbidOnly` zachować;
- worker cap 4 i serial projects zachować;
- learner copies pozostają poza regression gate;
- jeśli powstanie shared config helper, musi upraszczać warunki i zachować czytelność różnic, inaczej go nie tworzyć.

## Strengths to Preserve

- Live stack zamiast network mocks; brak `page.route`/`route.fulfill` w suite.
- Real Keycloak storage state z rozdzielonym session project.
- `App` facade, cienki `BasePage`, page/component composition i fixtures DI.
- Unikalne dane oraz cztery worker worlds, cap workerów i serializacja testów konfliktowych.
- Wyraźne rozdzielenie Playwright E2E, Playwright REST/BFF, Vitest i backend REST Assured.
- `tests-pom/.auth` jest ignorowane przez Git.
- Brak `waitForTimeout`, `test.only` i `as any` w snapshotcie.
- Wszystkie istniejące specs są objęte konfiguracjami; specjalne flag-off/TLS/visual ścieżki mają własne projekty.
- Stabilne test IDs są używane świadomie dla lab widgets; nie należy ich usuwać dla stylistycznej czystości.

## Implementation Decisions

1. Zakres produkcyjny Vue/Nitro/Spring pozostaje zamknięty. Jeśli test ujawni produkt bug, zapisać go osobno; nie rozszerzać tego programu bez zgody.
2. Dozwolone zmiany: live POM, Playwright configs, frontend package scripts i dokumentacja/evidence dotyczące programu.
3. Priorytet: poprawność kompilacyjna → runtime behavior → determinism/diagnostics → utrzymywalność.
4. Refaktory szerokie wykonujemy expand → migrate in batches → contract. Nie przełączamy wszystkich callsites jednym ślepym rewrite.
5. `BffClient` pozostaje app-as-API facade. Ekstrakcja domenowa jest opcjonalna i musi wykazać net-minus; obowiązkowe są bezpieczne contracts i parsing.
6. POM może zawierać readiness/state assertions (`expectLoaded`, widoczność kontrolki), ale business outcome/status/header/persistence pozostaje w spec.
7. `test.step` służy raportowaniu journey, nie dekorowaniu każdej metody POM.
8. Stateful live suite: zero retries i trace retained on failure.
9. Hasła tylko z env; żadnych wartości w kodzie, logach, evidence ani Git.
10. Nie aktualizować wersji Playwright/TypeScript/Nuxt i nie dodawać dependencies.
11. Nie edytować learner copies `My*`/`Lesson*`, `.kiro/**`, backendu ani produkcyjnego frontendu.
12. Nie commitować i nie pushować bez osobnego polecenia użytkownika.

## Testing Decisions

### Static seam

- `corepack pnpm typecheck:pom` — obowiązkowy strict gate całego frameworka.
- POM-only oxlint — zero errors; warnings muszą zostać zredukowane w dotkniętych plikach i nie mogą rosnąć globalnie.
- `git diff --check` — zero whitespace errors.
- statyczne searches: zakaz `page.route`, `route.fulfill`, `waitForTimeout`, `test.only`, `as any`; wyjątki wymagają udokumentowanej decyzji.

### Discovery seam

Uruchomić `playwright test --list` dla głównego configu i każdego aktywnego overlay. Sprawdzić nie tylko exit code, ale obecność wszystkich wymaganych projektów/specs. Learner config może legalnie mieć zero testów tylko wtedy, gdy dokumentacja nadal deklaruje pustą kopię ucznia.

### Playwright REST / BFF seam

Udowadnia BFF-specific forwarding, status/body/header shape, auth przez storage state i brak token leak. Nie duplikuje backend matrix bez osobnej odpowiedzialności proxy.

### Playwright E2E seam

Udowadnia user-visible journey, accessible locators, loading/empty/error/forbidden/success states i pojedynczą mutację. Preconditions mogą korzystać z `BffClient`; finalny UI oracle nie może być zastąpiony bezpośrednim DB/API-only checkiem.

### Live validation policy

- Targeted live test po każdym ticketcie, jeśli ma wymagany stack i credentials.
- Brak credentials/stack = `NOT_RUN`, nigdy PASS.
- Nie wypisywać wartości env. Preflight sprawdza wyłącznie obecność nazw.
- Event Lab live tests wymagają stacku Kafka.
- Po wszystkich ticketach jeden pełny, kontrolowany live POM run; wynik zawiera passed/failed/flaky/skipped i czas.

### Review seam

Po każdym ticketcie scoped `code-review`; po całości `code-review` + `playwright-sdet-review` + `rest-api-test-design` + `ponytail-review`. Wszystkie P0/P1 muszą być zamknięte. P2 może zostać odroczone tylko z uzasadnieniem, właścicielem i osobnym ticketem — nie przez przemilczenie.

## Acceptance Criteria

- [ ] `typecheck:pom` obejmuje wszystkie live specs, auth, fixtures, pages/components, API/utils/data/methods i aktywne configi; jest zielony.
- [ ] Nie ma nieistniejących calls `caption()`/`runExpirationSweep()`; dwa dotknięte tests są zielone live.
- [ ] Typy `Playwright`/`WorkerInfo` są poprawne; fixture setup/teardown przechodzi typecheck i targeted run.
- [ ] `BffClient` nie wymaga `body!`/`as any` do standardowych success/error flows, a high-risk contracts są zawężane po statusie.
- [ ] Żadna mutująca action nie znajduje się w callbacku polling/retry; PSP approve emituje jeden request.
- [ ] Wszystkie aktywne configi mają spójne `retries: 0` i failure trace, a discovery matrix jest zielona.
- [ ] Wszystkie hasła, także worker manager, są wymagane z env; storage state pozostaje ignored.
- [ ] Powtarzalne direct actions w specs są przeniesione do intent POM, a business assertions pozostają czytelne w specs.
- [ ] Długie journey mają 2–5 znaczących `test.step`; krótkie tests nie są sztucznie opakowane.
- [ ] Event Lab hotspot ma poprawne typy, brak unused warnings w dotkniętym zakresie, bezpieczne helpers i targeted live Kafka evidence.
- [ ] Martwe method artifacts są podłączone z realną wartością lub usunięte; README odpowiada kodowi.
- [ ] XPath w Notification Center jest zastąpiony stabilnym locator chain; locator changes mają targeted evidence.
- [ ] BFF header matrix rozstrzyga `X-Correlation-ID`/`Accept-Patch` jako VERIFIED albo NOT_APPLICABLE bez scope creep.
- [ ] POM-only lint ma zero errors i nie więcej warnings niż baseline; dotknięte hotspoty mają net reduction.
- [ ] Główny i każdy specjalny config przechodzą `--list`; żadna live spec nie jest osierocona.
- [ ] Targeted tests dla każdego ticketu oraz finalny pełny live run są świeżo zielone; `--list` nie jest użyty jako runtime proof.
- [ ] Finalne reviews nie mają otwartych P0/P1, `git diff --check` jest zielony i evidence zawiera komendy oraz wyniki bez sekretów.

## Sequencing

| Ticket | Cel | Blocked by |
|---|---|---|
| 01 | Strict compiler gate i krytyczne kontrakty | None |
| 02 | Typowane odpowiedzi BFF i fixture contracts | 01 |
| 03 | Bezpieczne actions i granice POM/oracles | 02 |
| 04 | Config diagnostics, discovery i auth secrets | 01, 03 |
| 05 | Event Lab WIP hardening | 02, 03, 04 |
| 06 | Business steps i locator hygiene | 03, 05 |
| 07 | Method artifacts i BFF contract matrix | 02, 06 |
| 08 | Final validation, independent review i evidence | 01–07 |

## Risks and Controls

| Ryzyko | Kontrola |
|---|---|
| Strict gate otwiera dużą liczbę błędów | Naprawiać kategoriami, zaczynać od root-cause types; zakaz suppressions |
| Refactor `BffClient` powoduje masowy diff | Preferować małe helpers/types; ekstrakcja tylko expand/migrate/contract |
| Live retries duplikują stan | `retries: 0`; unique data; jedna mutacja na próbę |
| Dirty Event Lab zostanie nadpisany | Baseline diff przed edycją; zachować cudze zmiany; targeted patch |
| Full lint pozostaje czerwony poza zakresem | Raportować osobno; POM-only gate musi być zielony; nie naprawiać produkcji bez zgody |
| Brak credentials/stack | Kontynuować static/discovery; live oznaczyć NOT_RUN i nie deklarować DONE |
| Nadmierna liczba `test.step`/POM methods | 2–5 business steps tylko dla długich journey; ponytail review |
| Dublowanie backend tests w Playwright REST | Testować wyłącznie BFF responsibility matrix |

## Out of Scope

- Zmiany zachowania produkcyjnego Vue/Nitro/Spring, nowe endpointy lub role.
- Kafka poza `eventlab`, produktowy Kafka dashboard, Schema Registry/Streams/EOS.
- PSP integration, settlement, payout, KYC, PCI/3DS lub nowe payment capabilities.
- Aktualizacje wersji/dependencies.
- Learner copies `My*`/`Lesson*`.
- Mocked Playwright suite, HAR albo network interception w live POM.
- Naprawa niezwiązanego błędu produkcyjnego lint bez osobnej autoryzacji.
- Backend `restkit/` i `paymentsupport/`.

## Further Notes

Oficjalne materiały użyte jako normatywne odnośniki:

- [Playwright TypeScript](https://playwright.dev/docs/test-typescript)
- [Playwright best practices](https://playwright.dev/docs/best-practices)
- [Playwright Page Object Models](https://playwright.dev/docs/pom)
- [Playwright retries](https://playwright.dev/docs/test-retries)
- [Playwright Trace Viewer](https://playwright.dev/docs/trace-viewer)
- [Playwright `test.step`](https://playwright.dev/docs/api/class-test#test-step)
- [Playwright API testing](https://playwright.dev/docs/api-testing)

Prompt wykonawczy i trwały stopping contract: `.codex/goals/playwright-tests-pom-quality-hardening.md`.
