import { expect } from '@playwright/test'
import type { BffClient } from '../api/bff-client'
import { expectStatus } from '../api/bff-client'

export async function assertPersistedMerchant(api: BffClient, merchantId: string): Promise<void> {
  const get = await api.merchants.get(merchantId)
  expectStatus(get, 200)
  expect(get.body?.merchantId).toBe(merchantId)
}

export async function assertPersistedOrder(
  api: BffClient,
  merchantId: string,
  paymentOrderId: string,
): Promise<void> {
  const get = await api.payments.get(merchantId, paymentOrderId)
  expectStatus(get, 200)
  expect(get.body?.paymentOrderId).toBe(paymentOrderId)
}
