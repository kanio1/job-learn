# Playwright 1.61 — wzorcowa architektura POM dla tego repozytorium

## Question

Które oficjalne zalecenia Playwright 1.61 powinny wyznaczać kolejną rundę
usprawnień `apps/frontend/tests-pom`, bez mechanicznego kopiowania ogólnych
wzorców do lokalnego frameworka?

## Current Answer

Repozytorium jest przypięte do `@playwright/test` `1.61.0` i TypeScript `6.0.3`.
Obecny kierunek jest zgodny z Playwright: POM-y kapsułkują lokatory i operacje,
fixture dostarczają zależności, konteksty izolują aktorów, storage state nie jest
śledzony przez Git, a API służy do preconditions i postconditions.

Kolejna runda powinna skoncentrować się na lokalnych lukach, nie na wymianie
frameworka:

1. utrzymać lokatory oparte o role/label/test-id oraz web-first assertions;
2. przenieść własność dodatkowych kontekstów i klientów do fixture, aby teardown
   był automatyczny;
3. zachować POM + Component Object + `App` facade, ale rozdzielić duży adapter
   BFF na transport i klientów domenowych;
4. zastąpić niesoundne rzutowanie wyniku HTTP dyskryminowaną unią sukces/błąd;
5. wykorzystać dostępne w 1.61 `page.localStorage` i `page.sessionStorage` tam,
   gdzie dziś `page.evaluate` jedynie czyta lub czyści Web Storage;
6. zachować `test.step` na poziomie scenariusza; nie wprowadzać automatycznych
   dekoratorów kroków do wszystkich metod POM.

## Why It Matters

Playwright rekomenduje testowanie zachowania widocznego dla użytkownika,
izolację testów, odporne locatory i auto-retry assertions. Oficjalny przykład POM
pokazuje wyższy poziom API nad stroną, a dokumentacja fixture opisuje zależności
dostarczające testowi dokładnie potrzebne zasoby i ich teardown. Dokumentacja
uwierzytelniania dodatkowo pokazuje fixture POM dla wielu ról oraz ostrzega, że
storage state jest sekretem.

Wersja 1.61 wnosi natywne API Web Storage. W tym repo część z 15 użyć
`page.evaluate` dotyczy wyłącznie local/session storage, więc można usunąć kod
wykonywany w stronie bez tworzenia nowej abstrakcji.

## Project Impact

- Nie zmieniać `BasePage` w gruby framework ani nie wdrażać Screenplay.
- Zachować `App` jako composition root/facade dla jednej `Page`.
- Dodać resource-owning Actor Factory jako fixture dla dodatkowych ról i
  kontekstów.
- Zachować `BffClient` jako publiczny punkt fixture, lecz skomponować go z małego
  transportu i klientów domenowych.
- Walidować odpowiedzi zewnętrzne schematami Zod i wyprowadzać typy przez
  `z.infer`, zamiast utrzymywać równoległe DTO i schematy.
- Używać `as const satisfies` dla tabel decyzyjnych i maszyn stanów, gdy literalne
  wartości są częścią modelu testowego.

## Test Impact

- UI/E2E: specs zachowują biznesowe oracles i web-first assertions; POM-y
  dostarczają akcje oraz locatory.
- BFF REST: wynik rozróżnia success/problem/empty bez `as T`, `!` i opcjonalnego
  `body` po spodziewanym sukcesie.
- Auth/RBAC: dodatkowe role powstają przez fixture i są zawsze zamykane w
  teardown.
- Storage/security: natywne Web Storage API 1.61 zastępuje prosty
  `page.evaluate`; oceny JWT/cookie pozostają realnymi oracle.
- Diagnostyka: trace/video/screenshot oraz celowe `test.step` pozostają; nie
  maskować flakiness retry ani sztucznymi timeoutami.

## Source Quality

Źródła są pierwszorzędne: oficjalna dokumentacja i release notes Playwright oraz
lokalny `package.json`. Zalecenia architektoniczne są wnioskami dla tego repo, a
nie cytatami z dokumentacji.

## Sources

- [Playwright Best Practices](https://playwright.dev/docs/best-practices) —
  zachowanie użytkownika, izolacja, locatory, web-first assertions i statyczne
  bramki TypeScript/lint.
- [Playwright Page Object Models](https://playwright.dev/docs/pom) — POM jako
  wyższy poziom API kapsułkujący selektory i operacje.
- [Playwright Fixtures](https://playwright.dev/docs/test-fixtures) — fixture DI,
  izolacja i automatyczny setup/teardown.
- [Playwright Authentication](https://playwright.dev/docs/auth) — setup projects,
  storage state, unikalne konta dla testów mutujących stan oraz POM fixtures dla
  wielu ról.
- [Playwright API testing](https://playwright.dev/docs/api-testing) — używanie
  `APIRequestContext` do preconditions/postconditions i jawne zwalnianie zasobów.
- [Playwright Assertions](https://playwright.dev/docs/test-assertions) —
  auto-retry assertions, `expect.poll` i znaczenie komunikatów oracle.
- [Playwright 1.61 release notes](https://playwright.dev/docs/release-notes#version-161)
  — natywne Web Storage API oraz możliwości dostępne w dokładnie przypiętej
  wersji.
- `apps/frontend/package.json` — lokalne wersje `@playwright/test` `1.61.0`,
  TypeScript `6.0.3` i Zod `4.4.3`.

## Uncertainty / Follow-up

- Oficjalne docs pokazują zarówno assertions w metodach POM, jak i w specs.
  Decyzja „biznesowe oracles w specs” jest świadomą lokalną konwencją zapisaną w
  `tests-pom/README.md` i poprzednim goalu, nie uniwersalnym wymaganiem Playwright.
- `await using` jest dostępne w Playwright 1.61, lecz natywne fixture teardown
  lepiej pasuje do współdzielonych aktorów. Nie wdrażać własnego lifecycle tylko
  po to, by użyć nowej składni.
- Minimalne zmiany dostępności/test hooks w aplikacji wymagają osobnego dowodu,
  że locator semantyczny nie istnieje. Nie są domyślnym zakresem goalu.
