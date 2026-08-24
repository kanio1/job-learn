# Expert review prompt — Event Streaming Lab, two commits + WIP

Użyj tego promptu w nowej sesji Codex. Review obejmuje dwa przypięte commity
oraz bieżące zmiany staged, unstaged i untracked.

---

## Prompt do wklejenia

Jesteś niezależnym senior reviewerem: Java/Spring architect, Kafka engineer
oraz Senior SDET. W repozytorium Payment Quality Engineering Lab wykonaj
gruntowny, evidence-driven review:

1. commitu 364f520b9156239c34fcd0986c1936ce41afb3db;
2. commitu 7f0e54d7880e041078ce7a7051231692cc9fc175;
3. bieżącego working tree względem 7f0e54d.

Repozytorium: /home/suso/job-learn

Tryb: REVIEW ONLY / FINDINGS ONLY.

Nie edytuj plików, nie uruchamiaj formatterów zapisujących zmiany, nie aktualizuj
statusów, nie commituj, nie stashuj, nie zmieniaj historii Git i nie uruchamiaj
ani nie zatrzymuj usług. Nie poprawiaj kodu. Możesz zaproponować najmniejszą
poprawkę, ale jej nie wdrażaj. Nie drukuj sekretów, tokenów ani haseł.

## 1. Zamrożony zakres

Przypięte punkty:

~~~text
BASE     = 6ee6827268cfc51d114a2fbcab982e6bef4d3159
COMMIT1  = 364f520b9156239c34fcd0986c1936ce41afb3db
COMMIT2  = 7f0e54d7880e041078ce7a7051231692cc9fc175
WIP_BASE = COMMIT2
~~~

Potwierdź repozytorium, HEAD i relacje commitów:

~~~bash
git -C /home/suso/job-learn rev-parse --show-toplevel
git -C /home/suso/job-learn branch --show-current
git -C /home/suso/job-learn rev-parse HEAD
git -C /home/suso/job-learn merge-base --is-ancestor 6ee6827268cfc51d114a2fbcab982e6bef4d3159 364f520b9156239c34fcd0986c1936ce41afb3db
git -C /home/suso/job-learn merge-base --is-ancestor 364f520b9156239c34fcd0986c1936ce41afb3db 7f0e54d7880e041078ce7a7051231692cc9fc175
~~~

Oczekiwany HEAD to 7f0e54d7880e041078ce7a7051231692cc9fc175.
Jeżeli HEAD jest inny, nie zastępuj SHA przez nowe HEAD~1 albo HEAD~2.
Zgłoś SCOPE_DRIFT, pokaż aktualny log i zatrzymaj review.

Zbuduj trzy oddzielne inventory.

### A. Commit 1

~~~bash
git -C /home/suso/job-learn show --stat --summary --format=fuller 364f520b9156239c34fcd0986c1936ce41afb3db
git -C /home/suso/job-learn diff --name-status 6ee6827268cfc51d114a2fbcab982e6bef4d3159..364f520b9156239c34fcd0986c1936ce41afb3db
git -C /home/suso/job-learn diff 6ee6827268cfc51d114a2fbcab982e6bef4d3159..364f520b9156239c34fcd0986c1936ce41afb3db
~~~

### B. Commit 2

~~~bash
git -C /home/suso/job-learn show --stat --summary --format=fuller 7f0e54d7880e041078ce7a7051231692cc9fc175
git -C /home/suso/job-learn diff --name-status 364f520b9156239c34fcd0986c1936ce41afb3db..7f0e54d7880e041078ce7a7051231692cc9fc175
git -C /home/suso/job-learn diff 364f520b9156239c34fcd0986c1936ce41afb3db..7f0e54d7880e041078ce7a7051231692cc9fc175
~~~

### C. WIP po Commit 2

~~~bash
git -C /home/suso/job-learn status --porcelain=v1 -uall
git -C /home/suso/job-learn diff --stat 7f0e54d7880e041078ce7a7051231692cc9fc175
git -C /home/suso/job-learn diff --name-status 7f0e54d7880e041078ce7a7051231692cc9fc175
git -C /home/suso/job-learn diff --cached --name-status
git -C /home/suso/job-learn ls-files --others --exclude-standard
~~~

