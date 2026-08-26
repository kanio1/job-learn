import { existsSync } from 'node:fs'
import type { Browser } from '@playwright/test'
import { merchantManagerAccountForWorker, POM_WORKER_COUNT, workerMerchant } from '../auth/accounts'
import { saveKeycloakStorageState } from '../auth/keycloak.setup'
import { BffClient, type Playwright } from '../api/bff-client'
import { pomBrowserBaseURL, pomNodeBaseURL, workerManagerAuthFile } from '../utils/env'

export type WorkerWorld = {
  index: number
  merchantId: string
  merchantReference: string
  storageState: string
  api: BffClient
}

export async function ensureWorkerWorld(
  browser: Browser,
  playwright: Playwright,
  testInfo: { parallelIndex: number },
): Promise<WorkerWorld> {
  const index = testInfo.parallelIndex % POM_WORKER_COUNT
  const world = workerMerchant(index)
  const storageState = workerManagerAuthFile(index)
  if (!existsSync(storageState)) {
    const page = await browser.newPage({
      storageState: { cookies: [], origins: [] },
      baseURL: pomBrowserBaseURL(),
    })
    try {
      await saveKeycloakStorageState(page, merchantManagerAccountForWorker(index), storageState)
    }
    finally {
      await page.close()
    }
  }
  const api = await BffClient.create(playwright, storageState, pomNodeBaseURL())
  return {
    index,
    merchantId: world.merchantId,
    merchantReference: world.merchantReference,
    storageState,
    api,
  }
}
