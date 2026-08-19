# 04 — Principal: szkielet TS (Drajna / Konovalov / Minchev)

```text
tests-pom/
  auth/        + tenant-admin.setup.ts
  fixtures/    App + opcjonalny BffClient
  pages/       cienki BasePage, intent methods
  api/         BffClient — zero seedLearning
  data/        factories + PaymentOrderDraft
  utils/       env, http, wait-bff, roles.openAs, dates, problem, persistence
  methods/     ISTQB rows
  specs/       tylko przepływy
```

Wzorce: Facade (`App`), composition (`ConfirmModal`), factory (unique*), adapter (`BffClient`), type guard (`isProblemDetails`), fixture DI, builder (`PaymentOrderDraft`).

Nie: tłusty BasePage, Screenplay, `page.route` w live, hasła w repo, import z `tests-pom` do learnera.

JSDoc na klasie `methods/`: **co robi** / **co się zmienia** / **e2e\|rest** / **jaki seed**.

Copy-map: `tests-pom/README.md` i `methods/README.md`.
