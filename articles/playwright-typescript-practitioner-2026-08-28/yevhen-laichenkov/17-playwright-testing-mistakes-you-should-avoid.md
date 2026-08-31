---
title: "17 Playwright Testing Mistakes You Should Avoid"
source: "https://elaichenkov.github.io/posts/17-playwright-testing-mistakes-you-should-avoid"
retrieved: "2026-08-28"
---

# 17 Playwright Testing Mistakes You Should Avoid

- Author: Yevhen Laichenkov
- Retrieval status: complete
- Firecrawl options: `formats: ["markdown"]`, `onlyMainContent: true`

## Article content

Firecrawl zwrócił główną treść artykułu (`statusCode: 200`). Poniżej znajduje
się szczegółowe opracowanie wszystkich 17 omawianych antywzorców, zachowujące
ich kolejność i sens. Pełna treść źródłowa nie jest reprodukowana słowo w słowo.

### 1. Test bez asercji

Samo przejście przez `page.goto` i akcje nie weryfikuje zachowania aplikacji.
Każdy test powinien zakończyć się asercją sprawdzającą oczekiwany rezultat.

### 2. Jednorazowe odczyty zamiast web-first assertions

Pobieranie `isVisible`, `textContent`, `count` albo URL-a i przekazywanie
wyniku do zwykłego `expect` jest zależne od chwili wykonania. Asercje takie jak
`toBeVisible`, `toHaveText`, `toHaveURL`, `toHaveCount` i `toBeEnabled`
ponawiają sprawdzenie do timeoutu.

### 3. Stałe opóźnienia `waitForTimeout`

Sleep jest zgadywaniem: może być zbyt krótki na wolnym środowisku i zbyt długi
na szybkim. Czekaj na konkretny sygnał UI lub domeny, np. widoczny heading,
gotowy przycisk albo zmianę statusu.

### 4. Traktowanie `networkidle` jako gotowości strony

Brak aktywnych połączeń przez chwilę nie oznacza, że użytkownik może wykonać
następną akcję. Long-polling, WebSockety i wolne API mogą dać zarówno zbyt
wczesny, jak i zbyt późny sygnał. Preferuj user-visible state.

### 5. Wstępne oczekiwanie przed akcją z auto-waiting

`click`, `fill`, `check` i podobne akcje już wykonują actionability checks.
Ręczne `locator.waitFor` przed każdą akcją powiela logikę i zaciemnia test;
usuń je, chyba że czekasz na odrębny, znaczący stan.

### 6. Nadużywanie `{ force: true }`

Force omija widoczność, stabilność, nakładanie elementów i inne kontrole.
Może ukryć prawdziwy błąd UI. Zamiast wymuszać kliknięcie, zamknij overlay
albo popraw sekwencję tak, aby odpowiadała działaniu użytkownika.

### 7. Nieprawidłowa kolejność `waitForResponse`

Listener dla odpowiedzi ustaw przed akcją, akcję wykonaj jako drugi krok, a
promise odpowiedzi awaituj jako trzeci. Odwrotna kolejność tworzy race condition,
w którym szybka odpowiedź może nadejść zanim listener zostanie podłączony.

### 8. Własne pętle retry

Ręczne liczniki, pętle i `waitForTimeout` dublują mechanizmy runnera. Dla
pojedynczej wartości użyj `expect.poll`, a dla kilku powiązanych akcji i
asercji — `toPass` z jawnym timeoutem oraz interwałami.

### 9. Zbyt długi timeout wewnątrz `toPass`

Jeżeli wewnętrzna asercja ma pełny, długi timeout, każda próba może blokować
ponowienie przez wiele sekund. Ustaw krótki timeout wewnętrzny, a dłuższy
timeout pozostaw zewnętrznemu `toPass`, aby retry następowało szybko.

### 10. Przestarzałe API

Autor wskazuje `waitForNavigation` i `waitForSelector` jako konstrukcje, które
często powinny zostać zastąpione `waitForURL`, locator assertions albo
web-first assertions. Przy migracji sprawdź aktualny changelog przypiętej
wersji Playwright.

### 11. Niejawne dopasowanie tekstu

Locator tekstowy bez `exact: true` może dopasować podciąg, np. „Submit” w
„Submit Order” lub „Submitting”. Jawna dokładność ogranicza niespodziewane
matchowanie i naruszenia strict mode po zmianie UI.

### 12. `expect.poll` do prostego DOM

`expect.poll` jest przeznaczone do wartości obserwowanych w czasie i własnych
funkcji odczytu. Jeżeli sprawdzasz zwykły tekst, widoczność albo liczbę
elementów, krótsze i bardziej idiomatyczne jest bezpośrednie web-first
assertion na locatorze.

### 13. `waitForFunction` do prostego UI

Funkcja w kontekście strony ma sens dla rzeczywiście niestandardowego warunku.
Nie używaj jej do prostego tekstu lub widoczności, które można wyrazić
locator assertion; rozwiązanie będzie czytelniejsze i lepiej raportowane.

### 14. Negacja zamiast pozytywnej asercji

Jeżeli istnieje semantyczna asercja pozytywna, np. `toBeHidden`, jest ona
czytelniejsza niż `not.toBeVisible`. Jasny opis oczekiwanego stanu ułatwia
diagnostykę i ogranicza dwuznaczność.

### 15. Brak `eslint-plugin-playwright`

Dedykowany plugin ESLint może automatycznie wykrywać część opisanych
antywzorców. Włączenie rekomendowanej konfiguracji dla katalogu testów
przenosi część jakości z review do powtarzalnego lintowania CI.

### 16. Zwracanie nowego POM z każdej akcji

Akcja POM nie musi tworzyć i zwracać kolejnego page objectu po każdym kliknięciu.
Taki styl zwiększa sprzężenie i utrudnia ponowne użycie. Niech test zdecyduje,
który obiekt strony utworzyć po przejściu do oczekiwanego stanu.

### 17. Zależność testów przez `test.describe.serial`

Serial łączy testy w kruchy łańcuch: awaria pierwszego kroku pomija kolejne i
ukrywa ich niezależny stan. Przy wieloetapowym przepływie użyj jednego testu
z `test.step`; osobne testy powinny przygotowywać własne dane i działać w
dowolnej kolejności.

### Konkluzja autora

Stabilność wynika z prostych testów: asercji zachowania użytkownika, zaufania
do wbudowanego oczekiwania i unikania sztucznych hacków czasowych. Te zasady
mają jednocześnie skrócić suite, ograniczyć flakiness i ułatwić utrzymanie.

[Canonical source](https://elaichenkov.github.io/posts/17-playwright-testing-mistakes-you-should-avoid)
