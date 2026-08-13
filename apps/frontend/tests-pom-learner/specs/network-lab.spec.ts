import { test } from '@playwright/test'

// Copy-from-reference: tests-pom/specs/network-lab.spec.ts
// Do not import from tests-pom. Live suite: waitForResponse, never route.fulfill.

test.describe('network lab (learner)', () => {
  test.skip(true, 'Implement after copying NetworkLabPage from tests-pom')
})
