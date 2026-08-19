import { test as base, expect } from '@playwright/test'
import '../utils/ipv4-first'
import { App } from '../pages/App'
import { BffClient } from '../api/bff-client'
import { POM_WORKER_COUNT, workerMerchant } from '../auth/accounts'
import { pomBrowserBaseURL, pomNodeBaseURL, workerManagerAuthFile } from '../utils/env'
import { ensureWorkerWorld, type WorkerWorld } from './worker-session'

/** Live POM suite: never use page.route / route.fulfill. */

export type { WorkerWorld }

type PomFixtures = {
  app: App
  api: BffClient | undefined
  workerApp: App
  ownedMerchantId: string
  storageState: string | { cookies: [], origins: [] }
}

type PomWorkerFixtures = {
  workerWorld: WorkerWorld
}

export const test = base.extend<PomFixtures, PomWorkerFixtures>({
  storageState: async (_fixtures, use, testInfo) => {
    if (testInfo.project.name === 'chromium-manager') {
      const index = testInfo.parallelIndex % POM_WORKER_COUNT
      await use(workerManagerAuthFile(index))
      return
    }
    const configured = testInfo.project.use.storageState
    if (typeof configured === 'string') {
      await use(configured)
      return
    }
    await use({ cookies: [], origins: [] })
  },
  ownedMerchantId: async (_fixtures, use, testInfo) => {
    const index = testInfo.parallelIndex % POM_WORKER_COUNT
    await use(workerMerchant(index).merchantId)
  },
  app: async ({ page }, use) => {
    await use(new App(page))
  },
  api: async ({ playwright, storageState }, use) => {
    if (typeof storageState !== 'string') {
      await use(undefined)
      return
    }
    const client = await BffClient.create(playwright, storageState, pomNodeBaseURL())
    await use(client)
    await client.dispose()
  },
  workerWorld: [async ({ browser, playwright }, use, testInfo) => {
    const world = await ensureWorkerWorld(browser, playwright, testInfo)
    await use(world)
    await world.api.dispose()
  }, { scope: 'worker' }],
  workerApp: async ({ browser, workerWorld }, use) => {
    const context = await browser.newContext({
      storageState: workerWorld.storageState,
      baseURL: pomBrowserBaseURL(),
    })
    const page = await context.newPage()
    await use(new App(page))
    await context.close()
  },
})

export { expect }

export function requireApi(api: BffClient | undefined): BffClient {
  if (!api) {
    throw new Error('BffClient requires project.use.storageState (not chromium-guest).')
  }
  return api
}
