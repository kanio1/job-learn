# Lesson 1: AssertJ allMatch, Lambdas, Predicate and API Response Validation

## Lekcja 1: AssertJ `allMatch`, lambdy, `Predicate` i walidacja odpowiedzi API

## 1. Krotka intuicja

Wyobraz sobie, ze backend zwraca liste platnosci. Ty jako SDET nie patrzysz tylko na to, czy endpoint odpowiedzial `200 OK`. Patrzysz tez na to, czy dane w odpowiedzi sa zgodne z kontraktem.

Przyklad myslenia testera:

```text
Endpoint zwrocil liste platnosci.
Oczekuje, ze kazda platnosc ma walute PLN.
Jesli chociaz jedna platnosc ma EUR, USD albo null, test powinien to wykryc.
```

Kod:

```java
assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()));
```

Na pierwszy rzut oka to wyglada jak magia. W praktyce to tylko zdanie zapisane w Javie:

```text
Sprawdz, ze zawartosc odpowiedzi sklada sie wylacznie z elementow,
ktore spelniaja warunek: waluta elementu jest rowna PLN.
```

Analogia dla 15-latka: masz koszyk z kartami. Na kazdej karcie jest waluta. `allMatch` to nauczyciel, ktory sprawdza kazda karte i mowi: "Kazda karta musi miec napis PLN". Jesli jedna karta ma EUR, cala kontrola nie przechodzi.

Dlaczego to wazne dla SDET / Senior QA Automation:

- API moze miec poprawny status HTTP, ale bledne dane.
- Test automatyczny powinien jasno pokazywac, co bylo oczekiwane i co przyszlo z systemu.
- Fluent assertions w AssertJ pomagaja pisac testy czytelne jak zdania.
- `Predicate<T>` pozwala nazywac reguly walidacji, czyli zamieniac "techniczny kod" w "jezyk testu".

## 2. Kod startowy

Zaczynamy od bardzo prostego modelu domenowego. To nie jest pelny system platnosci. To minimalny model do nauki walidacji odpowiedzi API.

```java
import java.util.List;

record PaymentDto(
        String id,
        String currency,
        int amount,
        String status
) {
}

record PageResponse<T>(
        List<T> content
) {
}
```

Przykladowa odpowiedz API jako obiekt Java:

```java
PageResponse<PaymentDto> response = new PageResponse<>(
        List.of(
                new PaymentDto("p1", "PLN", 100, "PAID"),
                new PaymentDto("p2", "PLN", 250, "PAID"),
                new PaymentDto("p3", "PLN", 300, "PENDING")
        )
);
```

Pelny test JUnit 5 z AssertJ:

```java
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentApiResponseTest {

    @Test
    void shouldReturnOnlyPaymentsInPln() {
        PageResponse<PaymentDto> response = new PageResponse<>(
                List.of(
                        new PaymentDto("p1", "PLN", 100, "PAID"),
                        new PaymentDto("p2", "PLN", 250, "PAID"),
                        new PaymentDto("p3", "PLN", 300, "PENDING")
                )
        );

        assertThat(response.content())
                .allMatch(item -> "PLN".equals(item.currency()));
    }
}
```

W prawdziwym tescie REST API obiekt `response` moglby powstac po deserializacji JSON-a z REST Assured. Na tym etapie tworzymy go recznie, bo uczymy sie mechanizmu.

## 3. Wyjasnienie linia po linii

### `record PaymentDto(...)`

```java
record PaymentDto(
        String id,
        String currency,
        int amount,
        String status
) {
}
```

`PaymentDto` to pojedyncza platnosc w odpowiedzi API.

DTO oznacza Data Transfer Object. To obiekt, ktory przenosi dane miedzy API a testem. Nie musi miec logiki biznesowej. Ma przechowywac dane.

`record` w Javie automatycznie tworzy:

- konstruktor,
- metody dostepu, np. `id()`, `currency()`, `amount()`, `status()`,
- `equals`,
- `hashCode`,
- `toString`.

Czyli ten zapis:

```java
new PaymentDto("p1", "PLN", 100, "PAID")
```

tworzy jeden obiekt platnosci:

```text
id       = p1
currency = PLN
amount   = 100
status   = PAID
```

### `record PageResponse<T>(List<T> content)`

```java
record PageResponse<T>(
        List<T> content
) {
}
```

`PageResponse<T>` to odpowiedz stronicowana albo kontener na liste elementow.

`T` oznacza "jakis typ". To typ generyczny.

Jesli napiszemy:

```java
PageResponse<PaymentDto> response
```

to Java rozumie:

```text
To jest odpowiedz, ktorej content zawiera liste PaymentDto.
```

Dlatego:

```java
response.content()
```

zwraca:

```java
List<PaymentDto>
```

A nie pojedynczy `PaymentDto`.

### `PageResponse<PaymentDto> response = ...`

```java
PageResponse<PaymentDto> response = new PageResponse<>(
        List.of(
                new PaymentDto("p1", "PLN", 100, "PAID"),
                new PaymentDto("p2", "PLN", 250, "PAID"),
                new PaymentDto("p3", "PLN", 300, "PENDING")
        )
);
```

`response` to zmienna. Trzyma cala odpowiedz API.

Nie trzyma jednej platnosci. Trzyma obiekt `PageResponse`, a w nim jest pole `content`, czyli lista platnosci.

Mozesz o tym myslec tak:

```text
response
  content
    [0] PaymentDto("p1", "PLN", 100, "PAID")
    [1] PaymentDto("p2", "PLN", 250, "PAID")
    [2] PaymentDto("p3", "PLN", 300, "PENDING")
```

### Co oznacza `response.content()`

```java
List<PaymentDto> payments = response.content();
```

Ta linia wyciaga liste platnosci z odpowiedzi.

`response` to caly kontener.

`content` to nazwa komponentu rekordu `PageResponse`.

`content()` to metoda automatycznie wygenerowana przez `record`.

Poniewaz `PageResponse<T>` zostal uzyty jako `PageResponse<PaymentDto>`, metoda `content()` zwraca dokladnie:

```java
List<PaymentDto>
```

### Pojedynczy obiekt vs lista

Pojedynczy obiekt:

```java
PaymentDto payment = new PaymentDto("p1", "PLN", 100, "PAID");
```

To jedna platnosc.

Lista obiektow:

```java
List<PaymentDto> payments = List.of(
        new PaymentDto("p1", "PLN", 100, "PAID"),
        new PaymentDto("p2", "PLN", 250, "PAID"),
        new PaymentDto("p3", "PLN", 300, "PENDING")
);
```

To kilka platnosci w jednej kolekcji.

Analogia: `PaymentDto` to jedna karta z danymi platnosci. `List<PaymentDto>` to talia kart.

### Czym jest `List`

`List` to kolekcja elementow w okreslonej kolejnosci.

Cechy `List`:

- moze miec 0, 1 albo wiele elementow,
- elementy maja kolejnosc,
- mozna przechodzic po elementach jeden po drugim,
- mozna pobierac element po indeksie, np. `payments.get(0)`.

Przyklad:

```java
PaymentDto first = payments.get(0);
```

### Czym jest `Iterable`

