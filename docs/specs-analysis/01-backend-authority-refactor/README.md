# Spec 01: Backend Authority Refactor

**Status:** ✅ Ukończony  
**Branch:** `018-rest-security-p1-error-auth-method-hardening`  
**Pliki źródłowe:** `.kiro/specs/backend-authority-refactor/`

---

## Cel

Backend-only, test-first, behavior-preserving refaktoring warstwy security/authority w `apps/backend`.
Cel: zastąpić magic strings jednym źródłem prawdy, uczynić konwerter ról explicit i fail-closed,
skonfigurować czytelną nazwę principala, dodać walidację azp tokenu.

---

## Zakres (4 obszary — R1, R2, R5, R4)

| Ref | Obszar | Zmiana |
|---|---|---|
| R1 | Typed Authority Catalog | Klasa `Authorities` z 9 stałymi compile-time; `SecurityConfig` + `MerchantController` korzystają ze stałych |
| R2 | Explicit Role Converter | `KeycloakRealmRoleConverter` → explicit allowlist 10 ról; unknown roles ignorowane (fail-closed) |
| R5 | Principal Claim Name | `preferred_username` z fallback do `sub` |
| R4 | azp Validation | `AuthorizedPartyValidator` skomponowany z default validators (issuer + timestamp) |

**Jedyna intencjonalna zmiana zachowania:** nieznane realm roles nie produkują już inert `platform:<name>` authority — są ignorowane.

---

## Kluczowe decyzje architektoniczne

| Decyzja | Wybór | Uzasadnienie |
|---|---|---|
| Umiejscowienie `Authorities` | `lab.paymentquality.shared.security` | Najwyższa kohezja z konwerterem i SecurityConfig |
| Moduł `shared` → OPEN | `@ApplicationModule(type = OPEN)` | Merchant może referencować `Authorities` bez łamania ModulithArchitectureTest |
| `operate` role | `MERCHANT_PAYMENTS_OPERATE` jako stała lokalna konwertera | Nie jest egzekwowana przez żadną regułę; nie należy do katalogu 9 enforced constants |
| azp: validate `azp` vs `aud` | Option A: `azp == payment-quality-dashboard` | Brak zmiany realm Keycloak; `aud` domyślnie = `account` w tokenach Keycloak |
| Test profile: azp validator aktywny wszędzie | Wszystkie profile włącznie z `test` | `TestJwtConfiguration` mirroruje produkcyjny decoder; `TestJwtSupport` addytywnie dodaje `azp` claim |

---

## Właściwości poprawności (PBT — jqwik)

| Property | Waliduje |
|---|---|
| P1: Known roles → documented authorities | Req 2.2, 2.6 |
| P2: Unknown roles → ignored (fail-closed) | Req 2.7, 3.5 |
| P3: Malformed/absent claim → empty | Req 2.4, 2.5 |
| P4: Catalog no-drift | Req 1.2–1.5 |
| P5: azp accept/reject + composition | Req 5.2–5.4 |
| P6: Principal name derivation | Req 4.2–4.4 |

---

## Wyniki regresji

```
./mvnw test   → BUILD SUCCESS — 266 tests, 0 failures, 0 errors, 5 skipped
./mvnw verify → BUILD SUCCESS — 266 unit + 4 integration (Failsafe), all green
```

Dodatkowe fixy w ramach final checkpoint (pre-existing bugs):
- Naprawiono import `startsWith` (Mockito vs Hamcrest)
- Dodano `merchantPaymentLifecycleToken()` w `TestJwtSupport`
- Dodano `GlobalExceptionHandler` dla 405/415/406 z `application/problem+json`

---

## Cross-spec impact (dokumentacja follow-up)

Spec R2 unieważnia założenie "inert authority" w `iam-roles-and-keycloak-login`:
- design.md Decision 1 i discrepancy A zostały zaktualizowane ✅
- `KeycloakRealmRoleConverter` używa teraz explicit allowlist — composite role display names (`PLATFORM_ADMIN` etc.) są **ignorowane**, nie produkują `platform:PLATFORM_ADMIN`
- Property 1 i Property 9 w design.md `iam-roles` zostały zaktualizowane ✅

---

## Lessons learned

1. **Characterization-first PBT** — napisz testy pinujące AKTUALNE zachowanie przed refaktoringiem; po refaktoringu flip jednej asercji udowadnia zachowanie preservation
2. **Additive test changes** — `TestJwtConfiguration`/`TestJwtSupport` poszerzono, nie osłabiono żadnej istniejącej asercji
3. **`operate` role** — role mające authority ale nie będące egzekwowaną przez żaden endpoint to dokumentowana kategoria; `Authorities` zawiera tylko 9 enforced constants
4. **Cross-module constants** — `@ApplicationModule(type = OPEN)` to właściwy wzorzec Modulith dla technicznego modułu cross-cutting
