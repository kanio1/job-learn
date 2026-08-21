---
Status: DONE
Category: enhancement
---

# Dual-depth logout (shallow vs Keycloak SSO)

## 1. Capability Proposal

**Working name:** Dual-depth sign-out and account switch  
**Why now:** Operators cannot change Keycloak persona after menu Sign out; deep logout is hidden in Session Lab.  
**Roadmap fit:** Session / OIDC learning (existing path A/B) + Merchant 360 operator UX. Not a new payment capability.

## 2. Business Goal

- **Problem:** “Sign out” does not match the mental model “I can log in as someone else.” SSO resume looks like a broken login form.
- **Outcome:** Shared-lab and role-switch users get a complete logout; learners can still see shallow vs deep.
- **If not solved:** Persona testing stays on private windows; Tenant suspended / wrong-role confusion continues.

## 3. Actors and Stakeholders

- **Primary:** platform.admin / platform.operator switching lab accounts on one browser.
- **Secondary:** tenant.admin, merchant.manager; SDET writing session E2E.
- **Internal:** lab teaching OIDC (Session Lab must remain).
- **Goals:** switch account in <30s; shared workstation does not leave a live IdP session.

## 4. Business Workflow

**Trigger:** User opens the avatar menu.

**Main success (recommended default — deep):** Sign out → clear BFF session → Keycloak `end_session` → optional “Do you want to log out?” → `/login` → Continue to Keycloak → **username/password**.

**Alternate — shallow (explicit):** “Sign out of dashboard only” → `/login` → copy: still signed in at Keycloak as `{username}` → Continue (SSO) **or** Use a different account.

**Alternate — switch account:** Clear BFF → authorize with `prompt=login` (or `prompt=select_account`) → login form **without** requiring full SLO of other hypothetical RPs.

**Failure:** Keycloak confirm cancelled → user may still have OP session; dashboard must stay logged out (BFF already cleared). Login must not look like a dead-end.

## 5. Business Rules and Decisions

| Rule | Decision |
|---|---|
| Single RP in this lab | Default Sign out = **deep** (Okta: single app → also IdP) |
| Session Lab | Keep educational End OIDC; do not be the only deep path |
| Two equal unlabeled Sign outs | **Forbidden** — labels must name the depth |
| Shallow after deep | Idempotent; `/login` stays |
| `id_token_hint` | Prefer if we can store id_token without blowing cookie; else `client_id` + confirm (current Session Lab) |
| Login page | Never only “Continue to Keycloak” after shallow logout without explaining SSO |

## 6. Domain Vocabulary

- **Shallow logout:** RP session only (`nuxt-session`).
- **Deep logout:** RP + OP SSO (`end_session`).
- **Account switch:** force re-auth (`prompt=login`) after shallow or deep.
- **SSO resume:** Keycloak skips login because `AUTH_SESSION_ID` lives.

## 7. Data Needs

- Display username on `/login` after shallow logout (from last session or none).
- `endSessionUrl` already from POST `/api/session-lab/end-session`.
- Audit: optional `SESSION_LOGOUT_SHALLOW` / `SESSION_LOGOUT_DEEP` later — not required for v1.

## 8. Candidate Acceptance Criteria

1. Avatar **Sign out** performs deep logout; next Continue to Keycloak shows Keycloak **Sign in to your account** (username + password), not silent SSO.
2. A clearly labelled **Sign out of dashboard only** (menu or Session Lab) does **not** call `end_session`; Continue may SSO-resume; page explains that.
3. **Use a different account** on `/login` reaches a Keycloak login form for another user.
4. Session Lab End OIDC still works (classroom).
5. No JWT in `localStorage` / `sessionStorage` after either logout.

## 9. Ambiguities and Open Questions

- Store `id_token` for hint vs keep confirm page?
- Put shallow only in Session Lab (simpler menu) vs both in avatar?
- `prompt=login` vs `prompt=select_account` (Keycloak theme support)?

## 10. Initial Tester Lens

- Highest risk: default Sign out change **breaks** `session.spec.ts` E2E-027 / EG-W2-11 contract.
- States: logged in, shallow logged out + SSO alive, deep logged out, confirm cancelled.
- Shared browser / two tabs: deep logout in tab A vs tab B BFF cookie.
- Do not treat Keycloak confirm as optional if `id_token_hint` absent.

## 11. Feature Sequencing Recommendation

**Split, next (small):**

1. **P0 UX copy + discovery:** `/login` after shallow: “Still signed in at Keycloak”; link **Use a different account** (`prompt=login` or End OIDC). Add Session Lab to dashboard search / Mirror Lab already links it.
2. **P0 product default:** menu Sign out = existing Session Lab `end-session` hop (deep). Relabel tests.
3. **P1:** explicit shallow in avatar as secondary, or keep shallow only in Session Lab.
4. **Defer:** back-channel SLO, Sign out everywhere across devices, storing id_token.

**Reject:** two unlabeled Sign out buttons; hiding deep logout only on `/admin/session-lab`.

## 12. Spec Kit Input Summary

- **Title:** Dual-depth logout and account switch  
- **Intent:** Match operator mental model of Sign out on a single-RP Keycloak dashboard while preserving a named shallow path for OIDC learning.  
- **Scope:** `AppUserMenu`, `login.vue`, reuse `/api/session-lab/end-session`, optional `prompt=login`, POM `session.spec.ts`.  
- **Non-goals:** RFC 7009 token revocation, front/back-channel to other RPs, Keycloak admin console UX.  
- **Must-preserve:** HttpOnly session; no JWT in Web Storage; Session Lab classroom.  
- **Open questions:** id_token_hint vs confirm; shallow in menu vs lab-only.
