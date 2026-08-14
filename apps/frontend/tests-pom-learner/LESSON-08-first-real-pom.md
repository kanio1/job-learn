# Lekcja 8/75 — pierwszy realny POM: wiele kont, ról i tenantów

Wariant mentorski. TypeScript i Playwright od podstaw na prawdziwym stosie. POM budujesz sam.

Dla kogo: kilka lat QA, pierwszy poważny tydzień z TypeScript + Playwright. Nie tłumaczę, czym jest test negatywny. Tłumaczę, jak ten sam osąd QA zapisać w TS i w przeglądarce.

Gałąź: `checkout-protocol-lab-foundation` (commit `9c15a24`).
Timebox: około 90 minut. Mięso to Kroki 4–6. Reszta to brama, żeby test w ogóle miał z czym rozmawiać.

---

## 0. Jak czytać tę lekcję

Każdy krok ma trzy warstwy:

1. **Po co** — jaka metoda testowania i dlaczego nie inna.
2. **Co to jest w TS/Playwright** — minimalny język, bez którego nie napiszesz kodu.
3. **Zrób** — plik, TODO, komenda.

Nie kopiuj gotowych speców ani `MerchantsListPage.ts`. To byłoby odpisanie pracy domowej. Kopiujesz tylko logowanie OIDC — to infrastruktura, nie POM.

Po każdym STOP odpowiedz sobie jednym zdaniem, zanim pójdziesz dalej.

---



## 1. Cel

Po tej lekcji umiesz:

- odróżnić **RBAC** (czy wolno wejść do funkcji) od **zakresu tenanta** (jakie dane wracają, gdy już wolno);
- zapisać to jako trzy niezależne testy E2E, bez mocków;
- zbudować od zera jedną klasę Page Object w TypeScript;
- uruchomić tę samą klasę jako trzy różne konta, każde w osobnym `BrowserContext`;
- trzymać locatory w POM, a oczekiwania biznesowe w teście;
- wybrać asercję, która mówi to, co chcesz udowodnić (`toBeVisible` vs `toHaveCount(0)`).

Jedno ćwiczenie, jedna architektura:

```text
storageState konta  →  nowy BrowserContext  →  MyMerchantRegistryPage  →  Nuxt → BFF → Spring → Postgres
```

Klasa jest ta sama. Cookies, rola i tenant są inne.

---



## 2. Problem testowy — zanim pojawi się kod

Masz Merchant Registry (`/admin/merchants`). Pytanie nie brzmi „czy strona się otwiera”. Pytanie brzmi:

**Dla tego aktora, po przejściu przez prawdziwe logowanie, jaki zbiór merchantów i jaka kontrolka są legalne?**

To jest klasyczna **tablica decyzyjna** (ISTQB: decision table), nie „happy path”.


| Konto              | Composite role     | Tenant z tokena   | `merchants:read` | Oczekiwanie                                |
| ------------------ | ------------------ | ----------------- | ---------------- | ------------------------------------------ |
| `platform.admin`   | `PLATFORM_ADMIN`   | `PLATFORM_TENANT` | tak              | widzi dane przekrojowo, m.in. Alpha i Betę |
| `tenant.admin`     | `TENANT_ADMIN`     | `TENANT_ALPHA`    | tak              | widzi tylko merchantów Alpha               |
| `merchant.manager` | `MERCHANT_MANAGER` | `TENANT_ALPHA`    | nie              | komunikat o braku dostępu; bez tabeli      |


Dlaczego te trzy, a nie wszystkie pięć ról? **Partycje równoważności.** `SUPPORT_AGENT` i `READ_ONLY_USER` też mają `merchants:read` — dla *tej* strony wpadają do tej samej klasy co tenant/platform pod względem „czy lista w ogóle wraca”. Nie uczą Cię dziś nowego rozgałęzienia. Trzecie konto musi być tym, które **odpada wcześniej**.

```text
Zalogowane konto
        │
        ▼
  Ma merchants:read?  ──nie──►  403 / alert: test RBAC
        │ tak
        ▼
  Jaki tenant w tokenie?  ──►  dane tylko z tego zakresu: test tenanta
```

To rozróżnienie jest mięsem seniorskim. Tester, który na 403 managera napisze „tenant isolation działa”, myli warstwę. Manager **nie doszedł** do filtra tenanta. Żeby udowodnić izolację, potrzebujesz konta, które *ma* read i mimo to nie widzi obcego rekordu. Stąd `tenant.admin`, nie drugi manager.

### Oracle danych — nie zgaduj po nazwie


| Merchant             | Faktyczny tenant w seedzie |
| -------------------- | -------------------------- |
| `MERCHANT_ALPHA_001` | `TENANT_ALPHA`             |
| `MERCHANT_ALPHA_002` | `TENANT_ALPHA`             |
| `MERCHANT_BETA_001`  | `PLATFORM_TENANT`          |


