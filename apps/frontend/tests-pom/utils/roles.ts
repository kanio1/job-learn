import type { Browser, BrowserContext, Page } from '@playwright/test'
import { App } from '../pages/App'
import { BffClient, type Playwright } from '../api/bff-client'

/**
 * Second BrowserContext for dual-role journeys (RBAC, dual-control).
 * What changes: storageState path. Same App + optional BffClient.
 */
export async function openAs(
  browser: Browser,
  playwright: Playwright,
  storageStatePath: string,
  baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000',
): Promise<{ context: BrowserContext, page: Page, app: App, api: BffClient }> {
  const context = await browser.newContext({ storageState: storageStatePath, baseURL })
  const page = await context.newPage()
  const app = new App(page)
  const api = await BffClient.create(playwright, storageStatePath)
  return { context, page, app, api }
}
