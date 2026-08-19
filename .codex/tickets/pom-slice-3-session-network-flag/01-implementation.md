# 01 — POM Slice 3 session / network / flag

**What to build:** Idle BVA + merchants overlay, logout empty cookies + mid-journey cookie clear, Mirror statements BFF 404 on flag-off, live PDF magic, Network Lab offline + 503 TTL, widget iframe Approve.

**Blocked by:** None — live `--app` stack (flag-off overlay :3012).

**Seams:** Playwright E2E + BFF (`tests-pom`)

**Status:** done

**Category:** enhancement

- [x] Idle TTL-1 then +2s lock; overlay on `/admin/merchants`
- [x] FR-S04a empty `nuxt-session` after Sign out; EP-W2-014 clearCookies
- [x] Flag-off GET `/api/mirror-lab/statements` 404 (+ visual/network deep-link)
- [x] Live PDF download `%PDF-`
- [x] Network Lab setOffline + 503→200→TTL→503
- [x] Widget iframe Approve → fulfillment CONFIRMED
- [x] Catalog leftover retag
- [x] Playwright green gate + `--mirror-off` + lint
