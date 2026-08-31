import { test as base, expect } from '@playwright/test'
import '../utils/ipv4-first'
import { App } from '../pages/App'
import { BffClient, type StorageState } from '../api/bff-client'
import { POM_WORKER_COUNT, workerMerchant } from '../auth/accounts'
import { pomBrowserBaseURL, pomNodeBaseURL, workerManagerAuthFile } from '../utils/env'
import { ensureWorkerWorld, type WorkerWorld } from './worker-session'
import { ActorFactory } from './actors'
import { z } from 'zod'

/** Live POM suite: never use page.route / route.fulfill. */

export type { WorkerWorld }

type PomFixtures = {
  app: App
  api: BffClient
  actors: ActorFactory
  workerApp: App
  ownedMerchantId: string
  storageState: StorageState
}

type PomWorkerFixtures = {
  workerWorld: WorkerWorld
}

export const test = base.extend<PomFixtures, PomWorkerFixtures>({
  // oxlint-disable-next-line eslint/no-empty-pattern -- Playwright fixtures require destructured dependencies.
  storageState: async ({}, use, testInfo) => {
    if (testInfo.project.name === 'chromium-manager') {
      const index = testInfo.parallelIndex % POM_WORKER_COUNT
      await use(workerManagerAuthFile(index))
      return
    }
    const configured = testInfo.project.use.storageState
    const storageStatePath = z.string().safeParse(configured)
    if (storageStatePath.success) {
      await use(storageStatePath.data)
      return
    }
    await use({ cookies: [], origins: [] })
  },
  // oxlint-disable-next-line eslint/no-empty-pattern -- Playwright fixtures require destructured dependencies.
  ownedMerchantId: async ({}, use, testInfo) => {
    const index = testInfo.parallelIndex % POM_WORKER_COUNT
    await use(workerMerchant(index).merchantId)
  },
  app: async ({ page }, use) => {
    await use(new App(page))
  },
  api: async ({ playwright, storageState }, use) => {
    const client = await BffClient.create(playwright, storageState, pomNodeBaseURL())
    await use(client)
    await client.dispose()
  },
  actors: async ({ browser, playwright }, use) => {
    const actors = new ActorFactory(browser, playwright)
    try {
      await use(actors)
    }
    finally {
      await actors.dispose()
    }
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
