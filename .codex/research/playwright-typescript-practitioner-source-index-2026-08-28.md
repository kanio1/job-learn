# Playwright + TypeScript — indeks źródeł i praktyk (2026-08-28)

## Zakres i ograniczenie praw autorskich

To jest **indeks z autorskimi streszczeniami**, a nie archiwum treści
artykułów. Nie zapisuję pełnych tekstów cudzych wpisów (ani ich kopii w
Markdown), ponieważ byłoby to nieuprawnione powielanie materiału chronionego
prawem autorskim. Każda pozycja zachowuje URL, autora i zwięzłe opracowanie,
z którego można przejść do oryginału.

Badanie wykonano przez Firecrawl: zadanie agentowe, wyszukiwanie deweloperskie
i odczyt dokumentacji. Firecrawl chwilowo zwrócił `429` dla dodatkowego
wyszukiwania; dlatego jest to najlepszy dostępny **zweryfikowany indeks**, a
nie dowód kompletności całego Internetu. W szczególności `scrolltest.com` ma
co najmniej 87 stron paginacji i pełne przejście wszystkich nie było możliwe w
aktualnym limicie. Lista dla Michała Drajny była już zgromadzona w
[`michal-drajna-linkedin-playwright-posts.md`](michal-drajna-linkedin-playwright-posts.md).

## Wnioski — praktyki o najwyższej wartości

1. **Stabilność:** wybieraj lokatory odpowiadające percepcji użytkownika
   (`getByRole`, `getByLabel`, ewentualnie kontraktowe `data-testid`), korzystaj
   z auto-waitingu i web-first assertions; nie używaj `waitForTimeout` jako
   synchronizacji.
2. **Idiomatic TypeScript:** `strict` oraz jawny, jednolity model modułów;
   funkcje asynchroniczne są `await`owane; typy fixture opisują zależności
   testu zamiast luźnego globalnego stanu lub rzutowań `as`.
3. **Custom fixtures:** przekazuj POM-y, klientów API, deterministyczne dane i
   aktorów przez `test.extend`. Zasoby mutowalne zostają scope testu, kosztowne
   i bezpiecznie współdzielone — scope workera; zawsze zwalniaj je po `use()`.
4. **`playwright.config.ts`:** deklaruj projekty, zależności setupu
   uwierzytelnienia, limity workerów, retries oraz artefakty diagnostyczne.
   Ustawienia CI i lokalne mogą się różnić, jeśli różne są warunki działania.
5. **Oczekiwanie:** najpierw web-first assertion. `expect.poll` stosuj do
   pojedynczej wartości obserwowanej z czasem (np. eventual consistency),
   `expect(...).toPass()` do retry całego bloku zależnych akcji/asercji.
   Oba mechanizmy wymagają celowego timeoutu i sensownego oracle.
6. **Konstrukcja scenariusza:** test pokazuje biznesowy przepływ i jego oracle,
   POM kapsułkuje lokatory oraz akcje. `test.step` ma opisywać istotne etapy
   scenariusza w trace/report, nie mechanicznie każdą metodę.

## Źródła oficjalne Playwright (źródło normatywne)