`Iterable` to cos, po czym mozna iterowac, czyli przechodzic element po elemencie.

`List` implementuje `Iterable`, dlatego mozna napisac:

```java
for (PaymentDto payment : payments) {
    System.out.println(payment.currency());
}
```

To znaczy:

```text
Dla kazdej platnosci z listy payments wykonaj kod w bloku.
```

AssertJ moze wykonac asercje na kolekcji, bo `List` jest kolekcja i jest iterowalna. AssertJ wie, jak przejsc po elementach listy i sprawdzic warunek dla kazdego elementu.

### `assertThat(response.content())`

```java
assertThat(response.content())
```

`assertThat` pochodzi z AssertJ:

```java
import static org.assertj.core.api.Assertions.assertThat;
```

To poczatek asercji.

Asercja to sprawdzenie w tescie.

`assertThat(response.content())` oznacza:

```text
Sprawdzam liste platnosci, ktora przyszla w odpowiedzi.
```

AssertJ jest fluent API. Fluent oznacza, ze kolejne metody tworza zdanie.

```java
assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()));
```

Czytamy:

```text
Assert that response content all match condition.
Sprawdz, ze wszystkie elementy zawartosci odpowiedzi spelniaja warunek.
```

### AssertJ vs `assertTrue`

Wersja z JUnit `assertTrue`:

```java
assertTrue(response.content().stream()
        .allMatch(item -> "PLN".equals(item.currency())));
```

Wersja z AssertJ:

```java
assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()));
```

Obie moga sprawdzic to samo, ale wersja AssertJ jest bardziej czytelna w testach SDET.

Dlaczego:

- zaczyna od danych actual: `response.content()`,
- mowi, jaka regule sprawdzamy: `allMatch`,
- lepiej pasuje do wielu asercji na kolekcjach,
- zwykle daje bardziej diagnostyczne komunikaty bledu niz gole `assertTrue(false)`.

`assertTrue` mowi tylko: "oczekiwalem true, dostalem false". AssertJ czesto pokazuje kontekst kolekcji, ktora nie spelnila warunku.

### Lambda `item -> "PLN".equals(item.currency())`

```java
item -> "PLN".equals(item.currency())
```

To lambda expression.

Lambda to krotki zapis funkcji przekazywanej jako wartosc.

`item` to pojedynczy element listy. W tym przypadku jeden `PaymentDto`.

Skad Java wie, ze `item` jest `PaymentDto`?

Bo `response.content()` ma typ:

```java
List<PaymentDto>
```

A metoda `allMatch` dla tej kolekcji oczekuje reguly, ktora przyjmie element tej listy. Elementem listy jest `PaymentDto`. Dlatego Java wywnioskuje typ `item`.

Mozna zapisac jawnie:

```java
(PaymentDto item) -> "PLN".equals(item.currency())
```

Ale zwykle nie trzeba, bo Java potrafi wywnioskowac typ z kontekstu.

Strzalka `->` oznacza:

```text
wez item i zwroc wynik wyrazenia po prawej stronie
```

Prawa strona:

```java
"PLN".equals(item.currency())
```

zwraca `boolean`, czyli `true` albo `false`.

Dlaczego musi byc `boolean`?

Bo `allMatch` potrzebuje odpowiedzi na pytanie:

```text
Czy ten element spelnia warunek?
```

Dla kazdego elementu wynik musi byc:

```text
true  - element pasuje
false - element nie pasuje
```

### Lambda jako zwykla metoda

Ten kod:

```java
item -> "PLN".equals(item.currency())
```

jest skrotem dla myslenia:

```java
static boolean hasPlnCurrency(PaymentDto item) {
    return "PLN".equals(item.currency());
}
```

Czyli:

```text
Daj mi platnosc.
Sprawdze jej walute.
Zwroce true, jesli waluta to PLN.
Zwroce false, jesli waluta jest inna.
```

Lambda jest krotszym zapisem malej metody, ktora ma jedno zadanie.

## 4. Mechanizm myslenia

### `Predicate<T>`

```java
import java.util.function.Predicate;

Predicate<PaymentDto> hasPlnCurrency =
        item -> "PLN".equals(item.currency());
```

`Predicate<PaymentDto>` oznacza:

```text
Regula, ktora bierze PaymentDto i zwraca true albo false.
```

Mozesz myslec o `Predicate` jak o bramkarzu na imprezie.

- Przyjmuje jedna osobe, czyli jeden obiekt.
- Sprawdza warunek.
- Mowi: wchodzisz (`true`) albo nie wchodzisz (`false`).

W testach API `Predicate` jest bardzo naturalny, bo wiele walidacji brzmi tak:

```text
Czy ta platnosc ma walute PLN?
Czy ta platnosc ma dodatnia kwote?
Czy ta platnosc ma status PAID?
Czy ten merchant jest aktywny?
Czy ten blad walidacji dotyczy pola amount?
```

Przyklady predykatow:

```java
Predicate<PaymentDto> hasPlnCurrency =
        item -> "PLN".equals(item.currency());

Predicate<PaymentDto> hasPositiveAmount =
        item -> item.amount() > 0;

Predicate<PaymentDto> hasPaidStatus =
        item -> "PAID".equals(item.status());
```

Laczenie predykatow:

```java
Predicate<PaymentDto> validPayment =
        hasPlnCurrency.and(hasPositiveAmount);
```

To znaczy:

```text
Platnosc jest validPayment, jesli ma walute PLN i dodatnia kwote.
```

Mozna tez laczyc wiecej:

```java
Predicate<PaymentDto> paidPlnPaymentWithPositiveAmount =
        hasPlnCurrency
                .and(hasPositiveAmount)
                .and(hasPaidStatus);
```

Kiedy warto nazwac `Predicate`:

- gdy warunek ma znaczenie biznesowe albo testowe,
- gdy warunek powtarza sie w kilku asercjach,
- gdy lambda inline robi sie za dluga,
- gdy chcesz, zeby test czytal sie jak scenariusz.

Kiedy lambda inline wystarczy:

- gdy warunek jest bardzo krotki,
- gdy pojawia sie tylko raz,
- gdy nazwa predykatu nie dodalaby jasnosci.

Przyklad inline:

```java
assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()));
```

Przyklad z nazwanym predykatem:

```java
Predicate<PaymentDto> hasPlnCurrency =
        item -> "PLN".equals(item.currency());

assertThat(response.content())
        .allMatch(hasPlnCurrency);
```

### `allMatch` jako sposob myslenia

`allMatch` oznacza:

```text
Kazdy element kolekcji musi spelniac warunek.
```

Dla listy:

```java
List.of(
        new PaymentDto("p1", "PLN", 100, "PAID"),
        new PaymentDto("p2", "PLN", 250, "PAID"),
        new PaymentDto("p3", "EUR", 300, "PAID")
)
```

sprawdzenie:

```java
.allMatch(item -> "PLN".equals(item.currency()))
```

ma mentalny przebieg:

```text
p1 -> PLN == PLN -> true
p2 -> PLN == PLN -> true
p3 -> EUR == PLN -> false
calosc -> false
```

Test przechodzi tylko wtedy, gdy kazdy element zwroci `true`.

