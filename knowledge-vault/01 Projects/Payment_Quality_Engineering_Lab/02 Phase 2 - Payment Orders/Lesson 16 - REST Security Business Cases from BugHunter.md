# Lesson 16 - REST Security Business Cases from BugHunter

## Co bierzemy z Claude-BugHunter

Claude-BugHunter jest publicznym repozytorium zorientowanym na bug hunting i red-team. W tym projekcie nie kopiujemy tego stylu pracy. Bierzemy tylko wartość edukacyjną: listę klas ryzyka, które często pojawiają się w API.

Dla naszego Payment Quality Engineering Lab najcenniejsze są tematy:

- API misconfiguration: za szerokie DTO, nieznane pola JSON, przypadkowe wystawienie danych.
- BOLA / IDOR: merchant A nie może czytać ani zmieniać zamówień merchant B.
- BFLA: rola czytająca nie może wykonywać capture/refund/cancel.
- CORS: browser musi umieć zrobić preflight, ale preflight nie jest autoryzacją biznesową.
- HTTP methods: `HEAD`, `OPTIONS`, `PATCH`, `405 Allow`, `Accept-Patch`.
- Headers: `ETag`, `If-Match`, `Idempotency-Key`, `Cache-Control`, `Vary`, `X-Correlation-ID`.
- JWT/OAuth negative cases: expired token, brak roli, brak `merchant_id`, błędny issuer/audience jako przyszły temat.
- Evidence hygiene: jak raportować błąd bez wycieku tokenów, cookies i prywatnych danych.

W praktyce oznacza to: zamieniamy "jak znaleźć podatność" na "jak zaprojektować i przetestować defensywny kontrakt REST".

## Czego nie bierzemy

Nie bierzemy:

- skanerów,
- payload bibliotek,
- instrukcji exploitacji,
- automatyzacji bug bounty,
- zewnętrznego reconu,
- red-team workflow,
- omijania zabezpieczeń,
- ataków na realne cele.

Nie dodajemy też do tego projektu:

- mikroserwisów,
- Kafki,
- prawdziwego PSP,
- webhooków,
- settlement/payout/reconciliation,
- rate limitingu jako implementacji w tej lekcji,
- testów Rest Assured w tej fazie.

Ta lekcja jest mapą przyszłych przypadków. Implementacja i testy będą osobnymi krokami.

## Najważniejsze pojęcia

**Mass assignment** oznacza sytuację, w której klient może wysłać pola, których nie powinien kontrolować, np. `status`, `merchantId`, `capturedAmountMinor`, `version`. W naszym projekcie szczególnie ważny jest `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`, bo metadata PATCH nie może zmienić statusu płatności.

**BOLA / IDOR** oznacza dostęp do cudzego obiektu przez manipulację identyfikatorem w ścieżce. W naszym projekcie podstawowy przykład to Merchant A używa tokena z `merchant_id=A`, ale woła endpoint Merchant B.

**BFLA** oznacza brak poprawnej kontroli na poziomie funkcji. Przykład: `merchant:payments:read` pozwala czytać, ale nie może wykonywać `POST /capture`.

**CORS preflight** to zapytanie `OPTIONS` wysyłane przez browser przed właściwym requestem. Preflight może przejść bez bearer tokena, ale właściwy `POST`, `PATCH`, `GET` albo `HEAD` nadal musi wymagać auth.

**ETag / If-Match** chroni przed lost update. Klient czyta `ETag: "v3"` i mutuje tylko z `If-Match: "v3"`. Jeżeli wersja się zmieniła, backend zwraca `412 Precondition Failed`.

**Idempotency-Key** chroni przed duplikatem po retry. Ten sam klucz i ten sam payload powinny zwrócić ten sam wynik. Ten sam klucz i inny payload powinny dać `409 Conflict`.

**Cache-Control: no-store** jest ważne, bo payment detail/list/history zawiera dane wrażliwe biznesowo. Nawet `404` maskujący cudzy zasób nie powinien być cache'owany.

**application/problem+json** daje maszynowo czytelne błędy: `type`, `title`, `status`, `detail`, `code`, `correlationId`.

**X-Correlation-ID** pomaga połączyć błąd w teście, odpowiedź HTTP, log backendu i historię statusu.

## Lista przypadków do przyszłej implementacji

Najważniejsze przyszłe przypadki:

- `A1`: metadata PATCH odrzuca mass assignment poza `metadata`.
- `A2`: create payment order ma jasną politykę nieznanych pól JSON.
- `A3`: response DTO nie wystawia pól wewnętrznych.
- `B1`: Merchant A nie czyta payment detail Merchant B.
- `B2`: Merchant A nie mutuje payment lifecycle Merchant B.
- `B3`: `HEAD` nie wycieka istnienia cudzego payment order.
- `C1`: merchant reader nie może wykonać capture.
- `C2`: auditor czyta history, ale nie mutuje.
- `D1`: CORS preflight dopuszcza `Idempotency-Key`.
- `D2`: CORS preflight dopuszcza `If-Match`.
- `D4`: browser może odczytać `ETag`, `Location`, `X-Correlation-ID`, `Allow`, `Accept-Patch`.
- `E1`: `X-HTTP-Method-Override` jest ignorowany albo odrzucany.
- `E2`: `TRACE` nie echo'uje request headers.
- `E3`: `405 Method Not Allowed` zawiera `Allow`.
- `F1`: `Host` nie wpływa na `Location`.
- `F2`: `X-Forwarded-Host` nie jest ufany bez zaufanego proxy.
- `G1`: payment detail ma `Cache-Control: no-store`.
- `G4`: błędy `application/problem+json` mają `Cache-Control: no-store`.
- `H2`: ten sam `Idempotency-Key` i inny payload daje `409`.
- `H4`: stale `If-Match` przy capture daje `412`.
- `H5`: double capture race ma tylko jedną zwycięską mutację.
- `I1`: expired token daje `401` i `WWW-Authenticate`.
- `I3`: `merchant_id` w tokenie niezgodny ze ścieżką nie daje dostępu.
- `J1`: Actuator ma jawnie opisaną politykę ekspozycji.
- `J2`: OpenAPI/Swagger ma jawnie opisaną politykę ekspozycji.
- `K1`: logi Rest Assured redagują `Authorization` i `Cookie`.
- `K2`: raport błędu zawiera `X-Correlation-ID`.

Pełna lista znajduje się w:

`specs/012-rest-security-business-cases-from-bughunter/contracts/rest-security-learning-cases.md`

## Jak to potem testować Rest Assured

Nie piszemy testów w tej fazie, bo test powinien weryfikować istniejące, uzgodnione zachowanie. Gdybyśmy teraz napisali testy do niezaimplementowanych kontraktów, powstałaby mieszanina życzeń, założeń i realnej specyfikacji.

Późniejsze lekcje Rest Assured powinny ćwiczyć:

- budowanie tokenów testowych z różnymi rolami i `merchant_id`,
- przygotowanie dwóch merchantów i payment order należącego do jednego z nich,
- asercje statusów `401`, `403`, `404`, `405`, `409`, `412`, `415`, `422`, `428`,
- asercje headerów: `ETag`, `If-Match`, `Idempotency-Key`, `Cache-Control`, `Vary`, `Allow`, `Accept-Patch`, `WWW-Authenticate`, `X-Correlation-ID`,
- asercje `application/problem+json`,
- sprawdzanie braku pól w JSON,
- sprawdzanie braku mutacji w bazie przy `403`, `404`, `409`, `412`, `422`,
- CORS preflight bez bearer tokena,
- rozróżnienie preflight od właściwego requestu biznesowego,
- bezpieczne logowanie request/response tylko po nieudanej walidacji,
- redakcję tokenów i cookies w logach.

W modularnym monolicie każdy test powinien umieć wskazać, który moduł i która granica są weryfikowane:

- `shared.security`: role, JWT, CORS, route authorization.
- `shared.web`: correlation ID i ewentualne filtry nagłówków.
- `payment.internal.web`: kontroler, DTO, problem details, HTTP headers.
- `payment.internal.application`: idempotency, precondition, lifecycle service.
- `payment.internal.domain`: state machine i reguły lifecycle.
- `payment.internal.infrastructure`: repozytoria, Flyway schema, unikalność i optimistic locking.

## Pytania rekrutacyjne PL/EN

**PL Q:** Dlaczego mass assignment jest ryzykowny w metadata PATCH?
**PL A:** Bo klient może wysłać pola, których nie powinien kontrolować, np. `status`, `merchantId` albo kwoty. Metadata PATCH powinien modyfikować tylko `metadata`, a pola domenowe muszą być zmieniane przez reguły lifecycle.
**EN:** Mass assignment is risky because a client may set server-owned fields such as status, merchantId, or amounts. Metadata PATCH should only update metadata; domain fields must be changed through lifecycle rules.

**PL Q:** Dlaczego read DTO i write DTO powinny być osobne?
**PL A:** Response może zawierać wiele faktów biznesowych, ale request powinien przyjmować tylko komendę użytkownika. Oddzielenie DTO zmniejsza ryzyko przypadkowego dopuszczenia pól tylko do odczytu.
**EN:** Read and write DTOs should be separate because responses may expose many facts, while requests should accept only the user's command. This reduces accidental write access to read-only fields.