`MERCHANT_BETA_001` nie oznacza, że istnieje `TENANT_BETA`. W tym seedzie Beta należy do platformy. W QA zawsze sprawdzasz kontrakt danych. Nazwa to hipoteza, nie dowód.

Dlaczego seed, a nie tworzenie merchantów w teście? Dziś **nie mutujesz** rejestru. Tworzysz tylko obserwację. Jeśli każdy test dopisuje rekordy, przestajesz wiedzieć, czy izolacja działa, czy patrzysz na śmieci po poprzednim runie. Deterministyczny seed = wspólny, znany oracle.

---



## 3. Dlaczego ta metoda, a nie inna

Znasz te dylematy z API i z manuala. Tu wybierasz narzędzie pod *to* ryzyko.


| Metoda                                                 | Co udowadnia                            | Dlaczego dziś tak / nie                                                                          |
| ------------------------------------------------------ | --------------------------------------- | ------------------------------------------------------------------------------------------------ |
| **Live E2E** (Keycloak → UI → BFF → Spring → Postgres) | prawdziwa sesja + prawdziwe filtrowanie | Izolacja tenanta jest właściwością backendu. Mock listy udowodniłby Twój stub.                   |
| `page.route()` **/ mock sesji**                        | UI przy podstawionym JSON               | Świetne na stany loading/empty. Tu kłamią o tenant scope. Zakazane w tej lekcji.                 |
| **Sam test API (REST Assured)**                        | 403 i filtr na HTTP                     | Potrzebny osobno. Dziś uczysz się POM i tego, co *użytkownik* widzi po tym samym tokenie.        |
| **Login w każdym teście**                              | że formularz Keycloak działa            | Wolne, kruche, nie jest SUT tych trzech przypadków. Login raz w **setup**, potem `storageState`. |
| **Trzy przeglądarki**                                  | izolacja procesów                       | Przesada. Wystarczy trzy **konteksty** w jednym `Browser`.                                       |
| **Jedna** `page`**, wyloguj, zaloguj**                 | „jak user”                              | Miesza cookies, cache, localStorage. Fałszywe przecieki albo fałszywe GREEN.                     |
| **POM**                                                | jak obsłużyć ekran                      | Trzy testy, jeden ekran. Locatory w jednym miejscu.                                              |
| **Screenplay / App facade / BasePage biznesowe**       | wiele zadań i ekranów                   | Na jeden page object to nadmiar. Dziś nie tworzysz `MyBasePage`.                                 |
| `waitForTimeout(2000)`                                 | że minęły 2 sekundy                     | Nie że UI jest gotowe. Playwright sam czeka na `expect`.                                         |
| **Selektor CSS** (`.rounded-full`, `tr:nth-child(2)`)  | klasę / pozycję                         | Padnie po redesignie. User nie klika „drugiego tr”.                                              |


Żelazna zasada POM, którą dziś ćwiczysz w praktyce:

> POM opisuje **jak** obsłużyć ekran. Test opisuje **co** powinno się wydarzyć dla danego aktora.

Jeśli w klasie strony pojawi się `if (role === 'TENANT_ADMIN')`, POM zaczął znać biznes. Wtedy nie da się użyć tej samej klasy dla trzech kont bez kłamstwa.

---



## 4. Słownik, bez którego nie napiszesz kroku 4

Zostaw ten rozdział otwarty. To nie teoria do wkuwania — to nazwy rzeczy, które zaraz typujesz.

### Playwright: trzy pudełka

```text
Browser          uruchomiona Chromium (proces)
  └─ BrowserContext   profil: własne cookies, storage, sesja  ≈ „inne incognito”
       └─ Page        karta w tym profilu
            └─ Locator     przepis „jak znaleźć element”, jeszcze nie wyszukanie
```

- `Browser` — jedna na worker. Nie zamykasz go w teście.
- `BrowserContext` — tu żyje tożsamość. `storageState` wczytujesz **tu**.
- `Page` — `page.goto`, kliknięcia, asercje.
- `Locator` — leniwy. `page.getByRole('heading', { name: 'Merchants' })` nic jeszcze nie szuka w DOM. Szuka dopiero `click()` albo `expect(...).toBeVisible()`. Dlatego `rowByReference()` zwraca `Locator`, nie `Promise<Locator>`.

Dlaczego locatory, a nie `page.$()` / ElementHandle? Handle wskazuje konkretny węzeł z chwili zapytania. SPA go wyrzuci i dostaniesz stale node. Locator pyta ponownie — to pasuje do Vue/Nuxt.

### Playwright: auto-wait vs sen

```ts
await expect(registry.heading).toBeVisible()
```

`expect` z `@playwright/test` **ponawia** sprawdzenie aż do `expect.timeout` (u nas 15 s). To nie jest `assert` z JUnita, który pyta raz i pada.