Test pada, gdy chociaz jeden element zwroci `false`.

Porownanie:

```java
assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()));
```

Znaczenie:

```text
Wszystkie platnosci maja walute PLN.
```

```java
assertThat(response.content())
        .anyMatch(item -> "PENDING".equals(item.status()));
```

Znaczenie:

```text
Przynajmniej jedna platnosc ma status PENDING.
```

```java
assertThat(response.content())
        .noneMatch(item -> item.amount() < 0);
```

Znaczenie:

```text
Zadna platnosc nie ma kwoty mniejszej od 0.
```

Prosta sciaga:

| Metoda | Pytanie testowe | Przyklad |
|---|---|---|
| `allMatch` | Czy wszystkie elementy spelniaja warunek? | Wszystkie waluty to PLN |
| `anyMatch` | Czy przynajmniej jeden element spelnia warunek? | Istnieje platnosc PENDING |
| `noneMatch` | Czy zaden element nie spelnia warunku? | Nie ma ujemnych kwot |

### Czy to jest Stream?

Ten kod:

```java
assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()));
```

nie jest bezposrednio Java Stream API, bo nie ma:

```java
response.content().stream()
```

To jest AssertJ fluent assertion.

Ale koncepcyjnie dziala podobnie do:

```java
boolean result = response.content().stream()
        .allMatch(item -> "PLN".equals(item.currency()));
```

Roznica jest wazna.

Stream API:

```java
boolean result = response.content().stream()
        .allMatch(item -> "PLN".equals(item.currency()));
```

zwraca wartosc logiczna:

```text
true albo false
```

AssertJ:

```java
assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()));
```

wykonuje asercje testowa:

```text
Jesli warunek jest spelniony, test idzie dalej.
Jesli warunek nie jest spelniony, test pada.
```

W testach AssertJ czesto jest czytelniejszy, bo od razu pokazuje intencje testowa.

## 5. Wariant z petla

Zanim polubisz lambdy i AssertJ, warto zobaczyc wersje najbardziej doslowna.

```java
List<PaymentDto> payments = response.content();

for (PaymentDto payment : payments) {
    if (!"PLN".equals(payment.currency())) {
        throw new AssertionError("Expected currency PLN but was " + payment.currency());
    }
}
```

To mowi:

```text
Wez liste platnosci.
Dla kazdej platnosci sprawdz walute.
Jesli waluta nie jest PLN, rzuc blad testu.
```

Zalety petli:

- jest bardzo doslowna,
- dobra dla poczatkujacego,
- dobra, gdy potrzebujesz wielu krokow, logowania albo bardziej zlozonego debugowania.

Wady petli:

- wiecej kodu,
- mniej przypomina zdanie testowe,
- latwiej zrobic niespojny komunikat bledu,
- recznie budujesz mechanizm asercji.

Wersja petli pokazuje mechanike. Wersja AssertJ pokazuje intencje.

## 6. Wariant ze Stream API

```java
boolean allPaymentsAreInPln = response.content().stream()
        .allMatch(item -> "PLN".equals(item.currency()));

assertThat(allPaymentsAreInPln)
        .isTrue();
```

Co tu sie dzieje:

```text
1. response.content() zwraca List<PaymentDto>.
2. stream() tworzy strumien elementow.
3. allMatch sprawdza kazdy element.
4. Wynik trafia do boolean allPaymentsAreInPln.
5. AssertJ sprawdza, czy wynik jest true.
```

To jest poprawne, ale w testach API czesto mniej eleganckie niz bezposrednie AssertJ na kolekcji.

Kiedy Stream API jest dobre:

- gdy chcesz przeksztalcic dane,
- gdy chcesz zrobic `map`, `filter`, `sorted`, `toList`,
- gdy wynik logiczny albo lista jest pozniej uzywana w kilku miejscach.

Przyklad:

```java
List<String> actualCurrencies = response.content().stream()
        .map(PaymentDto::currency)
        .toList();

assertThat(actualCurrencies)
        .containsOnly("PLN");
```

Tutaj Stream API sluzy do przygotowania danych actual.

## 7. Wariant z AssertJ

Najprostsza asercja:

```java
assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()));
```

Kilka asercji na odpowiedzi:

```java
assertThat(response.content())
        .isNotEmpty()
        .allMatch(item -> "PLN".equals(item.currency()))
        .anyMatch(item -> "PENDING".equals(item.status()))
        .noneMatch(item -> item.amount() < 0);
```

Uwaga dla SDET: jeden lancuch asercji jest ladny, ale czasem osobne asercje daja lepsza diagnostyke.

Czytelniej w raporcie testowym moze byc tak:

```java
assertThat(response.content())
        .isNotEmpty();

assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()));

assertThat(response.content())
        .anyMatch(item -> "PENDING".equals(item.status()));

assertThat(response.content())
        .noneMatch(item -> item.amount() < 0);
```

Dlaczego?

Bo gdy test padnie, szybciej widzisz, ktora regule zlamano.

## 8. Wariant z nazwanym `Predicate`

```java
import java.util.function.Predicate;

Predicate<PaymentDto> hasPlnCurrency =
        item -> "PLN".equals(item.currency());

Predicate<PaymentDto> hasPositiveAmount =
        item -> item.amount() > 0;

Predicate<PaymentDto> hasPaidStatus =
        item -> "PAID".equals(item.status());

Predicate<PaymentDto> validPayment =
        hasPlnCurrency.and(hasPositiveAmount);
```

Uzycie:

```java
assertThat(response.content())
        .allMatch(hasPlnCurrency);

assertThat(response.content())
        .allMatch(hasPositiveAmount);

assertThat(response.content())
        .allMatch(validPayment);
```

Wersja z metoda:

```java
static boolean hasPlnCurrency(PaymentDto item) {
    return "PLN".equals(item.currency());
}
```

Mozna jej uzyc jako method reference:

```java
assertThat(response.content())
        .allMatch(PaymentApiResponseTest::hasPlnCurrency);
```

Co jest lepsze?

- Inline lambda: dobra dla bardzo prostego warunku.
- Named `Predicate`: dobry, gdy regule chcesz nazwac.
- Method reference: dobra, gdy masz juz metode z dobra nazwa.

## 9. Praktyka SDET expected vs actual

W testach SDET czesto myslimy tak:

```text
actual   = to, co faktycznie zwrocil system
expected = to, czego oczekuje test
```

Przyklad z walutami:

```java
List<String> actualCurrencies = response.content().stream()
        .map(PaymentDto::currency)
        .toList();

assertThat(actualCurrencies)
        .containsOnly("PLN");
```

`actualCurrencies` to dane faktyczne wyciagniete z odpowiedzi.

`"PLN"` to expected, czyli oczekiwana waluta.

Krok po kroku:

```java
response.content()
```

zwraca liste platnosci.

```java
.stream()
```

tworzy strumien platnosci.

```java
.map(PaymentDto::currency)
```

zamienia kazda platnosc na jej walute.

```java
.toList()
```

zbiera waluty do listy.

Dla danych:

```java
List.of(
        new PaymentDto("p1", "PLN", 100, "PAID"),
        new PaymentDto("p2", "PLN", 250, "PAID"),
        new PaymentDto("p3", "PLN", 300, "PENDING")
)
```

