import { expect } from '@playwright/test'
import type { BffClient } from '../api/bff-client'

export async function assertPersistedMerchant(api: BffClient, merchantId: string): Promise<void> {
  const get = await api.getMerchant(merchantId)
  expect(get.status).toBe(200)
  expect(get.body?.merchantId).toBe(merchantId)
}

export async function assertPersistedOrder(
  api: BffClient,
  merchantId: string,
  paymentOrderId: string,
): Promise<void> {
  const get = await api.getPaymentOrder(merchantId, paymentOrderId)
  expect(get.status).toBe(200)
  expect(get.body?.paymentOrderId).toBe(paymentOrderId)
}
