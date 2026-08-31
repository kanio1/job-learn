---
title: "Best Practices | Playwright"
source: "https://playwright.dev/docs/best-practices"
retrieved: "2026-08-28"
---

# Best Practices | Playwright

> Uwaga dotycząca praw autorskich: zamiast reprodukować pełną treść strony
> zewnętrznej, plik zawiera oczyszczone, szczegółowe opracowanie merytoryczne
> pobrane przez Firecrawl oraz odsyłacz do oryginału.

## Testing philosophy

Testy powinny sprawdzać zachowanie widoczne dla użytkownika, a nie szczegóły
implementacji takie jak nazwy funkcji, typy struktur danych czy klasy CSS.
Każdy test powinien być niezależny: własny kontekst przeglądarki, cookies,
storage i dane. Hooki `beforeEach`/`afterEach` mogą usuwać powtarzalność, ale
niewielka duplikacja jest akceptowalna, gdy poprawia czytelność.

Nie testuj zewnętrznych usług, których nie kontrolujesz. Zależności third-party
mockuj przez Network API i zapewnij deterministyczną odpowiedź. Jeżeli test
korzysta z bazy danych, kontroluj dane w stabilnym środowisku testowym; dla
visual regression przypnij wersję systemu i przeglądarki.

## Locators

Używaj locatorów Playwright, ponieważ zapewniają auto-waiting i retry-ability.
Preferuj atrybuty user-facing oraz jawne kontrakty: role, nazwę, label i
świadomie zdefiniowany test id. Locatory można zawężać przez chaining i
filtering, np. najpierw wybrać element listy po tekście, a potem przycisk w
jego obrębie.

Unikaj selektorów opartych na strukturze DOM i klasach CSS, ponieważ zmiany
prezentacyjne łamią takie testy. XPath/CSS powinny być świadomym fallbackiem,
a nie domyślną strategią.

## Generowanie locatorów

`codegen` oraz rozszerzenie VS Code pomagają znaleźć locator i rozpocząć test.
Wygenerowany kod trzeba przejrzeć: generator przyspiesza authoring, lecz nie
zastępuje oceny stabilności selektora ani oceny scenariusza biznesowego.

## Web-first assertions

Używaj asercji Playwright z `await`, aby runner czekał na oczekiwany stan i
ponawiał sprawdzenie do timeoutu. Odczyt wartości metodą natychmiastową, a
następnie ręczne `toBe`, nie daje takiej synchronizacji. Ta sama zasada
dotyczy komunikatów, elementów pojawiających się po akcji i innych stanów
eventual-consistency.

## Debugging i artefakty

Lokalnie pomocne są VS Code, Inspector, UI Mode i tryb debug. W CI trace
viewer jest bogatszym dowodem niż sam screenshot lub video: pozwala obejrzeć
timeline, snapshot DOM i requesty sieciowe. Trace warto włączać na pierwszym
retry (`on-first-retry`), a nie dla każdego testu, aby ograniczyć koszt.

## Narzędzia i TypeScript

Playwright dostarcza rozszerzenie VS Code, codegen, trace viewer i UI Mode.
Testy mogą być pisane w TypeScript, co poprawia integrację z IDE i wykrywanie
błędnych sygnatur. W CI uruchamiaj `tsc --noEmit` oraz ESLint z regułą
`@typescript-eslint/no-floating-promises`, aby wykryć brakujące `await`.

## Projekty przeglądarek

Konfiguruj osobne projekty dla Chromium, Firefox i WebKit, a także urządzeń,
jeśli ryzyko produktu tego wymaga. Nazwa projektu powinna jasno określać
przeglądarkę lub emulowane urządzenie; konfiguracja `use` nie powinna mieszać
niepowiązanych wariantów w jednym smoke teście.

## Aktualizacje zależności

Utrzymuj Playwright i browser binaries w aktualnej, kontrolowanej wersji.
Przed aktualizacją sprawdź release notes, uruchom mały smoke gate, a następnie
pełną suite. Aktualizacja runnera może wymagać zgodności reportera,
orchestratora i konfiguracji CI.

## CI, parallelism i sharding

Uruchamiaj testy często, najlepiej na każdym commitcie i pull requeście. Na CI
instaluj tylko potrzebne browsery, używaj lockfile i zachowuj trace/report po
failure. Playwright uruchamia testy równolegle między workerami; testy w jednym
pliku są domyślnie sekwencyjne. `test.describe.configure({ mode: 'parallel' })`
oraz sharding między maszynami wymagają niezależnych danych i braku zależności
od kolejności.

## Soft assertions

`expect.soft` pozwala zebrać kilka niezależnych problemów i zgłosić je po
zakończeniu testu. Stosuj soft assertions tylko dla kontroli, po których dalsze
kroki nadal mają sens; krytyczna precondition powinna pozostać zwykłą
asercją.

## Źródło

[Oryginalny artykuł Playwright — Best Practices](https://playwright.dev/docs/best-practices)