wynik bedzie:

```java
List.of("PLN", "PLN", "PLN")
```

Asercja:

```java
assertThat(actualCurrencies)
        .containsOnly("PLN");
```

oznacza:

```text
Lista moze miec wiele elementow, ale wszystkie wartosci maja byc PLN.
```

### Wersja AssertJ z `extracting`

```java
assertThat(response.content())
        .extracting(PaymentDto::currency)
        .containsOnly("PLN");
```

To robi podobna rzecz, ale bez recznego tworzenia `actualCurrencies`.

Czytamy:

```text
Z listy platnosci wyciagnij currency i sprawdz, ze zawiera tylko PLN.
```

### `allMatch` vs `extracting(...).containsOnly(...)`

Wersja `allMatch`:

```java
assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()));
```

Mowi:

```text
Kazdy obiekt PaymentDto spelnia warunek: currency == PLN.
```

Wersja `extracting`:

```java
assertThat(response.content())
        .extracting(PaymentDto::currency)
        .containsOnly("PLN");
```

Mowi:

```text
Wyciagam konkretne pole currency i sprawdzam wartosci tego pola.
```

Kiedy uzyc `allMatch`:

- gdy warunek dotyczy calego obiektu,
- gdy laczysz kilka pol, np. currency i amount,
- gdy masz nazwany `Predicate`.

Kiedy uzyc `containsOnly`:

- gdy chcesz sprawdzic zbior wartosci,
- gdy nie interesuje Cie liczba powtorzen,
- gdy chcesz powiedziec: "w tej kolekcji sa tylko takie wartosci".

Kiedy uzyc `extracting`:

- gdy chcesz sprawdzic konkretne pole DTO,
- gdy expected vs actual ma byc bardzo widoczne,
- gdy chcesz poprawic komunikat bledu dla listy pol.

Przyklad porownania calych obiektow:

```java
List<PaymentDto> expectedPayments = List.of(
        new PaymentDto("p1", "PLN", 100, "PAID"),
        new PaymentDto("p2", "PLN", 250, "PAID")
);

assertThat(response.content())
        .containsExactlyElementsOf(expectedPayments);
```

To ma sens, gdy oczekujesz dokladnej listy obiektow.

Przyklad sprawdzania tylko jednego pola:

```java
assertThat(response.content())
        .extracting(PaymentDto::status)
        .contains("PENDING");
```

To ma sens, gdy status jest wazny, ale reszta danych nie jest przedmiotem tej konkretnej asercji.

## 10. Pulapki

### Pulapka 1: odwrocone `equals`

Ryzykowny zapis:

```java
item.currency().equals("PLN")
```

Jesli `currency()` zwroci `null`, test nie dostanie ladnej informacji o niespelnionej regule. Dostanie `NullPointerException`.

Bezpieczniejszy zapis:

```java
"PLN".equals(item.currency())
```

Jesli `currency()` jest `null`, wynik bedzie po prostu `false`.

### Pulapka 2: za sprytna lambda

Slaby przyklad:

```java
assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()) && item.amount() > 0 && !item.id().isBlank() && Set.of("PAID", "PENDING").contains(item.status()));
```

Problem: technicznie dziala, ale trudno czytac i trudno diagnozowac.

Lepszy wariant:

```java
Predicate<PaymentDto> hasPlnCurrency = item -> "PLN".equals(item.currency());
Predicate<PaymentDto> hasPositiveAmount = item -> item.amount() > 0;
Predicate<PaymentDto> hasNonBlankId = item -> item.id() != null && !item.id().isBlank();

assertThat(response.content())
        .allMatch(hasPlnCurrency)
        .allMatch(hasPositiveAmount)
        .allMatch(hasNonBlankId);
```

Jeszcze lepiej diagnostycznie: osobne asercje z opisami.

```java
assertThat(response.content())
        .as("all payments should use PLN currency")
        .allMatch(hasPlnCurrency);

assertThat(response.content())
        .as("all payments should have positive amount")
        .allMatch(hasPositiveAmount);
```

### Pulapka 3: `allMatch` na pustej liscie

To jest bardzo wazne.

W Java Stream API:

```java
List.<PaymentDto>of().stream()
        .allMatch(item -> "PLN".equals(item.currency()))
```

zwraca `true`.

Dlaczego? Bo nie znaleziono zadnego elementu, ktory lamie warunek.

W praktyce testow API to moze byc pulapka. Jesli endpoint powinien zwrocic platnosci, najpierw sprawdz:

```java
assertThat(response.content())
        .isNotEmpty()
        .allMatch(item -> "PLN".equals(item.currency()));
```

### Pulapka 4: mieszanie wielu intencji w jednym tescie

Test:

```java
assertThat(response.content())
        .isNotEmpty()
        .allMatch(item -> "PLN".equals(item.currency()))
        .allMatch(item -> item.amount() > 0)
        .anyMatch(item -> "FAILED".equals(item.status()));
```

jest zwiezly, ale jesli padnie, trzeba ustalic, ktora regule zlamano.

Dla nauki i diagnostyki SDET czesto lepsze sa oddzielne asercje.

### Pulapka 5: sprawdzanie stringow zamiast silniejszych typow

Na poczatku `String status` jest OK do nauki. W kodzie produkcyjnym albo dojrzalszych testach czesto lepiej miec enum:

```java
enum PaymentStatus {
    PAID,
    PENDING,
    FAILED
}
```

Wtedy literowka `"PAIDDD"` nie przejdzie tak latwo przez kod.

## Styl Blocha

Joshua Bloch w `Effective Java` bardzo mocno promuje czytelne typy, niemutowalnosc i API, ktore trudno uzyc zle.

W tej lekcji oznacza to:

- `PaymentDto` jako `record` jest dobry dla DTO, bo jest krotki, niemutowalny w praktyce i ma automatyczne `equals`, `hashCode`, `toString`.
- `PageResponse<T>` pokazuje typ generyczny, czyli odpowiedz moze miec `PaymentDto`, `MerchantDto`, `ErrorDto` albo inny typ.
- `List<PaymentDto>` jest lepsze niz surowe `List`, bo Java wie, jaki typ jest w srodku.
- `"PLN".equals(item.currency())` chroni test przed przypadkowym `NullPointerException`.
- Nazwany `Predicate`, np. `hasPlnCurrency`, poprawia API testu, bo czytasz regule zamiast odszyfrowywac warunek.

Bloch-style thinking dla SDET:

```text
Nie pisz tylko kodu, ktory dziala.
Pisz kod testowy, ktory za miesiac nadal bedzie jasny dla Ciebie i zespolu.
```

## Styl Subramaniama

Venkat Subramaniam czesto pokazuje, ze lambda i funkcyjny styl Javy sa dobre wtedy, gdy wyrazaja intencje, a nie gdy sa tylko ozdoba.

Lambda poprawia czytelnosc tutaj:

```java
item -> "PLN".equals(item.currency())
```

bo warunek jest krotki i naturalny.

Stream API jest ekspresyjne tutaj:

