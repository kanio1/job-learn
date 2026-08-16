import { expect, test } from '@playwright/test'
import {
  merchantAlphaId,
  merchantManagerBffRequest,
  seededAlphaPaymentOrderId,
} from '../support/live-merchant-bff'

const path = `/api/merchants/${merchantAlphaId}/payment-orders/${seededAlphaPaymentOrderId}`

test('BFF preserves backend conditional GET 304 with an empty body', async () => {
  const bff = await merchantManagerBffRequest()

  try {
    const initial = await bff.get(path)
    const etag = initial.headers().etag
    expect(initial.status()).toBe(200)
    expect(etag).toBeTruthy()

    const conditional = await bff.get(path, { headers: { 'If-None-Match': etag } })
    expect(conditional.status()).toBe(304)
    expect(await conditional.text()).toBe('')
    expect(conditional.headers().etag).toBe(etag)
    expect(conditional.headers()['cache-control']).toBe('no-store')
    expect(conditional.headers().vary).toContain('Authorization')

    const head = await bff.head(path)
    expect(head.status()).toBe(200)
    expect(await head.text()).toBe('')
    expect(head.headers().etag).toBe(etag)
  } finally {
    await bff.dispose()
  }
})
