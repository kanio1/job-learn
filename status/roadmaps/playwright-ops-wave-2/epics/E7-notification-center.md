---
name: epic-e7-notification-center
parent: playwright-ops-wave-2
epic: E7
tasks: [PW-OPS-T12]
last_updated: 2026-08-20
---

# Epic E7 — Notification Center (#19)

**Cel produktowy:** dzwonek z unread count, nie tylko toast.  
**Cel dydaktyczny:** async badge, read/unread persist, popover; może konsumować te same `eventId` co feed.

**Gate:** V34 tabela; E6 publisher pomaga, ale notyfikacje mogą powstać z HTTP lifecycle listener nawet zanim WS UI gotowe. Preferuj po T10.

Nuxt UI: `UPopover`, `UChip`, `UUser`, `UButton`, `USeparator`.  
POM: `NotificationCenterComponent`.

---

## Story E7-S1 — Inbox REST + badge

**Task:** `PW-OPS-T12` · P0

### Jako / chcę / aby

Jako support widzę „3” i po otwarciu / mark read zostaje 2 po reload.

### Business case

`BC-OPS-19` — Centrum, nie toast. Toast może dodatkowo pokazać push.

### Use case

`UC-OPS-27`

```text
new notification → badge 3 → 4
open popover → item visible
mark as read → badge 4 → 3
reload → stays read
```

### REST

| Method | Path |
|---|---|
| GET | `/api/notifications?unreadOnly=` |
| POST | `/api/notifications/{id}/read` |
| POST | `/api/notifications/read-all` |

Recipient = JWT `sub`. UNIQUE `(recipient_subject, event_id)` — duplicate WS nie dubluje badge.

### SQL

[01-infra B.4](../01-infra-postgres-keycloak-stack.md).

### Acceptance criteria

- [ ] Grupy New / Earlier (read_at null vs set) w popover.
- [ ] Chip ukryty przy 0 (`show=false`).
- [ ] Readonly może czytać i mark read **własne** (to nie mutacja domeny płatności).
- [ ] Manager nie widzi cudzych.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| RA-OPS-190 | RA | lifecycle capture tworzy notif dla support? **Decyzja:** platform + support z `payments:read`; manager tylko własny merchant. Udokumentować w implementacji audience. |
| RA-OPS-191 | RA | mark read persist SELECT read_at |
| RA-OPS-192 | RA | duplicate event_id drugi insert 0 rows (unique) |
| RA-OPS-193 | RA | BOLA GET cudzego id 404 |
| PW-OPS-E2E-190 | E2E | badge 3→4 po inject/notif create |
| PW-OPS-E2E-191 | E2E | open popover item visible |
| PW-OPS-E2E-192 | E2E | mark read badge 4→3 |
| PW-OPS-E2E-193 | E2E | reload stays read |
| PW-OPS-E2E-194 | E2E | read-all |

Audience default (żeby RA-OPS-190 nie był mglisty): emit do (1) assignee support case jeśli event case, (2) platform.admin **nie** spamować każdego capture — **tylko** FAILED, refund-approval-needed, case-assigned. Capture happy-path = feed WS, **nie** inbox. Inbox = actionable.

### Learning

Badge vs toast. Pinia dozwolone: dzwonek w layout + popover = dwa surface’y → `stores/notifications.ts` (nigdy token).