```java
List<String> actualCurrencies = response.content().stream()
        .map(PaymentDto::currency)
        .toList();
```

bo mowisz:

```text
Z platnosci wez waluty i zrob z nich liste.
```

Zwykla petla bylaby lepsza, gdy:

- warunek ma wiele krokow,
- potrzebujesz debugowania element po elemencie,
- chcesz budowac szczegolowy komunikat bledu,
- lambda staje sie za dluga.

Subramaniam-style thinking:

```text
Nie uzywaj Stream API na sile.
Uzywaj go, gdy kod staje sie bardziej wyrazisty niz petla.
```

## Styl Piotra Przybyla

Piotr Przybyl zwykle promuje nowoczesna Jave praktycznie: nie po to, zeby wygladala modnie, ale zeby kod byl krotszy, bezpieczniejszy i bardziej zrozumialy.

W tej lekcji:

- `record` skraca DTO bez pisania boilerplate.
- `List.of` daje szybkie, czytelne dane testowe.
- `Predicate` pozwala nazwac regule walidacyjna.
- `Stream.toList()` pozwala prosto zbudowac actual values.
- AssertJ pozwala pisac asercje jak zdania.

Ale sa pulapki:

- `null` nadal istnieje i moze zepsuc test.
- Za dlugie lambdy sa trudniejsze niz petle.
- Fluent API moze ukryc zlozonosc, jesli lancuch jest zbyt dlugi.
- Nowoczesna Java nie zwalnia z myslenia o expected vs actual.

Practical modern Java thinking:

```text
Uzyj nowej skladni, gdy pomaga testowi.
Nie uzywaj jej tylko dlatego, ze jest nowa.
```

## 11. Mini zadanie dla mnie

Masz taka odpowiedz:

```java
PageResponse<PaymentDto> response = new PageResponse<>(
        List.of(
                new PaymentDto("p1", "PLN", 100, "PAID"),
                new PaymentDto("p2", "EUR", 250, "PAID"),
                new PaymentDto("p3", "PLN", -50, "FAILED")
        )
);
```

Napisz asercje AssertJ, ktore sprawdza:

1. Lista nie jest pusta.
2. Przynajmniej jedna platnosc ma status `FAILED`.
3. Zadna platnosc nie ma pustego ID.
4. Wszystkie kwoty sa dodatnie.
5. Wszystkie waluty sa `PLN`.

### Pytania prowadzace

Zanim napiszesz kod, odpowiedz sobie:

```text
1. Czy warunek dotyczy wszystkich elementow, jednego elementu czy zadnego elementu?
2. Czy uzyc allMatch, anyMatch czy noneMatch?
3. Czy lambda inline bedzie czytelna?
4. Czy lepiej nazwac Predicate?
5. Czy chce zobaczyc jeden blad testu, czy kilka regulek osobno?
```

Mapa decyzji:

| Regula | Sposob myslenia | Metoda |
|---|---|---|
| Lista nie jest pusta | kolekcja jako calosc | `isNotEmpty` |
| Przynajmniej jedna platnosc ma `FAILED` | istnieje minimum jeden element | `anyMatch` |
| Zadna platnosc nie ma pustego ID | zaden element nie ma zlej cechy | `noneMatch` albo `allMatch` |
| Wszystkie kwoty sa dodatnie | kazdy element | `allMatch` |
| Wszystkie waluty sa `PLN` | kazdy element | `allMatch` |

### Rozwiazanie z lambdami inline

```java
assertThat(response.content())
        .isNotEmpty();

assertThat(response.content())
        .anyMatch(item -> "FAILED".equals(item.status()));

assertThat(response.content())
        .noneMatch(item -> item.id() == null || item.id().isBlank());

assertThat(response.content())
        .allMatch(item -> item.amount() > 0);

assertThat(response.content())
        .allMatch(item -> "PLN".equals(item.currency()));
```

Ten test celowo padnie dla podanych danych.

Dlaczego?

```text
p2 ma currency = EUR, wiec warunek "wszystkie waluty sa PLN" pada.
p3 ma amount = -50, wiec warunek "wszystkie kwoty sa dodatnie" pada.
```

To jest dobre cwiczenie SDET: test pokazuje, ze dane actual nie spelniaja expected.

### Rozwiazanie z nazwanymi `Predicate`

```java
Predicate<PaymentDto> hasFailedStatus =
        item -> "FAILED".equals(item.status());

Predicate<PaymentDto> hasBlankId =
        item -> item.id() == null || item.id().isBlank();

Predicate<PaymentDto> hasPositiveAmount =
        item -> item.amount() > 0;

Predicate<PaymentDto> hasPlnCurrency =
        item -> "PLN".equals(item.currency());

assertThat(response.content())
        .as("payments should not be empty")
        .isNotEmpty();

assertThat(response.content())
        .as("at least one payment should have FAILED status")
        .anyMatch(hasFailedStatus);

assertThat(response.content())
        .as("no payment should have blank id")
        .noneMatch(hasBlankId);

assertThat(response.content())
        .as("all payments should have positive amount")
        .allMatch(hasPositiveAmount);

assertThat(response.content())
        .as("all payments should use PLN currency")
        .allMatch(hasPlnCurrency);
```

Ta wersja jest dluzsza, ale bardziej profesjonalna, gdy reguly maja nazwy i moga byc uzyte ponownie.

### Wariant expected vs actual dla mini zadania

```java
List<String> actualCurrencies = response.content().stream()
        .map(PaymentDto::currency)
        .toList();

assertThat(actualCurrencies)
        .containsOnly("PLN");
```

```java
List<Integer> actualAmounts = response.content().stream()
        .map(PaymentDto::amount)
        .toList();

assertThat(actualAmounts)
        .allMatch(amount -> amount > 0);
```

Wersja z `extracting`:

```java
assertThat(response.content())
        .extracting(PaymentDto::currency)
        .containsOnly("PLN");

assertThat(response.content())
        .extracting(PaymentDto::status)
        .contains("FAILED");
```

## 12. Odpowiedz interview po angielsku

Short version:

```text
In API tests I use AssertJ fluent assertions to validate response DTO collections in a readable way. For example, assertThat(response.content()).allMatch(payment -> "PLN".equals(payment.currency())) verifies that every payment returned by the API has PLN currency. The lambda acts as a Predicate<PaymentDto>: it takes one DTO and returns true or false. I prefer AssertJ over plain assertTrue because it reads closer to the business rule and usually gives better failure diagnostics. When the rule is reused or business-relevant, I extract it into a named Predicate, such as hasPlnCurrency or hasPositiveAmount. For expected vs actual validation, I often extract fields with Stream API or AssertJ extracting, for example extracting(PaymentDto::currency).containsOnly("PLN").
```

Longer SDET version:

```text
When validating REST API responses, I separate the actual data returned by the system from the expected contract. If the endpoint returns a PageResponse<PaymentDto>, response.content() gives me a List<PaymentDto>. Since List is iterable, AssertJ can apply collection assertions to every element. allMatch is useful when every item must satisfy a rule, anyMatch when at least one item must satisfy it, and noneMatch when no item should satisfy a negative condition. Lambdas such as payment -> "PLN".equals(payment.currency()) are concise implementations of Predicate<PaymentDto>. I use inline lambdas for simple one-off checks and named Predicates for reusable or business-readable validation rules. I also use extracting when I want to compare a specific field, such as all returned currencies, against expected values. This keeps API tests readable, diagnostic, and aligned with expected vs actual thinking.
```

