# Prompt — Merchant 360 implement (Grok 4.6 + Cursor, high cache)

Operator playbook. The agent loop lives in skill `merchant-360-implement`. This file is how **you** start and continue chats so Grok 4.6 prompt cache hits.

Sources: [xAI prompt caching](https://docs.x.ai/developers/advanced-api-usage/prompt-caching) (exact prefix, append-only, `x-grok-conv-id` / Cursor conversation id), [xAI Grok 4.6](https://docs.x.ai/developers/grok-4-6), [Cursor prompting](https://cursor.com/docs/agent/prompting), [Cursor rules](https://cursor.com/docs/context/rules), [Cursor skills / Custom Modes](https://cursor.com/docs/skills.md). Stay under the **200K** Grok 4.6 request cliff ([pricing](https://docs.x.ai/developers/pricing)).

## Why this shape

Cache is an **exact prefix match** from the start of the request. Hits if you:

1. Keep Cursor **Rules / Skills / system** identical (Custom Mode on, no extra always-on dumps).
2. Stay in the **same Agent chat** for a whole fala (T01–T05, then T06–T09, …).
3. Paste the long body **once**. Later turns are one line (`next` / `PW-M360-Txx`).
4. Never edit earlier messages. Never re-paste this prompt. Never @-attach a different pile of files each turn.
5. Point at paths; do not paste `00`–`09` catalogs into the chat.

Misses if you: new chat every task, switch model mid-fala, @ 20 files on turn 1, paste the spec again, or compact/summarize by hand.

## Setup (once per chat)

1. Model: **Grok 4.6**, effort **High** (default). Do not switch to Fast mid-slice.
2. Agent mode. Custom Mode: `/merchant-360-implement` → **Use as Mode** (Option+Enter / Alt+Enter) so the skill stays on every turn ([Cursor prompting](https://cursor.com/docs/agent/prompting#custom-modes)).
3. Optional: `/tdd` as a second mode if the UI allows one skill mode only, skip — this skill already composes `tdd`.
4. Do **not** @-mention catalogs, epics, or `AGENTS.md` on the first message. The skill has the read order.
5. Watch the context ring. Near ~180K: finish the current T, start a **new** chat with the same first-message body and `Next` from `.codex/merchant-360-slice.md`.

## First message (paste once)

Copy the block below unchanged except the last line.

```text
Merchant 360 implement. Follow skill merchant-360-implement.

Constraints already in that skill: live stack, no page.route, 412 not 409, no CRM, no Nuxt UI bump, Flyway not CONCURRENTLY, no .kiro, no tests-pom-learner, one PW-M360-Txx then stop.

Read order is in the skill. Overlay: .codex/merchant-360-slice.md

Implement only the Next task (or the id on the last line). TDD at the seam the epic names. Update the slice overlay and task-board row when done. Do not commit.

Slice: PW-M360-T01
```

For a later fala’s new chat, keep every line identical; change only `Slice: PW-M360-T06` (or whatever `.codex/merchant-360-slice.md` says Next is).

## Follow-ups (this chat — do not re-paste the body)

```text
next
```

```text
PW-M360-T02
```

```text
blocked: <one sentence>. Continue the same slice after you read the failure.
```

If a review is needed after a slice (optional, new short turn):

```text
Review the slice you just closed. code-review + java-spring-review and/or playwright-sdet-review. No new T.
```

## Do not

- Paste FR tables, mermaid, or ISTQB matrices into the message.
- @ `docs/testing/merchant-360-erp-lab/` as a folder.
- Continue Ops Wave 2, CPL, or learner copies in this mode.
- Say “implement the whole milestone” in one turn.

## Fala → new chat (when the ring is fat)

| Chat | Slices |
|---|---|
| 1 | T01–T05 (E1 contracts) |
| 2 | T06–T09 (UTable) |
| 3 | T10–T11 (slideover/form) |
| 4 | T12–T13 (RBAC/ETag) |
| 5 | T14–T15 (import/kanban) |
| 6 | T16–T18 (tree/search/charts) |
| 7 | T19–T20 (calendar/grid; pin 4.7.1 gate) |

Same Custom Mode. First message = the block above with the new `Slice:` id.