`waitForTimeout(2000)` usypia zawsze. Albo za krótko (flake), albo za długo (wolny suite), i nigdy nie mówi *czego* czekasz.

### Playwright: widoczny vs nieobecny


| Asercja             | Znaczenie                        | Kiedy                                     |
| ------------------- | -------------------------------- | ----------------------------------------- |
| `toBeVisible()`     | element jest w DOM i widoczny    | heading, wiersz, który **musi** być       |
| `toHaveCount(0)`    | locator nie ma żadnego trafienia | przycisk/tabela usunięte przez `v-if`     |
| `not.toBeVisible()` | nie jest widoczny                | przejdzie też dla elementu `hidden` w DOM |


Na tej stronie 403 i brak Create merchant to `v-if` — węzła nie ma. `toHaveCount(0)` jest tu uczciwszym oracle niż „nie widać”.

Strict mode: jeśli `getByRole('button', { name: 'Create merchant' })` trafi dwa przyciski, akcja padnie. Dlatego `exact: true`. Na stronie jest aria-label przycisku i tekst modala — nie otwierasz modala, ale `exact` i tak jest higieną.

### TypeScript, tylko to co dziś użyjesz

**Union type** — wartość może być tylko z listy:

```ts
export type PomRole = 'PLATFORM_ADMIN' | 'TENANT_ADMIN' | 'MERCHANT_MANAGER'
```

Literówka `'TENANT_ADMN'` to błąd kompilatora, nie flake o 2:00. W QA to zamknięty zbiór klas równoważności ról.

**Interface** — kształt obiektu:

```ts
export interface PomAccount {
  username: string
  password: string
  role: PomRole
  tenantId: string
  merchantId?: string  // ? = opcjonalne; tenant.admin nie ma merchanta
}
```

`as const` — „te stringi są literałami, nie byle jakim `string`”:

```ts
export const pomAuthFiles = {
  tenantAdmin: `${authDir}/tenant-admin.json`,
} as const
```

`async` **/** `await` **/** `Promise<void>` — operacja trwa (sieć, klik, zapis pliku). `void` = nic nie zwracamy, ale i tak musisz `await`, bo inaczej test skończy się w połowie logowania.

`import type` — bierzemy tylko typ, nie runtime. `import type { Page }` nie wciąga przeglądarki do bundla.

**Klasa +** `readonly` **+** `private readonly page` — POM trzyma locatory raz, stronę dostaje w konstruktorze. `readonly` = nie podmienisz `this.heading` w połowie testu.

Nie potrzebujesz dziś generyków, dekoratorów ani `namespace`.

---



## 5. Czego dziś nie robimy

- PSP, hosted checkout, eventy Checkout Lab;
- kopiowanie gotowych speców i biznesowych POM z `tests-pom`;
- mockowanie sesji, BFF, backendu;
- `MyBasePage`, App facade, rozbudowane fixtures, API client;
- testowanie wszystkich pięciu ról;
- tworzenie albo edycja merchantów;
- testy security bezpośrednio na API — wrócisz do nich REST Assuredem.

---



## 6. Pliki, które utworzysz albo zmienisz

Pracuj w `apps/frontend`.

```text
tests-pom-learner/
├── auth/
│   ├── accounts.ts                       # zmieniasz: union + fabryka tenant.admin
│   ├── keycloak.setup.ts                 # kopia — prawdziwy login OIDC
│   ├── platform-admin.setup.ts           # kopia
│   ├── merchant-manager.setup.ts         # kopia
│   └── tenant-admin.setup.ts             # piszesz
├── helpers/
│   └── runAs.ts                          # piszesz — mięso: context
├── pages/
│   ├── BasePage.ts                       # kopia tylko dla LoginPage
│   ├── LoginPage.ts                      # kopia
│   └── MyMerchantRegistryPage.ts         # piszesz od zera — mięso: POM
├── specs/
│   └── My.auth-rbac.spec.ts              # piszesz — mięso: oracłe biznesowe
└── utils/
    ├── env.ts                            # zmieniasz: ścieżka tenant-admin.json
    ├── network.ts                        # kopia
    └── storage-safety.ts                 # kopia
```

Zmieniasz też `playwright.pom-learner.config.ts`.

Istniejące `specs/session-guest.spec.ts` (i skipnięte laby) zostaw. Nie są częścią tej lekcji.

---



## 7. Gate 0 — prawdziwy stos

Bez mocków potrzebujesz: Postgres + Keycloak, Spring z **seedem**, Nuxt na `:3000`.