## 13. Sciezka nauki dla SDET JDK 25

Ta lekcja ma nauczyc Cie nie tylko jednej metody AssertJ. Celem jest zbudowanie sposobu myslenia, ktory przyda sie w testach REST API, testach integracyjnych, code review i rozmowie technicznej.

Po tej lekcji powinienes umiec:

- wyjasnic, czym jest lambda w Javie,
- wyjasnic, czym jest `Predicate<T>`,
- uzyc `allMatch`, `anyMatch` i `noneMatch` w AssertJ,
- zdecydowac, kiedy uzyc lambdy inline, a kiedy nazwanego predykatu,
- rozpoznac pulapke `allMatch` na pustej liscie,
- walidowac kolekcje DTO z odpowiedzi API,
- oddzielic `actual` od `expected`,
- dobrac asercje tak, zeby blad w CI byl diagnostyczny,
- uzyc rekordow JDK 25 jako prostych DTO w testach,
- mowic o tym po angielsku na rozmowie SDET.

### Modul 1: DTO jako `record`

Temat:

```java
record PaymentDto(String id, String currency, int amount, String status) {
}
```

Co masz zrozumiec:

- `record` jest dobry do prostych, niemutowalnych modeli danych.
- Rekord automatycznie daje konstruktor, accessors, `equals`, `hashCode` i `toString`.
- W testach API rekord dobrze reprezentuje odpowiedz JSON po deserializacji.

Ryzyko SDET:

- Jesli DTO jest zbyt luzne, np. wszystko jako `Map<String, Object>`, test traci typowanie.
- Jesli DTO ma publiczne mutable pola, testy moga przypadkowo zmieniac dane actual.

Mini cwiczenie:

```java
record MerchantDto(String id, String name, boolean active) {
}
```

Napisz liste trzech merchantow i asercje, ktora sprawdza, ze wszyscy sa aktywni.

### Modul 2: Lambda jako mala regule testowa

Temat:

```java
payment -> "PLN".equals(payment.currency())
```

Co masz zrozumiec:

- Lambda to funkcja przekazana jako wartosc.
- Lewa strona przyjmuje argument.
- Prawa strona zwraca wynik.
- W `allMatch` wynik musi byc `boolean`.

Mentalny model:

```text
Wez jeden element listy.
Sprawdz go.
Zwroc true albo false.
Powtorz dla kazdego elementu.
```

Mini cwiczenie:

```java
item -> item.amount() > 0
```

Przepisz te lambde jako zwykla metode statyczna `hasPositiveAmount`.

### Modul 3: `Predicate<T>` jako nazwany warunek

Temat:

```java
Predicate<PaymentDto> hasPlnCurrency =
        payment -> "PLN".equals(payment.currency());
```

Co masz zrozumiec:

- `Predicate<T>` bierze `T` i zwraca `boolean`.
- `Predicate<PaymentDto>` to regule dla pojedynczego `PaymentDto`.
- Predykaty mozna laczyc przez `.and(...)`, `.or(...)`, `.negate()`.

Przyklad laczenia:

```java
Predicate<PaymentDto> hasPlnCurrency =
        payment -> "PLN".equals(payment.currency());

Predicate<PaymentDto> hasPositiveAmount =
        payment -> payment.amount() > 0;

Predicate<PaymentDto> isValidPaymentForListResponse =
        hasPlnCurrency.and(hasPositiveAmount);
```

SDET naming rule:

```text
Nazwa predykatu powinna mowic, jaka regule kontraktu API sprawdza.
```

Lepsze nazwy:

- `hasPlnCurrency`
- `hasPositiveAmount`
- `hasNonBlankId`
- `hasAllowedStatus`
- `belongsToRequestedMerchant`

Slabsze nazwy:

- `check`
- `condition`
- `predicate1`
- `testPayment`

### Modul 4: `allMatch`, `anyMatch`, `noneMatch`

Najwazniejsza roznica:

| AssertJ method | Pytanie | Typowy test API |
|---|---|---|
| `allMatch` | Czy wszystkie elementy spelniaja warunek? | Wszystkie elementy maja `currency = PLN` |
| `anyMatch` | Czy przynajmniej jeden element spelnia warunek? | Jest przynajmniej jeden element `PENDING` |
| `noneMatch` | Czy zaden element nie spelnia warunku? | Nie ma elementow z ujemna kwota |

Regula praktyczna:

```text
Jesli myslisz "kazdy", uzyj allMatch.
Jesli myslisz "istnieje", uzyj anyMatch.
Jesli myslisz "zaden", uzyj noneMatch.
```

### Modul 5: `extracting` vs `allMatch`

Obie wersje sa poprawne, ale ucza innego stylu myslenia.

`allMatch`:

```java
assertThat(response.content())
        .allMatch(payment -> "PLN".equals(payment.currency()));
```

Czytanie:

```text
Kazdy obiekt payment spelnia warunek waluty.
```

`extracting`:

```java
assertThat(response.content())
        .extracting(PaymentDto::currency)
        .containsOnly("PLN");
```

Czytanie:

```text
Wyciagam wszystkie waluty i sprawdzam zbior wartosci.
```

Kiedy wybrac `allMatch`:

- warunek laczy kilka pol,
- warunek jest nazwany jako `Predicate`,
- regule chcesz ponownie wykorzystac.

Kiedy wybrac `extracting`:

- sprawdzasz jedno pole,
- chcesz jasniej pokazac `actual` values,
- komunikat bledu ma pokazac liste wartosci pola.

## 14. Pelny przyklad JUnit z 5 testami AssertJ

Ten przyklad mozesz potraktowac jako material do przepisania w IDE. Kod jest samowystarczalny edukacyjnie.

```java
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentApiResponsePredicateTest {

    record PaymentDto(String id, String currency, int amount, String status) {
    }

    record PageResponse<T>(List<T> content) {
        PageResponse {
            content = List.copyOf(content);
        }
    }

    private static final Predicate<PaymentDto> HAS_PLN_CURRENCY =
            payment -> "PLN".equals(payment.currency());

    private static final Predicate<PaymentDto> HAS_POSITIVE_AMOUNT =
            payment -> payment.amount() > 0;

    private static final Predicate<PaymentDto> HAS_NON_BLANK_ID =
            payment -> payment.id() != null && !payment.id().isBlank();

    private static final Predicate<PaymentDto> HAS_ALLOWED_STATUS =
            payment -> Set.of("PAID", "PENDING", "FAILED").contains(payment.status());

    @Test
    void shouldReturnOnlyPlnPayments() {
        PageResponse<PaymentDto> response = successfulResponse();

        assertThat(response.content())
                .isNotEmpty()
                .allMatch(HAS_PLN_CURRENCY);
    }

    @Test
    void shouldReturnOnlyPositiveAmounts() {
        PageResponse<PaymentDto> response = successfulResponse();

        assertThat(response.content())
                .allMatch(HAS_POSITIVE_AMOUNT);
    }

    @Test
    void shouldReturnOnlyPaymentsWithNonBlankIds() {
        PageResponse<PaymentDto> response = successfulResponse();

        assertThat(response.content())
                .allMatch(HAS_NON_BLANK_ID);
    }

    @Test
    void shouldReturnOnlyAllowedStatuses() {
        PageResponse<PaymentDto> response = successfulResponse();

        assertThat(response.content())
                .allMatch(HAS_ALLOWED_STATUS);
    }

    @Test
    void shouldExposeActualCurrenciesClearly() {
        PageResponse<PaymentDto> response = successfulResponse();

        assertThat(response.content())
                .extracting(PaymentDto::currency)
                .containsOnly("PLN");
    }

    private static PageResponse<PaymentDto> successfulResponse() {
        return new PageResponse<>(List.of(
                new PaymentDto("p1", "PLN", 100, "PAID"),
                new PaymentDto("p2", "PLN", 250, "PAID"),
                new PaymentDto("p3", "PLN", 300, "PENDING")
        ));
    }
}
```