**PL Q:** Dlaczego merchant tenant isolation musi być testowany?
**PL A:** Payment order jest zasobem merchant-scoped. Jeżeli token Merchant A może odczytać lub zmienić order Merchant B, mamy BOLA/IDOR i realny problem bezpieczeństwa biznesowego.
**EN:** Merchant tenant isolation must be tested because payment orders are merchant-scoped resources. If Merchant A can read or mutate Merchant B's order, it is a BOLA/IDOR business security issue.

**PL Q:** Dlaczego `HEAD` może wyciekać istnienie zasobu?
**PL A:** `HEAD` nie zwraca body, ale status i nagłówki mogą zdradzić, czy zasób istnieje. Dlatego powinien używać tej samej logiki ownership co `GET`.
**EN:** `HEAD` can leak existence through status and headers even without a body. It should use the same ownership rules as `GET`.

**PL Q:** Dlaczego masked `404` powinien mieć `Cache-Control: no-store`?
**PL A:** Bo odpowiedź zależy od tożsamości i uprawnień. Cache nie może zapamiętać wyniku dla jednego użytkownika i pokazać go innemu.
**EN:** A masked `404` should use `Cache-Control: no-store` because the response depends on identity and authorization. A cache must not reuse it across users.

**PL Q:** Kiedy użyć `403`, a kiedy masked `404`?
**PL A:** `403` mówi "wiem, że request jest zabroniony"; masked `404` ukrywa istnienie zasobu. W payment tenant isolation trzeba jawnie ustalić politykę per endpoint.
**EN:** `403` says the request is forbidden; masked `404` hides whether the resource exists. Payment tenant isolation needs an explicit per-endpoint policy.

**PL Q:** Dlaczego merchant reader nie może wykonać capture?
**PL A:** Capture zmienia stan i może reprezentować ruch finansowy. Rola `merchant:payments:read` ma tylko widzieć dane, nie wykonywać operacje lifecycle.
**EN:** Capture changes state and may represent a financial action. A `merchant:payments:read` role should only read data, not perform lifecycle operations.

**PL Q:** Dlaczego auditor powinien czytać history, ale nie mutować payment order?
**PL A:** Auditor potrzebuje szerokiej widoczności do wyjaśniania zdarzeń, ale zasada least privilege nie pozwala mu zmieniać stanu płatności.
**EN:** An auditor needs broad visibility to explain events, but least privilege means they must not change payment state.

**PL Q:** Dlaczego CORS preflight nie powinien wymagać bearer tokena?
**PL A:** Browser wysyła preflight przed właściwym requestem i zwykle bez tokena. Auth musi dotyczyć właściwego `GET`, `POST`, `PATCH` albo `HEAD`, nie samego preflight.
**EN:** CORS preflight is sent by the browser before the actual request and usually without a token. Authentication belongs to the actual business request, not the preflight.

**PL Q:** Dlaczego `Access-Control-Expose-Headers` jest ważny dla `ETag`?
**PL A:** Backend może zwrócić `ETag`, ale browser JavaScript nie odczyta go bez expose headers. Bez tego frontend nie wyśle poprawnego `If-Match`.
**EN:** The backend may return an `ETag`, but browser JavaScript cannot read it unless it is exposed. Without it, the frontend cannot send a correct `If-Match`.

**PL Q:** Dlaczego `Access-Control-Expose-Headers` jest ważny dla `X-Correlation-ID`?
**PL A:** UI i tester muszą zobaczyć correlation ID, żeby połączyć błąd z logami backendu. Bez expose headers browser może ukryć ten nagłówek przed aplikacją.
**EN:** The UI and tester need the correlation ID to connect a failure with backend logs. Without exposed headers, the browser may hide it from the app.

**PL Q:** Dlaczego `X-HTTP-Method-Override` powinien być zwykle odrzucony?
**PL A:** Może zmienić semantykę requestu i ominąć route-specific security, jeżeli aplikacja lub proxy interpretuje go inaczej niż backend.
**EN:** `X-HTTP-Method-Override` can change request semantics and bypass route-specific security if proxies or the app interpret it differently.

**PL Q:** Co powinien zawierać `405 Method Not Allowed`?
**PL A:** Powinien zawierać `Allow`, żeby klient wiedział, jakie metody są obsługiwane dla zasobu, np. `GET, HEAD, PATCH, OPTIONS`.
**EN:** `405 Method Not Allowed` should include `Allow` so clients know which methods are supported for the resource.

**PL Q:** Dlaczego `PATCH` powinien wymagać właściwego `Content-Type`?
**PL A:** Bo semantyka merge patch różni się od dowolnego JSON-a. `Content-Type` mówi backendowi, jak interpretować body.
**EN:** `PATCH` should require the correct `Content-Type` because merge patch semantics differ from arbitrary JSON. The header tells the backend how to interpret the body.