Z katalogu głównego repo:

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
```

Backend — **obowiązkowo profil** `seed`. Samo `dev` podnosi API, ale `SeedRunner` wisi na `@Profile("seed")`. Bez seedu tabela jest pusta i asercje na `MERCHANT_ALPHA_001` kłamią.

```bash
cd apps/backend
SPRING_PROFILES_ACTIVE=dev,seed ./mvnw spring-boot:run
```

Hasła tylko w środowisku, nigdy w `.ts`:

```bash
export PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD=platform.admin
export PLAYWRIGHT_TENANT_ADMIN_PASSWORD=tenant.admin
export PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD=merchant.manager
```

Ten sam terminal, w którym potem odpalisz Playwrighta. Eksport w innym oknie nie istnieje.

Nie commituj `tests-pom-learner/.auth/*.json` — to zrzut zalogowanej sesji.

Jeśli Keycloak wcześniej szedł na HTTPS (`--full`), issuer nie będzie `http://localhost:8081` i setup padnie na redirect. Ta lekcja jest HTTP.

---



## 8. Krok 1 — skopiuj tylko bootstrap logowania

Jedyny dozwolony copy-step. Kopiujesz OIDC, nie POM Merchant Registry.

Dlaczego wolno skopiować login, a nie wolno `MerchantsListPage`? Logowanie Keycloak jest **wspólną infrastrukturą** (jak `Authorization: Bearer` w REST Assured). Merchant Registry jest **ćwiczeniem**. Gdybyś skopiował biznesowy POM, nie zbudujesz od zera nic, czego nie umiesz już odczytać.

`cp -n` nie nadpisze Twoich plików.

```bash
cd apps/frontend

mkdir -p tests-pom-learner/{auth,helpers,pages,specs,utils}

cp -n tests-pom/auth/accounts.ts tests-pom-learner/auth/accounts.ts
cp -n tests-pom/auth/keycloak.setup.ts tests-pom-learner/auth/keycloak.setup.ts
cp -n tests-pom/auth/platform-admin.setup.ts tests-pom-learner/auth/platform-admin.setup.ts
cp -n tests-pom/auth/merchant-manager.setup.ts tests-pom-learner/auth/merchant-manager.setup.ts

cp -n tests-pom/pages/BasePage.ts tests-pom-learner/pages/BasePage.ts
cp -n tests-pom/pages/LoginPage.ts tests-pom-learner/pages/LoginPage.ts

cp -n tests-pom/utils/env.ts tests-pom-learner/utils/env.ts
cp -n tests-pom/utils/network.ts tests-pom-learner/utils/network.ts
cp -n tests-pom/utils/storage-safety.ts tests-pom-learner/utils/storage-safety.ts
```

Dlaczego też `BasePage.ts`? Na tej gałęzi `LoginPage extends BasePage`. Sama kopia `LoginPage.ts` da TS2307 (nie można znaleźć modułu). To nie jest Twój biznesowy base — nie dziedzicz z niego `MyMerchantRegistryPage`.

**STOP 1.** Upewnij się, że nie skopiowałeś `tests-pom/specs/`, `MerchantsListPage.ts`, `App.ts` ani `fixtures/index.ts`. Jeśli tak — skasuj te kopie.

Otwórz skopiowany `keycloak.setup.ts` i przeczytaj go jak kontrakt, nie jak magię:

1. idzie na login,
2. wypełnia Keycloak,
3. czeka na `/admin/merchants`,
4. sprawdza sesję (`roles`, `tenantId`, `merchantId`),
5. zapisuje `storageState` do JSON.

Punkt 4 to **oracłe sesji przed testem**. Jeśli tenant.admin dostanie złą rolę, chcesz paść w setupie, nie w asercji tabeli.

---



## 9. Krok 2 — trzecie konto w TypeScript

Otwórz `tests-pom-learner/auth/accounts.ts`.

### 2A — rozszerz union

Było:

```ts
export type PomRole = 'PLATFORM_ADMIN' | 'MERCHANT_MANAGER'
```

Ma być:

```ts
export type PomRole =
  | 'PLATFORM_ADMIN'
  | 'TENANT_ADMIN'
  | 'MERCHANT_MANAGER'
```

Dlaczego nie `string`? `string` przyjmie `'admin'` i dowie się o tym dopiero Keycloak. Union przesuwa błąd w lewo — tam, gdzie w API testach stawiasz enum ról, a nie magiczny string w headerze.

### 2B — fabryka konta

Dopisz:

```ts
export function tenantAdminAccount(): PomAccount {
  return {
    username: optionalEnv(
      'PLAYWRIGHT_TENANT_ADMIN_USERNAME',
      'tenant.admin',
    ),
    password: requiredEnv('PLAYWRIGHT_TENANT_ADMIN_PASSWORD'),
    role: 'TENANT_ADMIN',
    tenantId: 'TENANT_ALPHA',
  }
}
```

Dlaczego funkcja, a nie `export const tenantAdmin = { ... }`? Hasło czytasz z env w chwili wywołania. Stały obiekt na górze pliku rzuci przy imporcie, zanim Playwright zdąży cokolwiek powiedzieć. Fabryka = leniwe odczytanie, ten sam trik co `requiredEnv` przy API clientach.

Dlaczego nie ma `merchantId`? `tenant.admin` działa w zakresie całego `TENANT_ALPHA`. Pole jest opcjonalne (`merchantId?:`). Setup porówna `session.user?.merchantId` z `undefined` — i tak ma być.

Hasło: `requiredEnv`, nie wpisane `'tenant.admin'` w pliku. Credential w repo to defect, nie wygoda.

### 2C — ścieżka storage state

W `tests-pom-learner/utils/env.ts` rozszerz:

```ts
export const pomAuthFiles = {
  platformAdmin: `${authDir}/platform-admin.json`,
  tenantAdmin: `${authDir}/tenant-admin.json`,
  merchantManager: `${authDir}/merchant-manager.json`,
} as const
```

Config learnera ustawia `PLAYWRIGHT_POM_AUTH_DIR` na `tests-pom-learner/.auth`, więc skopiowany default `tests-pom/.auth` zostaje nadpisany. Nie twardokoduj ścieżki drugi raz w specu — jeden kontrakt.

### 2D — setup konta

Utwórz `tests-pom-learner/auth/tenant-admin.setup.ts`:

```ts
import { test as setup } from '@playwright/test'
import { tenantAdminAccount } from './accounts'
import { saveKeycloakStorageState } from './keycloak.setup'
import { pomAuthFiles } from '../utils/env'

setup('prepare tenant-admin storage state', async ({ page }) => {
  await saveKeycloakStorageState(
    page,
    tenantAdminAccount(),
    pomAuthFiles.tenantAdmin,
  )
})
```

`test as setup` to ten sam Playwright, inna nazwa w raporcie. `await` jest obowiązkowe: zapis sesji to I/O.

Wzorzec: **setup = fixture tożsamości**, test = zachowanie po zalogowaniu. W Selenium często logowałeś się w `@Before`. Tu `@Before` jest osobnym projektem, bo trzy testy chcą trzech tożsamości, a login jest drogi.

---



## 10. Krok 3 — podłącz setup do Playwrighta

W `playwright.pom-learner.config.ts` dodaj projekt:

```ts
{
  name: 'setup-tenant-admin',
  testMatch: /auth\/tenant-admin\.setup\.ts/,
},
```

W projekcie `chromium-rbac` zależności mają być trzy:

```ts
dependencies: [
  'setup-platform-admin',
  'setup-tenant-admin',
  'setup-merchant-manager',
],
```

Dlaczego `chromium-rbac` nie ma `use.storageState` na stałe? Bo jeden projekt obsługuje **wiele** tożsamości. Admin-projekt ma jedną. RBAC-projekt tworzy kontekst per test przez `runAs`.

Dlaczego `testMatch: /specs\/.*auth-rbac.*\.spec.ts/`? Żeby ten spec nie wpadł do `chromium-admin` (tam jest już sesja platform.admin). Nazwa pliku **jest** routingiem. Stąd `My.auth-rbac.spec.ts`, nie `My.merchants.spec.ts`.

Hasła nie ma w configu. Config zna nazwę projektu i plik sesji.

```text
setup-tenant-admin
    → prawdziwy login Keycloak
tenant-admin.json
    → browser.newContext({ storageState })
zalogowana, odizolowana sesja TENANT_ALPHA
```

---



## 11. Krok 4 — helper `runAs` (mięso Playwright)

Utwórz `tests-pom-learner/helpers/runAs.ts`.

To nie jest POM. Helper nie wie, co to Merchant Registry. Wie tylko: *weź przeglądarkę, załóż profil z tym plikiem sesji, daj kartę, posprzątaj*.

W Selenium analogią jest nowy driver z wgranym profilem, nie `PageFactory`.

```ts
import type { Browser, Page } from '@playwright/test'

export async function runAs(
  browser: Browser,
  storageState: string,
  action: (page: Page) => Promise<void>,
): Promise<void> {
  const context = await browser.newContext({ storageState })

  try {
    const page = await /* TODO 1: utwórz stronę w context */
    await /* TODO 2: wykonaj callback action dla page */
  } finally {
    await /* TODO 3: zamknij context */
  }
}
```

Wskazówki do TODO:

1. `context.newPage()` — karta *w tym* profilu. `browser.newPage()` byłoby kartą w domyślnym kontekście, bez Twojego `storageState`.
2. `action(page)` — to callback. Test przekaże `async (page) => { ... }`. Helper nie wie, co test zrobi.
3. `context.close()` — zamyka karty i kasuje cookies tego profilu.

Dlaczego `try / finally`, a nie samo `close()` na końcu? Asercja rzuca. Bez `finally` kontekst zostaje — w suite z trzema kontami to wyciek sesji i flaki. W API testach analogia: zamykasz połączenie w `finally`, nie tylko na happy path.

`action: (page: Page) => Promise<void>` czytaj od środka: funkcja, która dostaje `Page` i zwraca Promise. To zwykły callback, nie magia frameworka.

Dlaczego `runAs` zwraca `Promise<void>`? Tworzenie kontekstu, callback i close są asynchroniczne. Nie ma wartości biznesowej do zwrotu. Test i tak musi `await runAs(...)`, inaczej Playwright uzna test za skończony, zanim otworzysz stronę.

**STOP 2.** Jednym zdaniem: dlaczego nie wolno użyć jednej wspólnej `page` dla trzech zalogowanych kont?

Oczekiwana myśl: jedna karta należy do jednego kontekstu i jednej sesji; osobne konteksty zapobiegają mieszaniu cookies i tożsamości. To ten sam powód, dla którego w API nie reuse’ujesz `RequestSpecification` z cudzym Bearerem.

---



## 12. Krok 5 — Page Object od zera (mięso POM)

Utwórz `tests-pom-learner/pages/MyMerchantRegistryPage.ts`.

Nie otwieraj `tests-pom/pages/MerchantsListPage.ts`. Kontrakt bierz z produkcji:

- `apps/frontend/app/pages/admin/merchants/index.vue`
- `apps/frontend/app/components/merchant/MerchantTable.vue` (`aria-label="Merchant registry"`)
- `apps/frontend/app/components/shared/TenantContextBadge.vue`



### Co POM wie, a czego nie


| Wie                                     | Nie wie                                  |
| --------------------------------------- | ---------------------------------------- |
| URL `/admin/merchants`                  | które konto to otwiera                   |
| heading, badge, tabela, przycisk, alert | czy brak wiersza jest poprawny biznesowo |
| jak znaleźć wiersz po reference         | czy Beta wolno widzieć                   |


Role-based oczekiwania w POM = ukryty `if`. Potem nie przetestujesz managera tą samą klasą.

### Preferowane API vs zakazane

```ts
page.getByRole('heading', { name: 'Merchants' })
page.getByTestId('tenant-context-badge')
page.getByRole('table', { name: 'Merchant registry' })
page.getByRole('button', { name: 'Create merchant', exact: true })
page.getByRole('alert').filter({ hasText: /permission/i })
page.getByRole('row').filter({ hasText: reference })
```

Zakaz:

```ts
page.locator('.rounded-full')     // klasa CSS przycisku — implementacja, nie kontrakt
page.locator('tr:nth-child(2)')   // pozycja — padnie po sortowaniu
page.waitForTimeout(2000)
```

Dlaczego `getByRole`? To jest nazwa, którą słyszy user i czytnik ekranu. Redesign koloru nie rozwala testu. W QA: asercja na **obserwowalny kontrakt**, nie na DOM cosmetics.

Dlaczego `getByTestId` na badge? Badge nie ma sensownej roli. `data-testid` jest świadomym hakiem na element bez nazwy dostępności. To wyjątek, nie domyślna strategia. Najpierw rola, potem label, testid na końcu.

Dlaczego `filter({ hasText })` na wierszu, a nie XPath `//tr[td[1]='...']`? XPath przywiązuje do kolumny. `hasText` mówi: wiersz, który zawiera ten reference. Kolumna może się przesunąć.