Zakres WIP obejmuje staged, unstaged i wszystkie nieignorowane pliki untracked.
Git diff nie pokazuje zawartości nowych plików. Otwórz każdy plik zwrócony przez
git ls-files --others --exclude-standard. Nie pomijaj lockfile, konfiguracji,
dokumentacji, skryptów ani plików statusowych.

Zbuduj też zagregowany widok kodu śledzonego:

~~~bash
git -C /home/suso/job-learn diff 6ee6827268cfc51d114a2fbcab982e6bef4d3159
~~~

Widok zagregowany służy do wykrywania interakcji. Pochodzenie findingu ustalaj
z widoków A, B i C. Każdy finding oznacz:

~~~text
COMMIT1
COMMIT2
WORKTREE
CROSS_LAYER
PRE_EXISTING_NOT_WORSENED
~~~

Nie raportuj problemu z BASE jako finding zakresu, chyba że jeden z dwóch
commitów albo WIP go aktywuje lub pogarsza.

## 2. Audyt przypadkowo zacommitowanych zmian

Commit 2 ma wiadomość dotyczącą przeglądu i zmian skilli, ale zawiera również
duży, mieszany zestaw plików. Nie zakładaj, że wszystkie były zamierzone.
Przejrzyj cały commit, nie tylko apps.

Dla każdego commitu przygotuj:

| Plik/grupa | Zgodność z commit message | Klasyfikacja | Dowód |
|---|---|---|---|

Klasyfikacje:

- EXPECTED;
- RELATED_BUT_POORLY_GROUPED;
- ACCIDENTAL_OR_UNRELATED;
- SUSPICIOUS_PLACEMENT;
- GENERATED_OR_VENDOR_BLOAT;
- SECURITY_OR_SECRET_RISK;
- REQUIRES_AUTHOR_CONFIRMATION.

Sprawdź szczególnie:

- kod i testy aplikacji w commicie opisanym jako zmiany skilli;
- AGENTS.md, CLAUDE.md, ADR, spec, status i roadmapy;
- masowe dodania, usunięcia i rename skilli oraz symlinki .cursor;
- duże upstream/reference bundles i ich rzeczywistą użyteczność;
- root META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports;
- osierocone odwołania po usunięciu lub rename skilli;
- produktowe backend/frontend files dołączone do governance;
- sekrety, .env, storage state, tokeny i prywatne klucze;
- root pnpm-lock.yaml i zgodność z faktycznym workspace;
- czy każdy commit jest reviewable i możliwy do bezpiecznego revertu/cherry-picku.

Nie przepisuj historii i nie wykonuj reset, rebase ani force-push. Możesz
zaproponować plan przyszłego rozdzielenia commitów, ale tylko jako rekomendację.

## 3. Kontekst repozytorium

Przeczytaj w tej kolejności:

1. AGENTS.md.
2. .agents/skills/README.md.
3. docs/agents/issue-tracker.md.
4. .codex/README.md.
5. .codex/current-state.md.
6. status/index.md.
7. status/evidence/latest-validation.md.
8. .codex/adr/0002-kafka-event-streaming.md.
9. .codex/specs/kafka-event-streaming-lab.md.
10. status/roadmaps/kafka-event-streaming-lab/task-board.md.
11. docs/testing/event-streaming-lab/01-acceptance-cases.md.
12. .codex/review-checklist-eventlab.md.

Status, task-board i current-state są deklaracjami, nie dowodem. Jeżeli mówią
DONE/PASS, a kod, test lub świeże uruchomienie tego nie potwierdza, zgłoś drift.

Priorytet źródeł:

~~~text
AGENTS
→ ACCEPTED ADR
→ aktualna spec i acceptance cases
→ kod oraz wykonywalne kontrakty
→ task-board/status
→ dokumenty historyczne
~~~

.kiro jest historycznym prior art i nie nadpisuje aktualnego ADR/spec.

## 4. Obowiązkowe skills

Jawnie potwierdź i zastosuj:

1. code-review — osobne osie Standards i Spec.
2. eventlab-kafka oraz references/lenses-lab-vs-prod.md.
3. java-spring-review.
4. rest-api-test-design.
5. playwright-sdet-review.
6. project-skill-governance-and-quality-review dla zmian .agents/skills.
7. ponytail-review jako oddzielny pass over-engineeringu.
8. official-docs-and-versioned-research dla faktów zależnych od wersji.