**PL Q:** Dlaczego `Host` nie powinien wpływać na `Location`?
**PL A:** Nieufany `Host` może zatruć linki w odpowiedzi. Bezpieczniej zwracać relative `Location` albo używać skonfigurowanego trusted base URL.
**EN:** An untrusted `Host` header can poison response links. It is safer to return a relative `Location` or use a configured trusted base URL.

**PL Q:** Dlaczego `X-Forwarded-Host` jest granicą zaufania?
**PL A:** Ten nagłówek ma sens od zaufanego reverse proxy, ale bezpośredni klient może go sfałszować. Backend musi wiedzieć, kiedy go ignoruje, a kiedy ufa proxy.
**EN:** `X-Forwarded-Host` only makes sense from a trusted reverse proxy. A direct client can forge it, so the backend must define when it ignores or trusts it.

**PL Q:** Dlaczego `Vary: Authorization` jest ważne?
**PL A:** Ten sam URL może dać inną odpowiedź dla różnych tokenów. `Vary: Authorization` mówi cache, że authorization wpływa na reprezentację.
**EN:** The same URL can return different responses for different tokens. `Vary: Authorization` tells caches that authorization affects the representation.

**PL Q:** Czym różni się `409 Conflict` od `412 Precondition Failed`?
**PL A:** `409` pasuje do konfliktu idempotency key albo innego konfliktu zasobu. `412` oznacza, że `If-Match` był składniowo poprawny, ale nie pasuje do aktualnej wersji zasobu.
**EN:** `409` fits an idempotency-key or resource conflict. `412` means `If-Match` was syntactically valid but did not match the current resource version.

**PL Q:** Dlaczego idempotency key musi mieć scope?
**PL A:** Ten sam klucz może być bezpieczny tylko dla tej samej intencji biznesowej. Scope powinien uwzględniać merchant, payment order, akcję i fingerprint payloadu.
**EN:** An idempotency key is safe only for the same business intent. Scope should include merchant, payment order, action, and payload fingerprint.

**PL Q:** Dlaczego double capture race jest ważnym przypadkiem?
**PL A:** Dwa równoległe requesty mogą próbować przechwycić tę samą autoryzację. System musi dopuścić tylko jedną zwycięską mutację.
**EN:** A double capture race matters because two concurrent requests may try to capture the same authorization. The system must allow only one winning mutation.

**PL Q:** Dlaczego expired token powinien zwrócić `401`, a nie `403`?
**PL A:** Expired token to problem uwierzytelnienia, nie braku roli. `401` sygnalizuje, że klient musi dostarczyć ważne credentials.
**EN:** An expired token is an authentication problem, not a missing-role problem. `401` signals that the client must provide valid credentials.

**PL Q:** Dlaczego OpenAPI exposure może być tematem security testing?
**PL A:** Dokumentacja może pokazać endpointy, modele i pola, których nie chcieliśmy wystawić publicznie. Trzeba testować politykę ekspozycji per profil.
**EN:** OpenAPI exposure is a security testing topic because docs may reveal endpoints, models, and fields that were not intended to be public. Exposure policy should be tested per profile.

**PL Q:** Dlaczego Actuator exposure trzeba planować przed dodaniem dependency?
**PL A:** Actuator może wystawić env, mappings, metrics albo inne dane diagnostyczne. Bez jawnej allow-listy łatwo przypadkowo ujawnić za dużo.
**EN:** Actuator exposure must be planned before adding the dependency because it can expose env, mappings, metrics, or diagnostics. Without an allow-list, it is easy to expose too much.

**PL Q:** Dlaczego correlation ID jest ważny w evidence?
**PL A:** Pozwala połączyć status HTTP, problem body, log backendu i wpis historii. Bez niego raport błędu jest trudniejszy do odtworzenia.
**EN:** A correlation ID connects the HTTP status, problem body, backend log, and history entry. Without it, a bug report is harder to reproduce.

**PL Q:** Dlaczego Rest Assured logs muszą redagować `Authorization` i `Cookie`?
**PL A:** Testy często trafiają do CI logs albo notatek. Tokeny i cookies nie mogą pojawić się w artefaktach, nawet w lokalnym labie.
**EN:** Rest Assured logs must redact `Authorization` and `Cookie` because test output often goes to CI logs or notes. Tokens and cookies must not appear in artifacts, even in a local lab.

**PL Q:** Dlaczego nie implementujemy testów w tej lekcji?
**PL A:** Bo najpierw musimy mieć uzgodniony kontrakt i wybrać mały zakres backend behavior. Testy do nieistniejącego zachowania byłyby nieczytelną mieszanką specyfikacji i założeń.
**EN:** We do not implement tests in this lesson because we first need an agreed contract and a small backend behavior scope. Tests for non-existing behavior would mix specification with assumptions.
