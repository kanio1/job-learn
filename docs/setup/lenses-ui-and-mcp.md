# Lenses — logowanie, UI (clusters/events) i MCP

> Lab ≠ prod. Lenses to **luneta** (teleskop operatora), nie produkt ani oracle CI.
> Ramka i zasady: `.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md`.

## Dwa klastry (nie mieszaj)

| Klaster | Co to jest | Port hosta | Kiedy używać |
|---|---|---|---|
| **Payment lab overlay** | Nasz broker `payment-quality-kafka` z `infra/compose/compose.kafka.yml` (KRaft, PLAINTEXT, RF1) | **9092** (internal 19092) | Zdarzenia z outboxu, Event Lab, środowisko Lenses `payment-lab` |
| **Lenses CE demo** | Playground Community Edition: HQ :9991, MCP sidecar :8000, tematy demo (`telecom_italia_data`, vessels…) | brak 9092 na hoście | Nauka operatora na danych demo |

Lab overlay jest **właścicielem 9092**. CE demo nigdy nie binduje 9092 ani 8081
(8081 = Keycloak). Gdy oba chcą 9092, jeden padnie — overlay ma priorytet.

## Logowanie do Lenses UI

1. Podnieś lab broker: `bash scripts/dev-stack.sh --kafka`.
2. Podnieś Lenses CE (jeśli nie działa): `docker compose -f ~/lenses-compose.yml up -d`.
3. Otwórz **http://localhost:9991**.
4. Zaloguj się danymi z `.env` stacka Lenses (zmienna typu
   `LENSES_ADMIN_PASSWORD`; w demo często `admin`/`admin`). Nie commituj haseł.
5. Na ekranie **Connections** wybierz środowisko:
   - **`payment-lab`** → nasz broker, adres `PLAINTEXT://payment-quality-kafka:19092`
     (nazwa serwisu z compose, NIE localhost),
   - `lenses-demo` → dane playground.

## Co oglądać w UI

### Clusters / Topology

Wybierz `payment-lab`. Zobaczysz jeden combined KRaft broker i tematy:

| Topic | Skąd |
|---|---|
| `lab.auditable-actions.v1` (3 partycje RF1) | outbox Modulith → externalizer |
| `lab.auditable-actions.v1-retry` | Spring `@RetryableTopic` |
| `lab.auditable-actions.v1-dlt` | techniczny DLT Springa (nie kontraktowy) |
| `lab.event-lab.dlq.v1` | **kontraktowy DLT** Event Lab (poison → DEAD) |

Uwaga: `eventlab_processed` to tabela w Postgres, nie topic Kafka.

### Events (Data explorer / Live events)

Topic `lab.auditable-actions.v1`:

- **key** = paymentOrderId (targetId),
- **value** = koperta v1 JSON: `eventId`, `action`, `targetType`, `targetId`,
  `tenantRef`, `correlationId`, `schemaVersion:"v1"`,
- kolumny pomocnicze: partition, offset, timestamp.

Smoke test end-to-end: autoryzuj płatność w UI labu (lub REST authorize z
`Idempotency-Key`) → w Live events pojawia się rekord `PAYMENT_AUTHORIZED` z
kluczem = paymentOrderId.

### Consumer groups

- `eventlab-inspector` — grupa konsumencka Event Lab; lag per partycja; po
  restarcie backendu lag chwilowo rośnie i wraca do 0.
- `…-retry`, `…-dlt` — techniczne grupy Spring RetryableTopic.

### DLT po poison

Z UI Event Lab (`/admin/event-lab`) zinjectuj poison (wymaga
`platform:event-lab:operate`). Konfiguracja `@RetryableTopic(attempts = "3")`
oznacza **3 łącznę próby** = pierwotne dostarczenie + 2 retry (fixed 500 ms);
dopiero po nich rekord ląduje na `lab.event-lab.dlq.v1` — zobaczysz go w Live
events tego tematu razem z nagłówkami retry. To potwierdza ścieżkę DEAD bez
zaglądania w Postgres.

### Czego NIE robić

- Nie resetuj offsetów `eventlab-inspector` bez świadomej decyzji.
- Nie traktuj findingów audytu dosłownie: RF=1 / PLAINTEXT / single-node KRaft
  są **zamierzone** (ADR 0002). `kafka-topic-audit` będzie raportował „RF=1
  critical” — sklasyfikuj jako *lab-shaped*, nie jako ticket produktowy.
- Sekcja Schema Registry będzie pusta (wave 1 = JSON envelope) — to OK.

## Lenses MCP (Grok/Claude)

Serwer jest skonfigurowany globalnie: `~/.grok/config.toml`, sekcja `Lenses`
(stdio przez `~/lenses-mcp/run-stdio.sh`, klucz API w `~/lenses-mcp/.env`).
HTTP sidecar CE na :8000 służy demo — do pracy nad Event Lab używaj środowiska
`payment-lab` przez stdio server.

Odkrycie narzędzi w sesji agenta:

```
search_tool("Lenses topics")
use_tool("Lenses__list_topics", { "environment": "payment-lab" })
```

Praktyczne sekwencje (kolejność: najpierw przeczytaj `lenses-lab-vs-prod.md`):

| Cel | Narzędzie / skill |
|---|---|
| Lista/audyt tematów labu | `Lenses__list_topics`, skill `kafka-topic-audit` (oczekuj lab-shaped findings) |
| Rekordy tematu | `Lenses__list_messages` (filtr po partycji/offsetcie) |
| Lag grupy `eventlab-inspector` | skill `kafka-consumer-lag` |
| Review DLT po poison | skill `kafka-dlq-review` |
| Ręczny resend pojedynczego rekordu (tylko lab, świadomie) | `Lenses__resend_message` |

Zasady:

1. Zawsze podawaj `environment: "payment-lab"` — inaczej trafisz w demo data.
2. MCP **nie jest oracle CI** — nie wywołuj go z Failsafe ani Playwright.
3. Operacje mutujące (resend, delete offsets) tylko na labie i świadomie.
4. Sekrety zostają w `~/lenses-mcp/.env` — nie commituj i nie drukuj ich.

## Szybka checklista „czy Lenses widzi mój event”

1. `scripts/dev-stack.sh --kafka` — broker healthy.
2. Autoryzacja płatności (UI albo REST z Idempotency-Key).
3. Lenses UI → `payment-lab` → `lab.auditable-actions.v1` → Live events →
   rekord z key = paymentOrderId.
4. Consumer group `eventlab-inspector` → lag spada do 0.
5. `/admin/event-lab` → wiersz PROCESSED dla tego paymentOrderId.
