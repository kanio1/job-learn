import { test } from '@playwright/test'

// Copy-from-reference: tests-pom/specs/session-guest.spec.ts
// Do not import page objects from tests-pom.

test.describe('session guest (learner)', () => {
  // Deliberately skipped: discoverable but never executed — the learner copies
  // LoginPage and fixtures from tests-pom before implementing this spec.
  test.skip('Implement after copying LoginPage and fixtures from tests-pom', () => {})
})
