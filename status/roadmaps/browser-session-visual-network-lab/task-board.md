---
name: mrl-task-board
parent: browser-session-visual-network-lab
status: IMPLEMENTED
last_updated: 2026-08-13
---

# Task board — kolejka MRL

Każdy task: ID `MRL-Txx`. Statusy: `TODO` | `IN_PROGRESS` | `DONE` | `BLOCKED`.  
Zaimplementowane 2026-08-13 (T01–T29). `REST-MULTIPART-01` nadal otwarte.

## Wave 1 — Session + Visual + Network (P0)

| Order | Task ID | Story | Tytuł | Status |
|---:|---|---|---|---|
| 1 | MRL-T01 | E0-S1 | Hub `/admin/mirror-lab` + flagi + nav (bez mobile) | DONE |
| 2 | MRL-T02 | E1-S1 | Cookie inspector UI + API atrybutów | DONE |
| 3 | MRL-T03 | E1-S2 | Idle lock + countdown + clock hook | DONE |
| 4 | MRL-T04 | E1-S3 | Logout BFF + empty storageState deny `/admin` | DONE |
| 5 | MRL-T05 | E1-S6 | Guest POM project + JWT-not-in-storage assertions | DONE |
| 6 | MRL-T06 | E2-S1 | Visual Lab page + stable tiles | DONE |
| 7 | MRL-T07 | E2-S2 | Component + full-page screenshot specs (mocked) | DONE |
| 8 | MRL-T08 | E3-S1 | Fault Lab: 503→200 button | DONE |
| 9 | MRL-T09 | E3-S2 | Abort/offline + lie-body scenario | DONE |
| 10 | MRL-T10 | E6-S1 | RA/IT szkielety flag + CSRF/GET-body gdy E4 | DONE |
| 11 | MRL-T11 | E6-S2 | PW catalog wired: mocked vs POM split | DONE |

## Wave 2 — Session advanced + Visual polish + HAR (P1)

| Order | Task ID | Story | Tytuł | Status |
|---:|---|---|---|---|
| 12 | MRL-T12 | E1-S4 | Concurrent sessions + revoke | DONE |
| 13 | MRL-T13 | E1-S5 | CSRF demo path vs Bearer no-CSRF | DONE |
| 14 | MRL-T14 | E2-S3 | Dark mode + stylePath mask + break-visual toggle | DONE |
| 15 | MRL-T15 | E3-S3 | HAR record/replay booking | DONE |
| 16 | MRL-T16 | E3-S4 | CORS credentials lab | DONE |
| 17 | MRL-T17 | E6-S3 | Learner empty specs session/visual/network | DONE |

## Wave 3 — PayU mirrors (P1)

| Order | Task ID | Story | Tytuł | Status |
|---:|---|---|---|---|
| 18 | MRL-T18 | E4-S1 | GET with body → 403 | DONE |
| 19 | MRL-T19 | E4-S2 | lang na redirectUri + hosted locale | DONE |
| 20 | MRL-T20 | E4-S3 | Refund notify same notifyUrl | DONE |
| 21 | MRL-T21 | E4-S4 | Payment link expiry UI + clock | DONE |
| 22 | MRL-T22 | E4-S5 | Same-origin iframe widget | DONE |
| 23 | MRL-T23 | E4-S6 | OAuth grant contrast panel | DONE |

## Wave 4 — Bank-like (P2, gated)

| Order | Task ID | Story | Tytuł | Status |
|---:|---|---|---|---|
| 24 | MRL-T24 | E5-S1 | Step-up BFF (no realm) + dynamic linking copy | DONE |
| 25 | MRL-T25 | E5-S2 | Statement CSV/PDF download | DONE |
| 26 | MRL-T26 | E5-S3 | Disputes + multipart evidence (REST-MULTIPART-01) | DONE |
| 27 | MRL-T27 | E5-S4 | Maker-checker dual storageState | DONE |
| 28 | MRL-T28 | E5-S5 | AIS-lite consent + revoke | DONE |
| 29 | MRL-T29 | E6-S4 | Traceability update + status registry | DONE |