### Starter — uzupełnij TODO

```ts
import type { Locator, Page } from '@playwright/test'

export class MyMerchantRegistryPage {
  readonly heading: Locator
  readonly tenantBadge: Locator
  readonly table: Locator
  readonly createMerchantButton: Locator
  readonly accessDeniedAlert: Locator

  constructor(private readonly page: Page) {
    this.heading = /* TODO: heading o nazwie Merchants */
    this.tenantBadge = /* TODO: tenant-context-badge */
    this.table = /* TODO: tabela o nazwie Merchant registry */
    this.createMerchantButton = /* TODO: przycisk Create merchant */
    this.accessDeniedAlert = /* TODO: alert zawierający brak permission */
  }

  async open(): Promise<void> {
    await /* TODO: przejdź na /admin/merchants */
  }

  rowByReference(reference: string): Locator {
    return /* TODO: wiersz tabeli zawierający reference */
  }
}
```

`open()` robi `this.page.goto('/admin/merchants')`. Nie asercjonuj tu, że tabela jest widoczna — manager jej nie ma. Nawigacja ≠ loaded-as-admin. Jeśli `open()` sprawdzi Create merchant, POM znowu poznał rolę.

Tekst alertu z produkcji: `You do not have permission to view merchants`. Matcher `/permission/i` jest odporniejszy na drobną zmianę title vs description; nadal nie złapie byle błędu sieci (tam jest `ErrorState`, nie ten alert).

