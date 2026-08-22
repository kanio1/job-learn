# 07 — Techniki poza Foundation i metamorphic (interview / curriculum)

Notatka SDET: metody doboru danych i projektowania TC/UC/BC, których **nie** recytujesz z ISTQB FL (EP, BVA, DT, ST, UC, pairwise, error guessing). Metamorphic (`MR-*`, `methods/metamorphic/`) już jest w [03](03-test-architect-techniques.md).

Na rozmowie zabłyśniesz, gdy powiesz **jaką relację w produkcie** pokrywasz i **którą warstwę** (tu: REST Assured vs BFF vs E2E), nie gdy wymienisz akronimy.

Kanon FL w labie: [03](03-test-architect-techniques.md) · zestawy: [05](05-combinations.md) · kod: `apps/frontend/tests-pom/methods/`.

---

## ISTQB Advanced (CTAL-TA) — „czarna skrzynka 2.0”

**Domain testing** (Kaner, Padmanabhan, Hoffman — *The Domain Testing Workbook*; CTAL-TA 4.0 ch. 3). EP+BVA na **wielowymiarowej** domenie: nie „amount 0 i 101”, tylko *amount × waluta × status × If-Match*. W labie: capture gdy `autoCapture` ON i `maxAutoCaptureMinor` na granicy 1 vs 0.

**Classification Tree Method** (Grochtmann/Grimm; CTAL-TA). Drzewo parametrów (rola, If-Match, from-status, amount class) → liście to TC. Lepsze na interview niż „zrobiłem pairwise”, bo widać **jak obciąć** kombinacje. W labie: `refundPolicy × amountMinor × PIN required`.

**Cause-effect graphing** (Myers, *The Art of Software Testing*; Elmendorf; CTAL-TA). Przyczyny → skutki z AND/OR/NOT, potem minimalna tabela. W labie: „brak If-Match **albo** stale **albo** illegal from” → 428 / 412 / 409, nigdy 409 na lock.

**Orthogonal arrays** (CTAL-TA, obok pairwise). Silniejsza statystyka niż „wziąłem PICT”. Na interview: pairwise = każda para; OA = kontrolowana siła t (2, 3…). W labie: tylko Checkout `mode × currency × scenario`, **nie** mieszać z ST ([05](05-combinations.md)).

**Defect-based / taxonomies** (CTAL-TA; Beizer *Software Testing Techniques*). TC z klasy defektu: lost update, BOLA mask 404, h3 wrap zamiast problem+json. API-070 (evidence 404 problem) to dokładnie to.

---

## Rapid Software Testing (Bach / Bolton)

To **nie** jest kolejna tabela ISTQB. To dobór misji i oracli pod niepewność. Skill w repo: `rapid-software-testing-risk-thinking`.

**HTSM** (*Heuristic Test Strategy Model*, Satisfice). Cztery osie: Project / Product / Quality criteria / Test techniques. Na interview: „zanim wybiorę BVA, modeluję produkt (SFDPOT) i kryteria (CRUSSPIC STMPL)”.

**SFDPOT** (product elements): Structure, Function, Data, Platform, Operations, Time. Wave 2: Data = JSONB policy; Operations = dwóch writerów; Time = PIN TTL / `page.clock`; Platform = BFF vs Spring.

**Oracle heuristics HICCUPPS** (History, Image, Comparable, Claims, User, Product, Purpose, Standards). „Skąd wiem, że to bug?” — 412 vs 409 to **Claims + Standards** (tabela HTTP w labie), nie zgadywanie.

**Tours** (Whittaker *Exploratory Software Testing*; w RST jako chartery). Landmark / money / copilot / anticonformity. W labie: money tour = refund+PIN; anticonformity = inject malformed WS.

**Charter + time-box** (Bach, session-based / SBTM). 90 min: „Atakuj If-Match na PATCH settings; szukam 409 tam gdzie ma być 412”. Interview: SBTM to raportowanie eksploracji, nie „klikam ad hoc”.

---

## Książki, które brzmią rzadziej niż ISTQB FL

**Output-domain partitioning** (Beizer). Partycjonujesz **wynik**, nie input: zbiór 412 vs 428 vs 409 vs 200+ETag+1. Potem szukasz najtańszego inputu, który trafia w każdą klasę odpowiedzi.

**Syntax testing / dirty dozen** (Beizer; Whittaker *How to Break Software*). Złośliwa składnia: malformed ETag `"stale-etag"` → 400, nie 412. W labie: malformed If-Match vs stale `"v99"`.

**CRUD / entity lifecycle matrix** (nie ISTQB FL). Każda encja × Create/Read/Update/Delete × rola × tenant. Support case i saved views to klasyczny CRUD; merchant **nie** ma Delete — świadomy gap, warto powiedzieć.

**Risk-based selection** (Bach *Rapid Software Testing*; ISO 29119-1 risk; CTAL-TM). Nie „100% EP”, tylko **która partycja zabija pieniądze/izolację**. Playbook: „minimal covering POM, pełna macierz w RA”.

**Scenario testing** (Kaner). Długi, wiarygodny dzień operatora, nie atom. Happy-path: znajdź → 360 → 412 conflict → PIN → feed. Scenariusz **uzupełnia** DT, nie zastępuje.

**All-pairs vs base-choice** (Cohen; NIST combinatorial; Ammann/Offutt). Base-choice: jeden „normalny” wektor, zmieniasz **jeden** parametr. Tanio na policy: baza MANUAL+OFF+threshold 50, potem tylko ON, tylko 0, tylko 100.

**Atomic vs molecular scenarios** (Copeland *A Practitioner’s Guide to Software Test Design*). Atom = jedna krawędź ST; molekuła = UC. Świadomie: RA = atomy; E2E = molekuły.

---

## Jak to powiedzieć na interview

„Foundation daje EP/BVA/DT. Na tym produkcie dokładam **domain testing** (amount × PIN × policy), **classification tree** żeby nie eksplodować ról, **cause-effect** na 412/428/409, a strategię biorę z **RST HTSM**: najpierw SFDPOT i oracłe HICCUPPS, potem dopiero tabela. Metamorphic zostawiam na relacje list (węższy filtr ⊆ szerszy), nie na każdy TC.”

To brzmi jak ktoś, kto czytał Kanera i Bacha, a nie tylko ściągę CTFL.

Źródła (nie cytować na rozmowie jako URL): ISTQB CTAL-TA 4.0 ch. 3; Satisfice HTSM; Kaner *Domain Testing Workbook*; Beizer *Software Testing Techniques*; Whittaker *How to Break Software* / *Exploratory Software Testing*; Copeland *A Practitioner’s Guide to Software Test Design*; Myers *The Art of Software Testing*.
