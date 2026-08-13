import { test as base, expect } from '@playwright/test'
import { App } from '../pages/App'
import { BffClient } from '../api/bff-client'

/** Live POM suite: never use page.route / route.fulfill. */

type PomFixtures = {
  app: App
  api: BffClient | undefined
}

export const test = base.extend<PomFixtures>({
  app: async ({ page }, use) => {
    await use(new App(page))
  },
  api: async ({ playwright }, use, testInfo) => {
    const storageState = testInfo.project.use.storageState
    if (typeof storageState !== 'string') {
      await use(undefined)
      return
    }
    const client = await BffClient.create(playwright, storageState)
    await use(client)
    await client.dispose()
  },
})

export { expect }

export function requireApi(api: BffClient | undefined): BffClient {
  if (!api) {
    throw new Error('BffClient requires project.use.storageState (not chromium-guest).')
  }
  return api
}