---



## 13. Krok 6 — trzy scenariusze (mięso oracła)

Utwórz `tests-pom-learner/specs/My.auth-rbac.spec.ts`.

Nazwa `auth-rbac` wrzuca plik do projektu `chromium-rbac`, który umie pracować na wielu `storageState`.

Fixture `{ browser }` — nie `{ page }`. Domyślna `page` należy do pustego/nie tego kontekstu. Tożsamość zakładasz sam przez `runAs`.

Pierwszy test jest wzorem konstrukcji. Dwa następne piszesz sam.

```ts
import { expect, test } from '@playwright/test'
import { runAs } from '../helpers/runAs'
import { MyMerchantRegistryPage } from '../pages/MyMerchantRegistryPage'
import { pomAuthFiles } from '../utils/env'

test.describe('Merchant Registry — account and tenant scope', () => {
  test('platform admin sees cross-tenant merchant data', async ({ browser }) => {
    await runAs(browser, pomAuthFiles.platformAdmin, async (page) => {
      const registry = new MyMerchantRegistryPage(page)

      await registry.open()

      await expect(registry.heading).toBeVisible()
      await expect(registry.tenantBadge).toHaveText('Platform')
      await expect(registry.createMerchantButton).toBeVisible()
      await expect(registry.rowByReference('MERCHANT_ALPHA_001')).toBeVisible()
      await expect(registry.rowByReference('MERCHANT_BETA_001')).toBeVisible()
    })
  })

  test('TODO: tenant admin sees only TENANT_ALPHA merchants', async ({ browser }) => {
    // Napisz sam według tabeli akceptacji.
  })

  test('TODO: merchant manager is stopped by RBAC before tenant filtering', async ({ browser }) => {
    // Napisz sam według tabeli akceptacji.
  })
})
```



