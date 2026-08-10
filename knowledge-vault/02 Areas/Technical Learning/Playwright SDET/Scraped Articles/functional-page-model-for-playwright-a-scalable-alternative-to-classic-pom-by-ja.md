# Functional Page Model for Playwright: A Scalable Alternative to Classic POM

The traditional Page Object Model (POM) has served as the de facto standard for structuring end-to-end test automation. Enter the **Functional Page Model (FPM)** — a modular, functional approach to organizing Playwright tests.

## What Is the Functional Page Model?

The Functional Page Model (FPM) breaks down test modules into **four separate components**:

- `component.actions.ts`: Contains reusable user interaction methods.
- `component.locators.ts`: Encapsulates all UI element selectors.
- `component.data.ts`: Manages test data and fixtures.
- `component.spec.ts`: Contains actual test definitions.

## The Structure

```
e2e/tests/clients/vitals/
    ├── vitals.actions.ts
    ├── vitals.locators.ts
    ├── vitals.data.ts
    └── vitals.spec.ts
```

## Component Breakdown

### Actions

```
export const createClinicalNote = async (page: Page, noteInput: NoteData) => {
  await Locators.chartNewBtn(page).click();
  await Locators.noteBtn(page).click();
  await Locators.noteInput(page).fill(noteInput.text);
  await Locators.saveBtn(page).click();
};
```

### Locators, Data, Specs

Specs wire together actions, data, and locators with `test()` blocks.

## Advantages

- Clear separation of concerns
- Improved reusability (pure functions)
- Easier onboarding
- Scalable for large teams

## Disadvantages

- Fragmented file structure
- Less familiar to traditional QA engineers
- Slight overhead for simple flows

## Final Thoughts

FPM trades class-based structures for composable functions — worth a serious look for growing test suites.
