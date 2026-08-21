# Shallow vs deep logout with Keycloak (OIDC)

## Answer

Two logout depths are **standard**, not a lab quirk:

| Depth | Clears | Typical product label |
|---|---|---|
| Shallow (RP / application) | Dashboard BFF cookie only | “Sign out of this app” |
| Deep (OP / SSO) | BFF cookie **and** Keycloak browser SSO | “Sign out” / “Sign out everywhere” (this client + IdP) |

OpenID Connect **RP-Initiated Logout 1.0** defines deep logout via `end_session_endpoint`. The OP **SHOULD** ask the End-User whether to log out of the OP as well, and **MUST** ask if `id_token_hint` is missing. The spec’s security note: in kiosk / shared-machine cases, leaving the OP session alive **violates the user’s expectation of being completely logged out**.

Auth0 documents **three session layers** (application, Auth0/SSO, upstream IdP). Okta’s Spring guidance: default is app-only; optionally also log out of the IdP — “If you only have a single application, then you may want to log out of the IdP as well.”

This lab today: menu **Sign out** = shallow only (by design, Session Lab owns deep). That is protocol-legal and useful for teaching SSO, but **wrong as the sole labelled “Sign out”** on a single-RP payment dashboard where operators switch Keycloak personas.

## Why it matters here

Operators hit “Continue to Keycloak” and are silently re-authenticated as the same user. Role-switch (platform.operator → platform.admin) fails. Deep logout lives only at `/admin/session-lab`, which is **not in the primary sidebar**.

## Project impact

Recommended product default for this lab (one RP): **Sign out = deep**. Keep shallow as an explicit advanced action and keep Session Lab as the protocol classroom. Login must explain SSO resume if shallow remains anywhere.

## Test impact (REST Assured / Playwright REST / Playwright E2E)

- Existing `session.spec.ts`: “Sign out does not call Keycloak `end_session`” **must change** if default Sign out becomes deep.
- New E2E: Sign out → Keycloak confirm Logout → `/login` → Continue → username/password (not silent SSO).
- New E2E: “Sign out of dashboard only” still skips `end_session`.
- Optional: `prompt=login` “Use a different account” without killing SSO.

## Sources

- [OpenID Connect RP-Initiated Logout 1.0](https://openid.net/specs/openid-connect-rpinitiated-1_0.html) — OP SHOULD/MUST confirm OP logout; kiosk security note; `id_token_hint` RECOMMENDED
- [Auth0 Logout Session Layers](https://auth0.com/docs/authenticate/login/logout) — application vs Auth0 SSO vs upstream IdP
- [Okta: Spring OIDC logout options](https://developer.okta.com/blog/2020/03/27/spring-oidc-logout-options) — app-only vs app+IdP; single-app → also IdP
- [Curity: OpenID Connect logout](https://curity.io/resources/learn/openid-connect-logout/) — SLO as SSO counterpart; RP-initiated `end_session`
- [Keycloak server admin: RP-Initiated Logout](https://www.keycloak.org/docs/latest/server_admin/index.html) — browser logout; optional confirmation when `id_token_hint` omitted; `post_logout_redirect_uri`
- Lab contract: `docs/testing/session-bff-oidc-contract.md` (path A vs B)

## Uncertainty / follow-up

- Storing `id_token` for `id_token_hint` vs cookie size (lab already omits `id_token` from `nuxt-session`).
- Whether `prompt=login` is enough for persona switch without SLO.
- Front-/back-channel logout of other RPs is out of scope (this lab has one dashboard client).