### Co tu jest wazne dla JDK 25

- `record` daje krotki model DTO bez boilerplate.
- Kompaktowy konstruktor rekordu `PageResponse` robi defensive copy przez `List.copyOf`.
- `Predicate<T>` pochodzi z `java.util.function` i jest standardowym interfejsem funkcyjnym.
- Lambdy implementuja interfejs funkcyjny bez anonimowych klas.
- `List.of` tworzy niemutowalna liste test data.
- `Set.of` tworzy maly, czytelny zbior do walidacji dozwolonych statusow.
- Method reference `PaymentDto::currency` czytelnie wskazuje pole wyciagane przez AssertJ.

### Dlaczego `List.copyOf` w rekordzie

Bez defensive copy:

```java
record PageResponse<T>(List<T> content) {
}
```

Jesli ktos przekaze mutable liste, moglby ja pozniej zmienic poza rekordem. W testach to ryzyko ukrytych zmian danych actual.

Z defensive copy:

```java
record PageResponse<T>(List<T> content) {
    PageResponse {
        content = List.copyOf(content);
    }
}
```

Rekord zachowuje wlasna niemutowalna kopie listy. To jest zgodne z mysleniem Effective Java: minimalizuj mutowalnosc i chron dane wewnetrzne.

## 15. Cwiczenia praktyczne

Nie traktuj tych zadan jak quizu. Traktuj je jak trening pisania testow, ktore po miesiacu nadal beda czytelne.

### Cwiczenie 1: Beginner - pierwsze `allMatch`

Cel:

```text
Nauczyc sie sprawdzac, ze kazdy element listy spelnia jeden prosty warunek.
```

Kod startowy:

```java
record PaymentDto(String id, String currency, int amount, String status) {
}

List<PaymentDto> payments = List.of(
        new PaymentDto("p1", "PLN", 100, "PAID"),
        new PaymentDto("p2", "PLN", 200, "PENDING")
);
```

Zadania:

- Napisz asercje, ze wszystkie platnosci maja walute `PLN`.
- Napisz asercje, ze wszystkie kwoty sa wieksze od `0`.
- Napisz asercje, ze zadne `id` nie jest puste.

Przed kodowaniem zapytaj siebie:

- Czy warunek dotyczy kazdego elementu?
- Czy lambda jest na tyle krotka, ze moze zostac inline?
- Czy potrzebuje `isNotEmpty` przed `allMatch`?

Expected learning outcome:

```text
Umiesz uzyc allMatch do prostych regul kontraktu API.
```

### Cwiczenie 2: Intermediate - nazwane `Predicate`

Cel:

```text
Nauczyc sie zamieniac techniczne lambdy w nazwane reguly testowe.
```

Kod startowy:

```java
Predicate<PaymentDto> hasPlnCurrency =
        payment -> "PLN".equals(payment.currency());
```

Zadania:

- Dodaj `hasPositiveAmount`.
- Dodaj `hasNonBlankId`.
- Dodaj `hasAllowedStatus` dla statusow `PAID`, `PENDING`, `FAILED`.
- Polacz `hasPlnCurrency` i `hasPositiveAmount` w `isValidListedPayment`.
- Uzyj `isValidListedPayment` w `assertThat(payments).allMatch(...)`.

Przed kodowaniem zapytaj siebie:

- Czy nazwa predykatu brzmi jak regula biznesowa albo kontraktowa?
- Czy polaczenie predykatow poprawia czytelnosc?
- Czy za duzo logiki nie ukrywa sie w jednym predykacie?

Expected learning outcome:

```text
Umiesz nazwac i ponownie wykorzystac reguly walidacji odpowiedzi API.
```

### Cwiczenie 3: Intermediate - `extracting` i actual values

Cel:

```text
Nauczyc sie wyciagac pola z DTO i porownywac actual values z expected values.
```

Zadania:

- Uzyj `.extracting(PaymentDto::currency).containsOnly("PLN")`.
- Uzyj `.extracting(PaymentDto::status).contains("PENDING")`.
- Uzyj `.extracting(PaymentDto::id).doesNotContainNull()`.
- Porownaj diagnostyke bledu z wersja `.allMatch(...)`.

Przed kodowaniem zapytaj siebie:

- Czy chce sprawdzic obiekt jako calosc, czy jedno pole?
- Czy failure message powinien pokazac liste wartosci pola?
- Czy `containsOnly` jest wystarczajace, czy potrzebuje `containsExactly`?

Expected learning outcome:

```text
Umiesz dobrac AssertJ extracting, gdy test ma jasno pokazac actual field values.
```

### Cwiczenie 4: Senior QA Automation - kontrakt odpowiedzi listy

Cel:

```text
Zaprojektowac zestaw asercji dla odpowiedzi list endpointu bez mieszania zbyt wielu intencji.
```

Scenariusz:

```text
GET /payment-orders zwraca liste payment orders widocznych dla merchant user.
```

Zadania:

- Sprawdz, ze lista nie jest pusta, jesli test data gwarantuje istniejace rekordy.
- Sprawdz, ze wszystkie elementy maja niepuste `id`.
- Sprawdz, ze wszystkie elementy maja dozwolony `status`.
- Sprawdz, ze wszystkie elementy naleza do oczekiwanego merchant context, jesli DTO to zawiera.
- Sprawdz, ze zadna wartosc wrazliwa, np. token albo raw card data, nie jest obecna w DTO.

Przed kodowaniem zapytaj siebie:

- Jaki jest kontrakt endpointu?
- Czy test data naprawde gwarantuje niepusta liste?
- Czy sortowanie jest czescia kontraktu, czy przypadkiem implementacji?
- Czy asercje sa diagnostyczne w CI?
- Czy test nie sprawdza przyszlej funkcjonalnosci, ktora nie jest jeszcze w scope?

Expected learning outcome:

```text
Umiesz projektowac asercje odpowiedzi API jako kontrakt, a nie tylko jako sprawdzenie statusu 200.
```

### Cwiczenie 5: REST Assured version

Cel:

```text
Polaczyc deserializacje odpowiedzi REST Assured z AssertJ collection assertions.
```