### Tabela akceptacji


| Przypadek        | `storageState`                 | Badge          | Widoczne                                                                     | Niewidoczne                                 |
| ---------------- | ------------------------------ | -------------- | ---------------------------------------------------------------------------- | ------------------------------------------- |
| Tenant admin     | `pomAuthFiles.tenantAdmin`     | `Alpha Tenant` | heading, tabela, Create merchant, `MERCHANT_ALPHA_001`, `MERCHANT_ALPHA_002` | `MERCHANT_BETA_001` → `toHaveCount(0)`      |
| Merchant manager | `pomAuthFiles.merchantManager` | `Alpha Tenant` | alert z brakiem permission                                                   | tabela i Create merchant → `toHaveCount(0)` |


Manager **ma** badge `Alpha Tenant`. Badge jest z sesji w layoutcie, nie z listy merchantów. To nie jest dowód, że widzi dane Alpha. Nie asercjonuj, że badge znika — nie znika.

Nie filtruj UI, żeby ukryć Betę przed asercją. Jeśli tenant.admin widzi `MERCHANT_BETA_001`, to fail izolacji. Zapisz trace, nie „poprawiaj” testu `filter()`.

### Jak czytać te trzy testy na recenzji

- platform + tenant admin = oba przeszły RBAC, różnią się **zbiorem wierszy** → dowód tenant scope w tym flow E2E;
- manager = brak uprawnienia do funkcji;
- sam manager **nie** dowodzi, że backend filtruje po tenancie.

W pakiecie testów API analogia: 403 na `GET /merchants` ≠ test IDOR. IDOR to 200 z obcym id albo pusta lista przy legalnym read.

Każdy `test(...)` ma własny `runAs` → własny context. Nie dziel stanu między przypadkami. To test independence, nie fanaberia.

---



## 14. Gate 1 — typecheck i discovery

```bash
cd apps/frontend
corepack pnpm typecheck
```

Potem:

```bash
corepack pnpm exec playwright test \
  --config playwright.pom-learner.config.ts \
  --project chromium-rbac \
  --list
```

`--list` odpowiada: czy config i importy TS się składają, czy nazwa speca trafia w `testMatch`, czy projekt widzi testy. **Nie** dowodzi, że logowanie działa.

Jeśli lista jest pusta: nazwa pliku albo `--project`. Jeśli TS2307 na `LoginPage`: nie skopiowałeś `BasePage.ts`.

---



## 15. Gate 2 — uruchom tylko tę lekcję

W tym samym terminalu co trzy `export` haseł:

```bash
corepack pnpm exec playwright test \
  tests-pom-learner/specs/My.auth-rbac.spec.ts \
  --config playwright.pom-learner.config.ts \
  --project chromium-rbac
```

Setup padł? Odpal sam winowajcę:

```bash
corepack pnpm exec playwright test \
  --config playwright.pom-learner.config.ts \
  --project setup-tenant-admin
```


