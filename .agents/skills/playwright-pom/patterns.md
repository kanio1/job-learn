# Lab POM patterns

Canon files, not a second framework. Names match `docs/testing/playwright-method-playbook/04-principal-typescript.md` (Drajna / Konovalov / Minchev).

## Allowed

| Pattern | Here | File |
|---|---|---|
| Facade | one fixture exposes every page | `tests-pom/pages/App.ts` |
| Fixture DI | `test.extend<{ app, api }>` | `tests-pom/fixtures/index.ts` |
| Thin BasePage | navigation + overlay + `expectLoaded` | `tests-pom/pages/BasePage.ts` |
| Component object | compose, do not inherit business | `tests-pom/pages/components/ConfirmModal.ts` |
| Adapter | cookie session against BFF | `tests-pom/api/bff-client.ts` |
| Factory | unique refs per worker | `tests-pom/data/factories.ts` |
| Builder | payment body | `tests-pom/data/payment-order-draft.ts` |
| Type guard | problem+json | `tests-pom/utils/problem.ts` |
| Method oracles | ISTQB rows, not a runner | `tests-pom/methods/` |

## Forbidden

| Pattern | Why |
|---|---|
| Screenplay (actors/tasks/questions) | overkill; lab uses App + specs |
| Fat BasePage / AuthenticatedPage inheritance | composition (`Sidebar`, `UserMenu`) |
| God object one class for the app UI | `App` is a facade, not 40 business methods |
| `page.route` / `route.fulfill` in `tests-pom` | live oracle; this suite is live-only |
| `waitForTimeout` | web-first `expect` |
| CSS / nth-child / XPath | role, label, test id |
| Role branching inside a page class | same class, different `storageState` |
| Importing `tests-pom` pages into `tests-pom-learner` | learner types their own `My*` |
| `vi.mock` as the live POM seam | Vitest only; even there prefer real seams |

## New page object

```ts
export class ExamplePage extends BasePage {
  async goto(): Promise<void> {
    await super.goto('/admin/example')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByRole('heading', { name: 'Example' })).toBeVisible()
  }

  rowByReference(reference: string) {
    return this.page.getByRole('row').filter({ hasText: reference })
  }

  async openCreateForm(): Promise<void> {
    await this.byTestId('action-create-example').click()
  }
}
```

Wire it on `App`, then use `app.example` from a spec. Do not add a parallel fixture per page unless the suite already does.

## Divergence from public skills

- Playwright docs put `expect()` inside page objects. This lab keeps journey claims in specs; `expectLoaded()` is the exception.
- TestDino POM pack allows assertions on page objects in one guide and forbids them in another. Follow the lab table above.
- LambdaTest / TestMu skills default to cloud grids and fat BasePage. Ignore.
- `lackeyjb/playwright-skill` is on-the-fly automation (MCP replacement), not this suite.

## Official locators

Use Playwright’s locator priority from [Page object models](https://playwright.dev/docs/pom) plus [locators](https://playwright.dev/docs/locators): role and label first. Auto-wait via `expect(locator)`, not `page.$`.