Szkic kodu:

```java
PageResponse<PaymentDto> response = given()
        .auth().oauth2(token)
        .when()
        .get("/payment-orders")
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<PageResponse<PaymentDto>>() {
        });

assertThat(response.content())
        .isNotEmpty()
        .allMatch(payment -> "PLN".equals(payment.currency()));
```

Zadania:

- Utrzymaj `statusCode(200)` jako osobna walidacje transportu HTTP.
- Utrzymaj AssertJ jako walidacje semantyki body.
- Dodaj predykat `hasAllowedStatus`.
- Dodaj `extracting(PaymentDto::currency).containsOnly("PLN")` i porownaj czytelnosc z `allMatch`.

Uwaga:

```text
Nie loguj tokenow ani naglowka Authorization w tescie.
```

Expected learning outcome:

```text
Umiesz rozdzielic REST Assured response extraction od AssertJ business assertions.
```

### Cwiczenie 6: Mockito version

Cel:

```text
Zobaczyc, ze Predicate i lambda sa tez przydatne przy weryfikacji argumentow przekazanych do zaleznosci.
```

Szkic:

```java
verify(repository).save(argThat(payment ->
        "PLN".equals(payment.currency()) && payment.amount() > 0
));
```

Zadania:

- Najpierw napisz wersje z `argThat` dla bardzo krotkiego warunku.
- Potem przepisz warunek do nazwanej metody `isValidSavedPayment`.
- Jesli warunek ma wiecej niz 2-3 czesci, uzyj `ArgumentCaptor` i AssertJ zamiast dlugiego `argThat`.

Lepszy wariant dla diagnostyki:

```java
ArgumentCaptor<PaymentDto> captor = ArgumentCaptor.forClass(PaymentDto.class);

verify(repository).save(captor.capture());

assertThat(captor.getValue().currency())
        .isEqualTo("PLN");

assertThat(captor.getValue().amount())
        .isPositive();
```

Expected learning outcome:

```text
Umiesz zdecydowac, kiedy lambda w Mockito jest czytelna, a kiedy AssertJ po captorze daje lepsza diagnostyke.
```

### Cwiczenie 7: Edge cases

Cel:

```text
Nauczyc sie myslec o danych, ktore psuja pozornie poprawne asercje.
```

Przygotuj test data dla przypadkow:

- pusta lista,
- `currency = null`,
- `id = ""`,
- `id = "   "`,
- `amount = 0`,
- `amount = -1`,
- `status = "UNKNOWN"`,
- lista z mieszanymi walutami `PLN` i `EUR`.

Dla kazdego przypadku zdecyduj:

- czy test powinien przejsc,
- ktora asercja powinna go zlapac,
- jaki komunikat bledu bylby najbardziej pomocny.

Expected learning outcome:

```text
Umiesz projektowac negatywne przypadki danych dla walidacji kolekcji DTO.
```

## 16. Zadanie kontrolne bez gotowego rozwiazania

Zbuduj klase testowa `PaymentOrderListContractTest`.

Wymagania:

- Uzyj `record PaymentOrderDto(String id, String merchantId, String currency, int amount, String status)`.
- Uzyj `record PageResponse<T>(List<T> content)` z defensive copy przez `List.copyOf`.
- Przygotuj metode `sampleResponse()` zwracajaca minimum 3 elementy.
- Napisz 5 osobnych testow AssertJ.
- Minimum 2 testy maja uzywac `allMatch`.
- Minimum 1 test ma uzywac `anyMatch`.
- Minimum 1 test ma uzywac `noneMatch`.
- Minimum 1 test ma uzywac `extracting`.
- Minimum 2 reguly maja byc nazwane jako `Predicate<PaymentOrderDto>`.

Testy do napisania:

- `shouldReturnOnlyOrdersForExpectedMerchant`
- `shouldReturnOnlyOrdersWithNonBlankIds`
- `shouldReturnOnlyPositiveAmounts`
- `shouldContainAtLeastOnePendingOrder`
- `shouldNotExposeUnsupportedStatuses`

Before coding checklist:

- Czy test data pasuje do nazw testow?
- Czy predykaty maja nazwy w stylu `has...`, `is...`, `belongsTo...`?
- Czy `allMatch` nie ukrywa pustej listy?
- Czy asercje sa oddzielone tam, gdzie diagnostyka jest wazniejsza niz zwiezlosc?
- Czy statusy jako `String` sa swiadomym uproszczeniem do lekcji?

## 17. Rubryka samooceny

Ocen swoje rozwiazanie w skali 0-2 dla kazdego punktu.

| Kryterium | 0 | 1 | 2 |
|---|---|---|---|
| Czytelnosc lambd | lambdy sa dlugie i trudne | czesc jest czytelna | kazda lambda ma jasna intencje |
| Nazwy predykatow | techniczne albo losowe | czesciowo dobre | brzmia jak reguly kontraktu |
| Diagnostyka AssertJ | failure bedzie niejasny | czesciowo pomocny | wiadomo, ktora regula padla |
| Edge cases | brak | podstawowe | pusta lista, null, blank, invalid status |
| JDK 25 style | mutable albo raw types | czesciowo typed | record, generics, immutable test data |
| SDET thinking | sprawdza tylko happy path | sprawdza kilka warunkow | sprawdza kontrakt i ryzyka API |

Interpretacja:

- 0-4: wroc do modulow 1-4.
- 5-8: napisz jeszcze raz cwiczenie 4 z lepszymi nazwami.
- 9-12: przejdz do wersji REST Assured albo Mockito.

## 18. Najczestsze pytania rekrutacyjne

### What is a Predicate in Java?

```text
A Predicate<T> is a standard functional interface that takes one value of type T and returns a boolean. In API tests I use it to express reusable validation rules, for example hasPlnCurrency or hasPositiveAmount.
```

### Why use AssertJ allMatch instead of assertTrue with streams?

```text
AssertJ allMatch keeps the assertion close to the actual collection and reads like a test rule. It is usually more expressive than assertTrue(response.content().stream().allMatch(...)), and it fits well with other fluent collection assertions.
```

### What is the risk of allMatch on an empty list?

```text
allMatch is true for an empty collection because no element violates the predicate. In API tests this can hide a bug if the endpoint was expected to return data, so I usually add isNotEmpty when non-empty content is part of the scenario.
```

### When would you extract a Predicate?

```text
I extract a Predicate when the rule has business meaning, is reused, or makes the assertion easier to read. For a very short one-off condition, an inline lambda is fine.
```

### When is extracting better than allMatch?

```text
extracting is better when I want to validate one field across all DTOs and see the actual field values in the assertion output. allMatch is better when the condition uses the whole object or combines multiple fields.
```

## 19. Nastepna lekcja

Naturalna kontynuacja tej lekcji:

```text
Lesson 2: AssertJ extracting, tuples, containsExactly and order-sensitive API contract validation
```

Tematy:

- `extracting` jednego pola,
- `extracting` wielu pol przez `tuple`,
- `containsExactly` vs `containsExactlyInAnyOrder`,
- sortowanie jako kontrakt API,
- diagnostyka expected vs actual,
- testy list endpointow w REST Assured.
