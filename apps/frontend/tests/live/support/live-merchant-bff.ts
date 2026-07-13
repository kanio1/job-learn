import { request as playwrightRequest, type APIRequestContext, type TestInfo } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { liveAuthFiles } from '../auth/live-keycloak'

export const merchantAlphaId = '00000000-0000-0000-0000-0000000000b1'
export const seededAlphaPaymentOrderId = '00000000-0000-0000-0000-0000000000c1'

export async function merchantManagerBffRequest(): Promise<APIRequestContext> {
  return playwrightRequest.newContext({
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000',
    storageState: liveAuthFiles.merchantManager,
  })
}

export function uniqueLiveReference(testInfo: TestInfo, label: string): string {
  const worker = testInfo.workerIndex
  const token = randomUUID().slice(0, 8)
  return `PW-LIVE-${label}-W${worker}-${token}`
}

export function createPaymentBody(clientOrderReference: string) {
  return {
    amountMinor: 1234,
    currency: 'PLN',
    clientOrderReference,
  }
}
