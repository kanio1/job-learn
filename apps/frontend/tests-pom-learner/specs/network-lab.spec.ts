import { test } from '@playwright/test'

// Copy-from-reference: tests-pom/specs/network-lab.spec.ts
// Do not import from tests-pom. Live suite: waitForResponse, never route.fulfill.

test.describe('network lab (learner)', () => {
  // Deliberately skipped: discoverable but never executed — the learner copies
  // the reference page object from tests-pom before implementing this spec.
  test.skip('Implement after copying NetworkLabPage from tests-pom', () => {})
})