Przeczytaj również jako repozytoryjne standardy, nie tryby implementacyjne:

- .agents/skills/spring-modulith/SKILL.md;
- .agents/skills/spring-modulith/jdk25.md;
- .agents/skills/java-rest-api-testing-effective-java-mentor/SKILL.md;
- jego references/review-checklist.md;
- .agents/skills/nuxt-frontend/SKILL.md;
- .agents/skills/playwright-pom/SKILL.md.

Jeżeli Lenses MCP i payment-lab już działają, opcjonalnie wykonaj wyłącznie
read-only: kafka-topic-audit, kafka-dlq-review i kafka-consumer-lag.
Najpierw przeczytaj lenses-lab-vs-prod.md. Wyniki klasyfikuj jako
REAL_LAB_DEFECT, LAB_SHAPED_ACCEPTED albo PLAYGROUND_ONLY.
Nie wykonuj resend, reset offsets, create/delete topic ani innych mutacji.

## 5. Rzeczywiste wersje i dokumentacja

Sprawdź pom.xml, package.json, lockfiles i compose. Jeżeli Maven jest dostępny:

~~~bash
cd /home/suso/job-learn/apps/backend
./mvnw -q help:evaluate -Dexpression=project.parent.version -DforceStdout
./mvnw -q dependency:tree -Dincludes=org.springframework.kafka:spring-kafka,org.apache.kafka:kafka-clients,org.springframework.modulith:spring-modulith-events-kafka,org.springframework.retry:spring-retry
~~~

Każdy wersjozależny finding musi podawać komponent, rozwiązaną wersję, oficjalny
URL, sekcję API i odniesienie do linii kodu.

Źródła pierwotne:

- https://docs.oracle.com/en/java/javase/25/
- https://openjdk.org/projects/jdk/25/
- https://docs.spring.io/spring-framework/reference/
- https://docs.spring.io/spring-kafka/reference/4.0/
- https://docs.spring.io/spring-modulith/docs/2.0.x/api/
- https://kafka.apache.org/40/
- oficjalne docs Spring Boot 4.0.x, Testcontainers 2.0.x, Maven Failsafe,
  Playwright 1.61 i Nuxt 4.4.

Nie używaj latest, gdy opisuje inną linię. Brak dokładnej dokumentacji patcha
oznacz jawnie i obniż confidence.

## 6. Oś Standards — obowiązkowe passy

### Java 25 / Effective Java

Sprawdź release 25, preview/incubator API, brak nieuzasadnionego JPMS, virtual
threads lub Scoped Values, sensowne records/sealed/patterns, immutability,
defensive copies, nullability, equals/hashCode, wyjątki bez sekretów, reflection,
stringly-typed config, potrzebę nowych zależności i czy powinien nimi zarządzać
Boot BOM. Nie proponuj funkcji JDK 25 tylko dlatego, że istnieje.

### Spring Framework / Boot / Modulith

Sprawdź:

- granice eventlab i brak cross-module internal imports;
- brak Kafka producer/listener w shared, payment lub audit;
- Profile kafka plus app.event-lab.enabled;
- start, Surefire i seeds bez brokera przy flag-off;
- bean conditions, autoconfiguration registration, wiring i lifecycle;
- poprawny placement AutoConfiguration.imports w Maven resources;
- proxy/self-invocation dla Transactional, Async i listenerów;
- transakcje DB, publication registry, rollback i resubmission;
- Flyway, JPA validate, typy i constraints;
- race na unique consumer_group plus event_id;
- duplicate insert i stan transakcji;
- REST, problem+json, correlation ID, security i tenant masking;
- minimalność i zbędne warstwy.

### Kafka / Spring Kafka

Sprawdź:

- zgodność image Kafka w compose i Testcontainers;
- KRaft single-node, 3 partycje, RF1 i PLAINTEXT jako lab constraints;
- auto-create OFF i jawne source/retry/DLT topics;
- RetryableTopic, RetryTopicNamesProviderFactory i KafkaAdmin naming;
- czy lab.event-lab.dlq.v1 faktycznie otrzymuje rekord;
- attempts kontra retries, backoff i suffixing dla rozwiązanej wersji;
- wpływ non-blocking retry na ordering;
- key=targetId, partycjonowanie, envelope i headers;
- poison przed lub wewnątrz listenera;
- offset commit względem eventlab_processed;
- restart, rebalance, redelivery i idempotencję;
- brak fałszywych twierdzeń o Kafka EOS.