| Objaw                                     | Najpierw sprawdź                                          |
| ----------------------------------------- | --------------------------------------------------------- |
| Brak zmiennej środowiskowej               | trzy hasła w **tej** powłoce                              |
| Brak `tenant-admin.json`                  | projekt setup + `dependencies`                            |
| Redirect / login timeout                  | Keycloak HTTP, Spring `dev,seed`, origin `localhost:3000` |
| Pusty rejestr / brak `MERCHANT_ALPHA_001` | profil `seed`, nie samo `dev`                             |
| TS2307 przy LoginPage                     | kopia `BasePage.ts`                                       |
| Test nieznaleziony                        | `My.auth-rbac.spec.ts` + `--project chromium-rbac`        |
| Tenant admin widzi Betę                   | nie maskuj filtrem; to defect izolacji albo zły oracle    |


---



## 16. Gate 3 — kontrolowana czerwień

Zmień na chwilę jedno poprawne oczekiwanie, np. badge na `'Wrong Tenant'`. Odpal ten jeden test. Zobacz diff. Przywróć. Znowu GREEN.

Po co? Test, którego nigdy nie widziałeś na czerwono, może asercjonować powietrze. To ten sam nawyk, co mutation testing w miniaturze: zepsuj oracle, upewnij się, że suite krzyczy.

Nie commituj zepsutej asercji.

---



## 17. Podział odpowiedzialności


| Warstwa                     | Odpowiedzialność              | Czego tam nie ma           |
| --------------------------- | ----------------------------- | -------------------------- |
| `accounts.ts`               | typ i tożsamość konta         | locatorów, asercji UI      |
| setup auth                  | prawdziwy login + zapis sesji | „kto widzi Betę”           |
| `runAs.ts`                  | lifecycle kontekstu           | wiedzy o Merchant Registry |
| `MyMerchantRegistryPage.ts` | locatory i nawigacja          | reguł RBAC/tenant          |
| spec                        | wybór konta i oczekiwania     | CSS i szczegółów Keycloak  |


Jeśli nie wiesz, gdzie włożyć linię: czy zmiana roli powinna jej dotyczyć? Nie → POM. Tak → spec.

---



## 18. Definition of Done

- [ ] pracujesz na `checkout-protocol-lab-foundation`
- [ ] nie skopiowałeś gotowego speca ani biznesowego POM
- [ ] `TENANT_ADMIN` jest w `PomRole`
- [ ] setup tworzy `tenant-admin.json` przez prawdziwy Keycloak
- [ ] każde konto ma osobny `BrowserContext`
- [ ] `MyMerchantRegistryPage` nie zawiera reguł kto-co-widzi
- [ ] spec ma trzy czytelne przypadki
- [ ] zero `page.route()`, `route.fulfill()`, `waitForTimeout()`
- [ ] `corepack pnpm typecheck` przechodzi
- [ ] `--list` znajduje testy
- [ ] ukierunkowane uruchomienie jest GREEN
- [ ] jedna kontrolowana czerwień i powrót do GREEN

---



## 19. Cztery pytania rekrutacyjne

**1. Why a separate BrowserContext per account?**

Each context has isolated cookies and session state. It prevents one user's authentication from leaking into another user's test.

Po polsku: kontekst to osobny profil. Sesje się nie mieszają. To nie to samo co nowy proces przeglądarki i nie to samo co `clearCookies()` na wspólnej karcie.

**2. Does a hidden button prove backend authorization?**

No. It proves UI behavior. The backend must enforce authorization independently; sensitive paths also need API-level negative tests.

Ukryty przycisk to *presentation*. 403 na `GET /api/merchants` to *enforcement*. Dziś widzisz oba przez UI (alert po 403), ale to nadal nie zastępuje kontraktu API.

**3. Why does merchant manager 403 not prove tenant isolation?**

RBAC rejected the request before tenant filtering ran. Isolation needs an account with read authority and a check that foreign-tenant rows are still absent.

**4. What belongs in a Page Object?**

Locators and reusable UI interactions. Account selection, permission rules and business expectations stay in the test.

---



## 20. Raport po lekcji

Odpowiedz krótko:

1. Jakie pliki utworzyłeś albo zmieniłeś?
2. Dlaczego `tenant.admin` nadaje się do testu izolacji, a `merchant.manager` nie?
3. Co w Twoim kodzie jest POM, a co helperem? Czym się różnią?
4. Co zwraca `runAs(...)` i dlaczego to `Promise<void>`?
5. Dlaczego `rowByReference` zwraca `Locator`, a `open()` zwraca `Promise<void>`?
6. Wklej `--list` i końcowe trzy testy.

Następny mały krok (nie teraz): ten sam model na `MyPaymentsPage` dla `merchant.manager` — swój merchant dostępny, obcy odrzucony. Najpierw domknij jeden POM i trzy sesje.