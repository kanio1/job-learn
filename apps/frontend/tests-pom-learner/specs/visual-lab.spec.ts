import { test } from '@playwright/test'

// Copy-from-reference: tests/e2e/visual-lab.spec.ts (mocked goldens) and tests-pom for live flows.
// Do not import from tests-pom.

test.describe('visual lab (learner)', () => {
  // Deliberately skipped: discoverable but never executed — the learner copies
  // the reference page object from tests-pom before implementing this spec.
  test.skip('Implement after copying App facade from tests-pom', () => {})
})
