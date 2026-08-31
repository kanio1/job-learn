# Playwright + TypeScript — piąta iteracja: pozostałe luki (2026-08-28)

## Zakres i ograniczenie

Ta iteracja użyła **MCP Exa Search** (live crawl) i porównała wyniki z
czterema wcześniejszymi indeksami. Poniżej zapisuję nowe, zweryfikowane URL-e
oraz autorskie streszczenia. Nie zapisuję pełnej treści cudzych artykułów:
byłby to niedozwolony przedruk materiałów chronionych prawem autorskim.

## Nowe materiały

### Vitaliy Potapov

- [Authentication in Playwright: You Might Not Need Project Dependencies](https://vitalets.github.io/posts/playwright/authentication-without-project-dependencies/)
  (24 października 2025) — leniwe, role-aware tworzenie i cache `storageState`
  może ograniczyć koszt setupu przy suite obejmującym wiele ról. Klucz cache
  powinien uwzględniać rolę, środowisko i — przy danych mutowalnych — worker.
  To alternatywa dla setup projects, nie powód do usunięcia jawnych projektów
  Keycloak z tego repo; wymaga też dodatkowego `@global-cache/playwright`.

### Vitali Haradkou

- [Graceful test cancellation with `AbortSignal`](https://dev.to/vitalicset/stop-leaking-resources-how-to-use-abortsignal-in-playwright-tests-jb2)
  (16 lutego 2026) — anuluj własne długie operacje asynchroniczne po timeoutach;
  nie zastępuje to web-first assertions.
- [Real containers via a Playwright fixture](https://vitalicset.hashnode.dev/playwright-labs-testcontainers)
  (26 marca 2026) — test-scoped fixture może zarządzać prawdziwą usługą z
  automatycznym cleanupem. Nie uzasadnia to dodania zależności frontendowej do
  obecnego projektu bez osobnej decyzji.
- [Playwright Labs: Best Practices as Code](https://dev.to/vitalicset/introducing-playwright-labs-best-practices-as-code-198n)
  (20 stycznia 2026) — repozytorium zasad jest materiałem referencyjnym, nie
  normatywną dokumentacją API.
- [Email-safe Playwright report](https://vitalicset.hashnode.dev/playwright-email-react)
  (30 marca 2026) — reporter e-mail powinien być osobnym adapterem; nie jest
  potrzebny przy lokalnym `list` reporterze bez konkretnego wymagania SMTP.

### TestDino

- [Playwright 1.62: isolated retries, AbortSignal and more](https://testdino.com/blog/playwright-1-62-release)
  (30 lipca 2026, Pratik Patel) — opisuje isolated retries, `AbortSignal`,
  component testing i WebP screenshots. Projekt używa `@playwright/test`
  **1.61.0**, więc API 1.62 wymaga osobnego canary i nie powinno być kopiowane
  do obecnych testów.

### Viktor Konovalov

- [Validate the accessibility tree, not the DOM](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7472539768522940418-yMME)
  (16 czerwca 2026) — publiczny autorski tip, aby oracle opierać na kontrakcie
  dostępności, a nie przypadkowej strukturze DOM. Zgodne z user-facing
  locators i ARIA snapshots w `tests-pom`.

- [Playwright tip: stop waiting blindly — use `expect.poll()`](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-tip-stop-waiting-blindly-use-activity-7449021903387844608-Oes9)
  (12 kwietnia 2026) — dla stanu backendu, kolejki, joba lub płatności,
  którego Playwright nie obserwuje przez lifecycle UI, odczytuj status w
  `expect.poll` z jawnym timeoutem i interwałami; nie dodawaj stałego snu.

- [Playwright tip: use `expect.toPass()` when a single assertion retry is not enough](https://www.linkedin.com/posts/viktorkonovalovqa_playwright-typescript-qa-activity-7468564794011246592-LFyP)
  (5 czerwca 2026) — gdy trzeba ponowić całą sekwencję trigger → wait →
  verify, opakuj tylko ten blok w `toPass`; nie włączaj szerokich retry, które
  maskują regresje.

### Stefan Minchev

- [Stop writing while loops to wait for a database update](https://www.linkedin.com/posts/stefan-minchev-qa_qa-typescript-ai-activity-7460346882448392192-yn6y)
  (13 maja 2026) — `expect.poll` zastępuje ręczne pętle `while`,
  `setTimeout` i przypadkowe backoffy, jeśli warunek jest bezefektowym
  odczytem eventual state. Wpis jest publicznym postem LinkedIn; Exa nie
  znalazł osobnego, kompletnego archiwum blogowego autora.

### Currents.dev

- [Playwright testing in Staging vs Production](https://currents.dev/posts/playwright-testing-staging-vs-production)
  (15 maja 2026, Currents Team) — `baseURL` powinien być walidowany przy
  ładowaniu konfiguracji; nie współdziel `storageState` między środowiskami.
  Użyj projektów/tagów do jawnego rozdzielenia produkcji i stagingu oraz
  izoluj konta i dane per worker. To uzupełnia wcześniejsze wpisy Currents o
  auth, CI, agentach i testach API.

### ScrollTest / Pramod Dutta

- [Playwright Actions and Auto-Waiting: Day 3](https://scrolltest.com/playwright-actions-auto-waiting-day-3/)
  (11 czerwca 2026) — korzystaj z actionability checks i wbudowanego
  auto-waitingu; po akcji czekaj na obserwowalny sygnał nawigacji/stanu zamiast
  `waitForTimeout`. URL jest nowy względem wcześniejszej pozycji o
  asercjach z „Day 3”.

### Butch Mayhew / Playwright Solutions

- [Playwright-cli Boosts Token Efficiency for Coding Agents](https://www.linkedin.com/posts/butchmayhew_playwright-cli-just-changed-how-i-work-with-activity-7426626393586884608-BYI4)
  (9 lutego 2026) — autor porównuje CLI z MCP pod kątem kosztu tokenów i
  wskazuje progresywne drzewa dostępności jako praktyczny interfejs do
  diagnozowania oraz authoringu testów przez agenta. To post LinkedIn, nie
  artykuł z archiwum Playwright Solutions; nie należy mieszać go z tekstami
  T.J. Mahera, które Exa również zwraca jako materiały powiązane.

## Wyniki negatywne i korekty

- Angela Zelaya: Exa znalazł reshary, ale nie samodzielny, publiczny artykuł
  techniczny; nie przypisuję jej cudzych treści.
- Stefan Minchev: Exa znalazł powyższy publiczny, autorski tip LinkedIn o
  `expect.poll`; nie znaleziono osobnego, kompletnego archiwum blogowego.
- Yevhen Laichenkov: tag Playwright nadal wskazuje dwa wcześniej opisane wpisy.
- Michal Drajna: znalezione posty o database rollback i auth są odsyłaczami do
  materiałów innych autorów, nie nowymi autorskimi artykułami.
- Archiwum Butcha Mayhewa pozostaje dynamiczne i częściowo paginowane; wcześniejsze
  listy są indeksem relewantnych tekstów, nie dowodem kompletności wszystkich
  publikacji. Nowy post LinkedIn o Playwright CLI zapisano osobno, z wyraźnym
  oznaczeniem typu źródła.

## Recheck i deduplikacja piątej iteracji

- [Playwright TypeScript Checklist](https://scrolltest.com/playwright-typescript-checklist/)
  pojawił się ponownie w Exa, ale jest już w indeksie drugiej iteracji — nie
  dodaję duplikatu.
- [Joseph Ward — blog index](https://josephward.tech/blog/) nadal pokazuje
  trzy relewantne wpisy zapisane w iteracji drugiej; nie znaleziono nowego
  artykułu Playwright/TypeScript.
- Zapytania o Butcha zwróciły również tutoriale T.J. Mahera i repozytorium
  przykładowe oparte na jego kursie. Nie są one publikacjami Butcha, więc nie
  zostały przypisane temu autorowi.
- Dla Angeli Zelayi i Michala Drajny nie znaleziono nowej, jednoznacznie
  autorskiej publikacji technicznej poza pozycjami już zapisanymi; znalezione
  reshare'y pozostają oznaczone jako takie albo pominięte.

## Mapa pokrycia wskazanych autorów i stron

Poniższa mapa rozdziela nowe pozycje od materiałów już zebranych. „Brak nowej”
oznacza wynik rechecku Exa, a nie twierdzenie, że autor przestał publikować.

| Autor/strona | Stan po piątej iteracji | Indeks źródłowy |
|---|---|---|
| Anton Gulin | wpisy o POM, retry, evidence i passkeys zebrane wcześniej; brak nowego URL-u w tym rechecku | [iteracja 4](playwright-typescript-practitioner-source-index-iteration-4-2026-08-28.md) |
| Michal Drajna | osobny katalog publicznych artykułów i postów LinkedIn; brak nowej jednoznacznie autorskiej pozycji w tej rundzie | [katalog Drajny](michal-drajna-linkedin-playwright-posts.md) |
| Angela Zelaya | brak zweryfikowanego, samodzielnego artykułu Playwright/TypeScript | ta iteracja, „Wyniki negatywne” |
| Viktor Konovalov | trzy autorskie tipy LinkedIn, w tym dwa nowe o `poll`/`toPass` | ta iteracja |
| Stefan Minchev | jeden nowy autorski tip LinkedIn o `expect.poll`; brak blogowego archiwum | ta iteracja |
| ScrollTest | seria 21 dni i dodatkowe wpisy; nowy wpis o actions/auto-waiting | [iteracja 2](playwright-typescript-practitioner-source-index-iteration-2-2026-08-28.md), [iteracja 4](playwright-typescript-practitioner-source-index-iteration-4-2026-08-28.md) |
| Joseph Ward | blog index nadal pokazuje trzy relewantne wpisy; brak nowego | [iteracja 2](playwright-typescript-practitioner-source-index-iteration-2-2026-08-28.md) |
| Vitaliy Potapov | seria „Playwright in Pictures” oraz nowy wpis o auth cache | [iteracja 3](playwright-typescript-practitioner-source-index-iteration-3-2026-08-28.md), ta iteracja |
| Artem Bondar | Bondar Academy zmapowana w iteracji 3; brak nowego URL-u w rechecku | [iteracja 3](playwright-typescript-practitioner-source-index-iteration-3-2026-08-28.md) |
| Vitali Haradkou | pięć wcześniejszych wpisów + cztery nowe materiały o cancellation, fixture, best-practices i reporterze | ta iteracja |
| Currents.dev | wcześniejsze wpisy + nowy przewodnik staging/production | ta iteracja |
| Yevhen Laichenkov | dwa wpisy i repozytorium `playwright-expect`; brak nowego wpisu | [iteracja 1](playwright-typescript-practitioner-source-index-2026-08-28.md), [iteracja 4](playwright-typescript-practitioner-source-index-iteration-4-2026-08-28.md) |
| TestDino | wcześniejsze poradniki + release 1.62 | ta iteracja |
| Level Up Coding | dwa artykuły o fixture zebrane wcześniej; brak nowego | [iteracja 1](playwright-typescript-practitioner-source-index-2026-08-28.md) |
| Sajith Dilshan / Medium | dziesięć artykułów o TS/Playwright, fixture i annotations; brak nowego | [iteracja 1](playwright-typescript-practitioner-source-index-2026-08-28.md), [iteracja 4](playwright-typescript-practitioner-source-index-iteration-4-2026-08-28.md) |
| Butch Mayhew | pozycje Playwright Solutions + nowy post LinkedIn o CLI; katalog pozostaje dynamiczny | [iteracja 2](playwright-typescript-practitioner-source-index-iteration-2-2026-08-28.md), [iteracja 3](playwright-typescript-practitioner-source-index-iteration-3-2026-08-28.md), [iteracja 4](playwright-typescript-practitioner-source-index-iteration-4-2026-08-28.md), ta iteracja |

## Metoda i ograniczenia

- Użyto MCP **Exa Search** (live crawl), a następnie MCP **Exa Fetch** do
  odczytu nowych stron Currents, ScrollTest, LinkedIn i indeksu Josepha Warda.
  Autorstwo i daty porównano z indeksami z iteracji 1–4.
- Exa nie jest kompletnym archiwum autora. Wynik „brak nowej” oznacza tylko,
  że w publicznie dostępnych wynikach rechecku nie znaleziono kolejnej
  jednoznacznie autorskiej pozycji.
- Linki LinkedIn są oznaczone jako posty, a nie artykuły blogowe. Nie zaliczam
  tutoriali T.J. Mahera ani repostów do dorobku Butcha, Michala lub innych
  autorów bez jednoznacznego byline.
- Zachowuję URL-e i własne streszczenia zamiast pełnego przedruku artykułów;
  pełna treść cudzych publikacji jest chroniona prawem autorskim.

## Synteza praktyk dla tego repo

1. Fixture jest kontraktem lifecycle: test scope dla `page` i danych
   mutowalnych, worker scope tylko dla bezpiecznie współdzielanych zasobów;
   setup kończy się przed `use()`.
2. `playwright.config.ts` koduje politykę wykonania: projekty, zależności,
   auth state, workery, retries i artefakty.
3. Preferuj locator assertions. `expect.poll` służy krótkiemu, bezefektowemu
   odczytowi eventual state, a `toPass` celowemu retry powiązanego bloku z
   jawnym timeoutem.
4. `test.step` opisuje biznesowe etapy, nie każde mechaniczne wywołanie POM.
5. `strict`, `import type`, typy fixture i małe argumenty POM są idiomatycznym
   TypeScript; `any`, globalny stan i wymuszone casty ukrywają błędy.

## Źródła normatywne

- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Playwright Fixtures](https://playwright.dev/docs/test-fixtures)
- [Playwright Assertions](https://playwright.dev/docs/test-assertions)
- [Playwright Configuration](https://playwright.dev/docs/test-configuration)

W razie konfliktu bloga z dokumentacją lub lokalną wersją 1.61.0 pierwszeństwo
ma dokumentacja oficjalna.