- [Best practices](https://playwright.dev/docs/best-practices) — user-facing
  locators, izolacja, auto-wait i web-first assertions.
- [Fixtures](https://playwright.dev/docs/test-fixtures) — typowane dependency
  injection, scope i setup/teardown custom fixtures.
- [Configuration](https://playwright.dev/docs/test-configuration) — `defineConfig`,
  projekty, timeouts, reporters i `use`.
- [Assertions](https://playwright.dev/docs/test-assertions) — retrying
  assertions, `expect.poll`, `toPass`, `expect.configure`.
- [Test structure](https://playwright.dev/docs/test-parallel) i
  [authentication](https://playwright.dev/docs/auth) — workers, izolacja,
  setup projects i `storageState`.

### `expect.poll` kontra `toPass`

| Potrzeba | Właściwy mechanizm | Granica |
| --- | --- | --- |
| Jedna wartość ma ostatecznie osiągnąć stan | `expect.poll(() => value).toBe(...)` | Funkcja powinna być krótka, obserwowalna i bez efektów ubocznych. |
| Kilka powiązanych poleceń/asercji może chwilowo nie przejść | `expect(async () => { … }).toPass({ timeout, intervals })` | Nie ukrywa defektu ani nie zastępuje deterministycznego przygotowania danych. |
| UI ma stać się używalne lub widoczne | `await expect(locator).toBe…()` | To preferowana droga; nie opakowuj jej dodatkowym pollingiem. |

Uwaga wersyjna: lokalny projekt używa `@playwright/test` **1.61.0** i
TypeScript **6.0.3** (`apps/frontend/package.json`). Domyślne `toPass` ma
timeout `0`, więc timeout musi być jawny, gdy retry jest zamierzone.

## Indeks wpisów praktyków i blogów

### Yevhen Laichenkov

- [17 Playwright Testing Mistakes You Should Avoid](https://elaichenkov.github.io/posts/17-playwright-testing-mistakes-you-should-avoid)
  (2026-02-14) — checklista przeciw flakom: odpowiedzialność pojedynczego
  testu, lokatory użytkownika i rezygnacja z timeoutów opartych na czasie.
- [TIL: Playwright step decorator for better test reporting](https://elaichenkov.github.io/posts/til-playwright-step-decorator)
  (2026-02-16) — decorator TypeScript może dodać POM-owym operacjom czytelne
  kroki i parametry w raporcie; używać selektywnie, aby nie zaszumieć trace'a.

### Vitaliy Haradkou

- [Modern TypeScript Decorators: TC39 Stage 3](https://blog-vitaliharadkous-projects.vercel.app/blog/20-typescript-decorators)
  (2026-02-20) — nowy model dekoratorów i silniejsze typowanie; nie mieszać go
  bez potrzeby ze starszym `reflect-metadata`.
- [Testcontainers boilerplate packaged](https://blog-vitaliharadkous-projects.vercel.app/blog/21-testcontainers)
  (2026-03-26) — fixture jest właścicielem startu, readiness i teardown realnej
  zależności integracyjnej.
- [Angular-aware selector engine](https://blog-vitaliharadkous-projects.vercel.app/blog/22-angular-selectors)
  (2026-04-02) — izoluj frameworkowy sposób znajdowania elementu w warstwie
  locatorów zamiast rozsiewać go po specach.
- [Type-safe SQL in Playwright tests](https://blog-vitaliharadkous-projects.vercel.app/blog/23-pw-sql)
  (2026-04-13) — typy mogą ograniczać niebezpieczne użycie SQL, ale kluczowy
  jest niezawodny cleanup połączeń przy parallelismie.
- [Reporter Slack](https://blog-vitaliharadkous-projects.vercel.app/blog/24-pw-slack)
  (2026-04-30) — reporting jako osobna odpowiedzialność, z maskowaniem sekretów.

### Currents.dev

- [Debug Playwright tests in CI](https://currents.dev/posts/how-to-debug-playwright-tests-in-ci)
  — trace na retry, screenshot/video po błędzie, filtrowane reruny i log
  `pw:api` dla rozwiązywania awarii CI.
- [Testing authentication](https://currents.dev/posts/testing-authentication-with-playwright-the-complete-guide)
  — setup project + `storageState`; dla równoległości konta per worker.
- [Speed up Playwright tests](https://currents.dev/posts/how-to-speed-up-playwright-tests)
  — świadoma liczba workerów, persistent auth i odmienne ustawienia CI/lokalne.
- [Strategies for Playwright test agents](https://currents.dev/posts/9-strategies-to-get-the-most-out-of-playwright-test-agents)
  — najpierw architektura i mały wzorcowy test, potem skalowanie generatorów.
- [Tests that survive UI refactors](https://currents.dev/posts/designing-playwright-tests-that-survive-ui-refactors)
  — kontrakt użytkownika, nie struktura DOM, jako granica abstrahowania.
- [Playwright API testing at scale](https://currents.dev/posts/playwright-api-testing)
  (2026-07) — `APIRequestContext` w fixture z jawnym disposal; bez mutowalnego
  `beforeAll` współdzielonego przez workery.

### Sajith Dilshan

- [Async/await in Playwright TypeScript](https://medium.com/@sajith-dilshan/mastering-async-await-in-playwright-typescript-1343e5b21722)
  — operacje UI/API są asynchroniczne i powinny być `await`owane, aby zachować
  kolejność oraz otrzymywać właściwe błędy.
- [Locator strategy](https://medium.com/@sajith-dilshan/playwright-locator-strategy-choosing-the-right-locator-for-stable-and-maintainable-test-automation-018a4fd0e16c)
  — role i label zamiast CSS zależnego od implementacji.
- [Auto-waiting](https://medium.com/@sajith-dilshan/playwright-auto-waiting-the-secret-behind-stable-and-reliable-test-automation-bd3987a3156e)
  — meaningful condition/assertion zamiast sztucznego sleepa.
- [Scalable authentication](https://medium.com/@sajith-dilshan/scalable-authentication-in-playwright-why-globalsetup-falls-short-644cf0fb4db4)
  — setup project jest bardziej kompozycyjny od `globalSetup`.
- [Why `tsconfig.json` matters](https://medium.com/@sajith-dilshan/why-tsconfig-json-matters-in-a-playwright-typescript-project-and-why-its-often-missing-1f8c99b598fc)
  — runner nie zastępuje jawnej kontroli `strict`, resolution, target i includes.
- [Module systems](https://medium.com/@sajith-dilshan/understanding-typescript-module-systems-for-playwright-commonjs-vs-es-modules-e2a8caffa328)
  — wyrównaj model ESM/CJS w `package.json`, `tsconfig` i importach.
- [Hooks](https://medium.com/@sajith-dilshan/playwright-hooks-the-secret-behind-clean-scalable-test-automation-8448bc56c1b4)
  — wąskie, przewidywalne przygotowanie i sprzątanie, bez ukrywania logiki testu.
- [Error handling and strict mode](https://medium.com/@sajith-dilshan/playwright-error-handling-explained-common-errors-strict-mode-and-best-practices-63fac0949ea0)
  — napraw niejednoznaczny locator, nie łap szeroko wyjątku.

### TestDino i Level Up Coding

- [POM pattern](https://testdino.com/blog/playwright-page-object-model) — POM
  udostępnia akcje/lokatory, test zachowuje scenariusz oraz assertions; fixture
  wstrzykuje gotowy POM.
- [Playwright test automation](https://testdino.com/blog/playwright-test-automation)
  — konfiguracja i układ katalogów oraz artefakty diagnostyczne dopasowane do CI.
- [Playwright 1.61 release](https://testdino.com/blog/playwright-1-61-release)
  — `expect.poll` do wartości zmieniających się w czasie; soft polling tylko gdy
  kontynuacja po failure ma wartość diagnostyczną.
- [Playwright architecture](https://testdino.com/blog/playwright-architecture)
  — test scope zapewnia świeżość, worker scope amortyzuje drogie zasoby.
- [Custom fixtures in TypeScript](https://levelup.gitconnected.com/how-to-create-custom-fixtures-in-playwright-typescript-a-complete-practical-guide-4fa8b2fc2c82)
  — typowane DI dla POM/test data i centralny lifecycle.
- [Fixtures in 2025](https://levelup.gitconnected.com/playwright-fixtures-in-2025-the-practical-guide-to-fast-clean-end-to-end-tests-55e9b3f7b5f7)
  — dobieraj scope do kosztu oraz izolacji i zawsze wykonuj teardown.

## Pokrycie nazw wskazanych w zapytaniu

| Autor / serwis | Rezultat |
| --- | --- |
| Michał Drajna | Istniejący, znacznie szerszy indeks: `michal-drajna-linkedin-playwright-posts.md`. |
| Yevhen Laichenkov (`elaichenkov.github.io`) | 2 zweryfikowane wpisy powyżej. |
| Vitaliy Haradkou | 5 zweryfikowanych wpisów powyżej. |
| Sajith Dilshan | 8 zweryfikowanych wpisów powyżej. |
| Currents.dev / TestDino / Level Up | 6 / 4 / 2 zweryfikowane wpisy powyżej. |
| Anton Gulin, Angela Zelaya, Viktor Konovalov, Stefan Minchev, Joseph Ward, Vitaliy Potapov, Artem Bondar, Butch Mayhew, Scrolltest | Nie udało się zweryfikować wpisów w obecnym limicie Firecrawl. Brak pozycji nie jest twierdzeniem, że autor nie publikuje. |

## Dopasowanie do tego repozytorium

`apps/frontend/playwright.pom.config.ts` już ma projekty setupu Keycloak,
zależności projektów, `storageState`, ograniczenie workerów, `expect.timeout`
oraz retain-on-failure dla trace/video/screenshot. `tests-pom` zawiera również
realne przykłady `expect.poll`, `toPass` i `test.step`. Najbardziej wartościowe
następne kroki to utrzymywanie spójności tych istniejących konwencji, a nie
dodawanie drugiego frameworka fixture ani globalnych hooków.

## Niepewność / dalsze badanie

- Nie deklaruj tej listy jako „wszystkie posty” wskazanych autorów: metadane
  części blogów oraz rate limit uniemożliwiły pełny crawl.
- Daty bez daty publikacji nie są zgadywane.
- Przy decyzjach o API Playwright kieruj się najpierw źródłami oficjalnymi;
  wpisy praktyków są uzupełnieniem, nie specyfikacją.