Terminologia: Kafka delivery = at-least-once; wymagany efekt domenowy =
idempotent exactly-once effect; dead-letter topic = DLT.

### REST Assured / KafkaIT

Dla każdego istotnego testu określ purpose, risk, stimulus, oracle, dowody HTTP,
DB i Kafka, unikalność danych oraz mutację, która powinna uczynić test czerwonym.

Szukaj test theater, DB-only oracle zamiast brokera, Thread.sleep, błędnych
timeoutów, współdzielonych topic/group/event IDs, zależności od kolejności,
wycieków consumer groups, luk 401/403/404, read vs operate, tenant mask,
correlation ID, nienaruszonego payment/audit oraz błędnego użycia Surefire
zamiast Failsafe.

### Nuxt / BFF / Playwright

Sprawdź browser → Nitro BFF → Spring, brak bezpośredniego Kafka/backend URL,
token tylko w sealed server session, Zod-before-render, proxy status/headers,
brak kafkajs/Kafka UI/ECharts, lokatory role → label → placeholder → test id,
business assertions w specs, brak page.route/route.fulfill/routeWebSocket,
storage state, auth dependencies, unikalne worker data, deterministic polling,
widoczny PROCESSED/DEAD i realny test braku wycieku Authorization.

### Skill governance

Sprawdź triggery i when-not-to-use, granice review/implement, martwe linki po
rename, symlinki .cursor, overlap, sprzeczne źródła prawdy, wersje wobec POM,
oddzielenie upstream reference od reguł repo oraz spójność README/SOURCE.

### Ponytail

Osobno znajdź zbędne dependencies, abstrakcje z jednym użyciem, duplikację
topic/config, speculative flexibility, kod zastępowalny JDK/Spring/Kafka API,
helper bloat, vendored/reference bloat i powieloną dokumentację.

Format:

~~~text
file:Lx: delete|stdlib|native|yagni|shrink: problem. Najmniejsze zastępstwo.
~~~

Nie usuwaj bezpieczeństwa, tenant isolation, idempotencji ani wymaganych testów.
Zakończ: net: -N lines possible albo Lean already. Ship.

## 7. Oś Spec

Zbuduj traceability matrix z ACCEPTED ADR 0002, aktualnej spec, acceptance cases
i aktywnych KAFKA-T.

| ID | Requirement | Origin | Implementation | Test/oracle | Fresh run | Status | Gap |
|---|---|---|---|---|---|---|---|

Statusy: VERIFIED, IMPLEMENTED_NOT_EXECUTED, PARTIAL, MISSING, CONTRADICTED,
NOT_IN_REVIEW_SCOPE, BLOCKED_BY_ENVIRONMENT.

Nie kopiuj PASS/DONE bez niezależnego dowodu. Odpowiedz:

1. Czy flag-off daje zero beanów i połączeń do Kafka?
2. Czy rollback daje zero publication i broker record?
3. Czy crash-heal tworzy incomplete publication i udowadnia resubmit?
4. Czy duplicate/rebalance/restart daje jeden efekt DB przy redelivery?
5. Czy poison test konsumuje rekord z rzeczywistego custom DLT?
6. Czy retry count, backoff i topic names odpowiadają wersji Spring Kafka?
7. Czy ordering per targetId jest zachowany lub jawnie ograniczony?
8. Czy inject wymaga operate, a read-only dostaje 403?
9. Czy tenant masking działa w list, detail, BFF i UI?
10. Czy payment i audit są nienaruszone po poison/duplicate?
11. Czy Playwright udowadnia proof-of-delivery do 5 s bez protocol oracle?
12. Czy docs/status nie deklarują więcej niż udowodniono?
13. Które produktowe pliki weszły do commitu o skilli i czy zasadnie?
14. Czy usunięcie/rename skilli złamało instrukcje lub routing?

Dla każdej luki zaproponuj najmniejszy test i realny bug, który wykryje.

## 8. Walidacja

Obowiązkowo:

~~~bash
git -C /home/suso/job-learn diff --check 6ee6827268cfc51d114a2fbcab982e6bef4d3159
git -C /home/suso/job-learn diff --check 7f0e54d7880e041078ce7a7051231692cc9fc175
rg -n 'page\.route|route\.fulfill|routeWebSocket|kafkajs|ECharts|kafka-ui|AKHQ|Thread\.sleep' /home/suso/job-learn/apps/backend /home/suso/job-learn/apps/frontend
find /home/suso/job-learn/.cursor/skills -maxdepth 1 -type l -xtype l -print
~~~

Wyniki wyszukiwania są tropem; otwórz kontekst przed findingiem.

Backend minimum:

~~~bash
cd /home/suso/job-learn/apps/backend
./mvnw -Dtest=ModulithArchitectureTest test
~~~

KafkaIT, jeżeli Docker/Podman działa:

~~~bash
./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' -Dit.test='*KafkaIT' verify
~~~

Nigdy nie uruchamiaj restkit ani paymentsupport.

Frontend, jeżeli dotknięty:

~~~bash
cd /home/suso/job-learn/apps/frontend
corepack pnpm typecheck
corepack pnpm lint
corepack pnpm test:unit
corepack pnpm exec playwright test --config playwright.pom.config.ts --list
~~~

Live Playwright tylko gdy stack już działa i hasła są w environment. Nie
uruchamiaj automatycznie dev-stack.sh --kafka. Validator skilli uruchom zgodnie
z jego instrukcją, bez autofix.

Dla każdej komendy podaj command, exit code, passed/failed/skipped, czas,
ostrzeżenia i GREEN, RED, NOT_RUN albo ENVIRONMENT_BLOCKED.
NOT_RUN i ENVIRONMENT_BLOCKED nie oznaczają PASS.

## 9. Findings

Raportuj tylko problem obserwowalny w Commit 1, Commit 2 lub WIP i poparty kodem,
testem, wynikiem albo dokumentacją.

~~~text
[P0|P1|P2|P3] [Standards|Spec|CommitHygiene]
[COMMIT1|COMMIT2|WORKTREE|CROSS_LAYER] [layer] file:line — title

Evidence:
Expected:
Impact:
Smallest remediation:
Missing/weak test:
Confidence: HIGH | MEDIUM | LOW
~~~

P0: utrata danych, cross-tenant/security breach, sekret.
P1: złamane AC, błędna semantyka Kafka/transakcji, fałszywy PASS albo commit
niebezpieczny do samodzielnego użycia/revertu.
P2: istotna luka testowa, flake, placement, governance lub mylące docs.
P3: lokalna czytelność lub drobna niespójność.

Nie ograniczaj liczby findings i nie duplikuj root cause. RF1, PLAINTEXT oraz
single-node KRaft klasyfikuj jako LAB_SHAPED_ACCEPTED, jeśli odpowiadają ADR.

## 10. Format raportu

~~~markdown
# Event Streaming Lab — two commits + WIP expert review

## Scope snapshot
- branch i przypięte SHA
- inventory Commit 1
- inventory Commit 2
- staged / unstaged / untracked
- scope drift

## Verdict
APPROVE / APPROVE_WITH_CHANGES / REQUEST_CHANGES

## Commit hygiene verdict
### Commit 364f520
### Commit 7f0e54d

## Findings
### P0
### P1
### P2
### P3

## Standards axis
Podsumowanie poniżej 400 słów, worst finding i counts.

## Spec axis
Podsumowanie poniżej 400 słów, worst finding i counts.

## Layer review
### Java 25 / Effective Java
### Spring Framework / Boot / Modulith
### Kafka / retry / DLT / idempotency
### REST Assured / KafkaIT
### Nuxt / BFF
### Playwright
### Security / tenant isolation
### Skill governance
### Documentation and evidence drift

## Accidental inclusion matrix
| Commit | File/group | Classification | Evidence | Recommendation |

## Requirement traceability
## Critical questions 1–14
## Versioned research ledger
## Ponytail review
## Validation evidence
## Environment limitations
## Clean areas
## Recommended repair order
## Final counts
~~~

Verdict:

- dowolny P0/P1 → REQUEST_CHANGES;
- tylko P2/P3 → APPROVE_WITH_CHANGES;
- brak findings → APPROVE.

Po raporcie zatrzymaj się. Nie implementuj i nie zmieniaj historii Git.
