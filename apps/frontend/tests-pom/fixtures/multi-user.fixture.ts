import type { Browser, Playwright } from '@playwright/test'
import { App } from '../pages/App'
import { BffClient } from '../api/bff-client'
import { pomAuthFiles, pomBrowserBaseURL, pomNodeBaseURL } from '../utils/env'

export async function openAdminAndOperator(browser: Browser, playwright: Playwright) {
  const adminContext = await browser.newContext({
    storageState: pomAuthFiles.platformAdmin,
    baseURL: pomBrowserBaseURL(),
  })
  const operatorContext = await browser.newContext({
    storageState: pomAuthFiles.platformOperator,
    baseURL: pomBrowserBaseURL(),
  })
  const adminPage = await adminContext.newPage()
  const operatorPage = await operatorContext.newPage()
  const operatorApi = await BffClient.create(playwright, pomAuthFiles.platformOperator, pomNodeBaseURL())
  return {
    adminPage,
    operatorPage,
    adminApp: new App(adminPage),
    operatorApp: new App(operatorPage),
    operatorApi,
    async close(): Promise<void> {
      await operatorApi.dispose()
      await adminContext.close()
      await operatorContext.close()
    },
  }
}
