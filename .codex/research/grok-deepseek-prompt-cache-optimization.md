# Grok Build CLI + DeepSeek-V4-Flash-0731 prompt-cache optimization

## Question

Czy prompt-goal i tickety programu Playwright `tests-pom` są ułożone tak, aby Grok Build CLI uruchomiony na `deepseek-v4-flash` uzyskiwał wysoki prompt/context cache hit rate, i jak je poprawić bez osłabienia acceptance contract?

## Current Answer

Po optymalizacji: tak, struktura jest dobrze dopasowana do cache opartego na identycznym prefiksie.

Oba providery premiują ten sam kształt wejścia:

- statyczne instrukcje na początku;
- identyczne wcześniejsze wiadomości;
- wyłącznie dopisywanie nowych turnów;
- zmienny stan możliwie późno i możliwie mały;
- kontynuowanie tej samej sesji zamiast przebudowywania promptu.

DeepSeek automatycznie cache'uje wspólne prefiksy. Hit wymaga pełnego dopasowania utrwalonej jednostki prefiksu; jednostki powstają m.in. na granicy requestu, po wykryciu wspólnego prefiksu i w interwałach długiego wejścia. Mechanizm jest best-effort, a odpowiedź API raportuje `prompt_cache_hit_tokens` i `prompt_cache_miss_tokens`.

xAI również automatycznie cache'uje identyczne początkowe wiadomości. Oficjalne wskazówki mówią, by front-loadować statyczne treści, nie edytować wcześniejszych messages i kontynuować przez stabilną conversation identity. W Grok Build CLI najbliższym bezpiecznym odpowiednikiem jest zachowanie jednej sesji przez `--continue`/`--resume`; to jest wniosek z modelu sesji CLI, ponieważ CLI nie dokumentuje użytkownikowi bezpośredniej kontroli nagłówka `x-grok-conv-id` dla custom modelu.

Lokalnie zweryfikowano:

- Grok Build CLI `1.0.5` stable;
- dostępny alias modelu: `deepseek-v4-flash`;
- oficjalna tabela DeepSeek mapuje ten alias na `DeepSeek-V4-Flash-0731`;
- Grok automatycznie ładuje stabilne repo instructions `CLAUDE.md` i `AGENTS.md` (łącznie około 5.8k tokenów według `grok inspect`), co tworzy duży potencjalnie cache'owalny prefiks.

## Why It Matters

Poprzedni goal był merytorycznie poprawny, lecz zawierał cache-hostile elementy:

1. kończył się tekstem `Start with ticket 01`, który musiałby zostać zmieniony po etapie;
2. wymagał edycji checkboxów we wcześniej czytanych ticketach;
3. sugerował ponowne czytanie pełnej, 382-liniowej specyfikacji;
4. łączył długą stabilną politykę z bieżącym stanem wykonania;
5. kierował do datowanego/zmiennego evidence zamiast stałej ścieżki.

Zmiana pojedynczego znaku we wczesnym fragmencie zmniejsza ponowne wykorzystanie kolejnych tokenów. Krótszy prompt nie jest sam w sobie głównym celem; ważniejsza jest niezmienność dużego prefiksu.

## Project Impact

Wprowadzono następujący układ:

| Warstwa | Mutability | Zawartość | Reguła odczytu |
|---|---|---|---|
| `AGENTS.md` + project instructions | stabilna | zasady repo | automatyczny prefix Grok |
| goal `.md` | immutable podczas programu | cel, invariants, loop, DONE-gate | prompt wejściowy |
| pełna specyfikacja | immutable | findings i globalne decyzje | raz na nową/skomapktowaną sesję; później tylko fragmenty |
| ticket `NN-*.md` | immutable | tylko delta i acceptance IDs | jeden aktualny ticket |
| goal `.state.md` | mutable, ≤40 linii | current ticket/phase/next action | raz na turn po stabilnym kontekście |
| fixed evidence `.md` | append-only | wyniki keyed by acceptance ID | tylko potrzebny tail/sekcja |

Goal zmniejszył się ze 132 do 102 linii. Dziewięć ticketów używa teraz stabilnych identyfikatorów `T01-A01` … `T09-A12`, a nie mutowanych checkboxów. Bieżący stan mieści się w 16 liniach i nie jest częścią stałego promptu.

Nie należy podczas wykonania edytować goal/spec/tickets. Zmieniają się wyłącznie state oraz append-only evidence.

## Recommended Grok Build Commands

Model CLI wybiera się aliasem, nie nazwą wersji:

```bash
# Pierwsze uruchomienie — dokładne bajty stabilnego promptu
grok --prompt-file .codex/goals/playwright-tests-pom-quality-hardening.md \
  --verbatim \
  --model deepseek-v4-flash

# Powrót do tej samej sesji
grok --continue --model deepseek-v4-flash
```

Headless:

```bash
# Pierwszy turn
grok --prompt-file .codex/goals/playwright-tests-pom-quality-hardening.md \
  --verbatim \
  --model deepseek-v4-flash \
  --output-format json

# Kolejne turny: dopisz krótki komunikat do istniejącej sesji
grok --continue \
  --single "Continue the active program from its state file." \
  --verbatim \
  --model deepseek-v4-flash \
  --output-format json
```

Nie używać nowego `grok -p "$(cat goal.md)"` dla każdego ticketu. `--prompt-file` zachowuje dokładne bajty, a `--continue`/`--resume` zachowuje append-only historię sesji. Nie doklejać do promptu daty, SHA, current ticketu ani wyników ostatniej komendy — należą do state/evidence.

## Test Impact

Ta zmiana nie modyfikuje Playwright ani produktu. Weryfikacja dotyczy prompt inputs:

- goal nie zawiera bieżącego ticketu, daty ani zmiennych wyników;
- state jest jedynym małym plikiem mutowanym in-place;
- tickets nie zawierają checkboxów do edycji i mają trwałe acceptance IDs;
- agent czyta dokładnie jeden ticket;
- duże logi/evidence są czytane selektywnie;
- `git diff --check` sprawdza poprawność Markdown.

Pomiar produkcyjnego hit rate powinien użyć provider usage fields. DeepSeek dokumentuje `prompt_cache_hit_tokens` / `prompt_cache_miss_tokens`; xAI dokumentuje `cached_tokens`. Grok Build `--output-format json` należy sprawdzić na rzeczywistym runie, ponieważ lokalnie nie wykonano płatnego wywołania i nie potwierdzono, czy wersja CLI eksponuje provider-specific usage dla custom modelu.

## Source Quality

- Oficjalna dokumentacja xAI jest źródłem normatywnym dla Grok Build, sessions i xAI prompt caching.
- Oficjalna dokumentacja DeepSeek jest źródłem normatywnym dla model version, cache units i usage fields.
- `grok version`, `grok models`, `grok inspect` i `grok --help` są authoritative dla lokalnie zainstalowanego CLI.
- Zastosowanie `--continue` jako sposobu zwiększenia hit rate w custom DeepSeek jest rozsądną inferencją z append-only session semantics i reguł DeepSeek, nie jawną gwarancją Grok CLI.

## Sources

- [xAI Prompt Caching](https://docs.x.ai/developers/advanced-api-usage/prompt-caching) — automatic exact starting-message caching.
- [xAI Prompt Caching Best Practices](https://docs.x.ai/developers/advanced-api-usage/prompt-caching/best-practices) — stable conversation identity, append-only messages, static content first, `cached_tokens` monitoring.
- [xAI What Breaks Caching](https://docs.x.ai/developers/advanced-api-usage/prompt-caching/multi-turn) — edits/removals/reordering wcześniejszych messages powodują miss.
- [Grok Build Headless & Scripting](https://docs.x.ai/build/cli/headless-scripting) — `--session-id`, `--resume`, `--continue` and stored sessions.
- [Grok Build overview](https://docs.x.ai/build/overview) — headless execution, custom models and model selection.
- [DeepSeek Context Caching](https://api-docs.deepseek.com/guides/kv_cache) — automatic cache, persisted prefix units and hit/miss usage fields.
- [DeepSeek Models & Pricing](https://api-docs.deepseek.com/quick_start/pricing/) — `deepseek-v4-flash` = `DeepSeek-V4-Flash-0731`, context and cache-hit/cache-miss pricing.

## Uncertainty / Follow-up

- Cache remains best-effort and może zostać evicted; poprawny program nie może zależeć od hitu.
- Nie potwierdzono płatnym wywołaniem, czy Grok Build 1.0.5 pokazuje DeepSeek `prompt_cache_hit_tokens` w swoim końcowym JSON.
- Zmiana modelu, providera, wcześniejszej wiadomości, project instructions albo utworzenie nowej sesji może obniżyć hit rate.
- Po 3–5 rzeczywistych turnach warto policzyć `hit_tokens / (hit_tokens + miss_tokens)`; jeśli CLI nie eksponuje pól, użyć provider usage/billing telemetry zamiast parsowania tekstu odpowiedzi.
