import { expect, test } from '@playwright/test'
import {
  createPaymentBody,
  merchantAlphaId,
  merchantManagerBffRequest,
  uniqueLiveReference,
} from '../support/live-merchant-bff'

test('BFF preserves payment creation and idempotency replay semantics through persistence', async ({}, testInfo) => {
  const bff = await merchantManagerBffRequest()
  const clientOrderReference = uniqueLiveReference(testInfo, 'IDEM')
  const idempotencyKey = uniqueLiveReference(testInfo, 'KEY')
  const payload = createPaymentBody(clientOrderReference)
  const path = `/api/merchants/${merchantAlphaId}/payment-orders`

  try {
    const first = await bff.post(path, {
      data: payload,
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    const firstBody = await first.json() as { paymentOrderId: string, clientOrderReference: string }

    const replay = await bff.post(path, {
      data: payload,
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    const replayBody = await replay.json()

    expect(first.status()).toBe(201)
    expect(replay.status()).toBe(200)
    expect(first.headers()['idempotency-replayed']).toBe('false')
    expect(replay.headers()['idempotency-replayed']).toBe('true')
    expect(first.headers().etag).toBeTruthy()
    expect(replay.headers().etag).toBe(first.headers().etag)
    expect(replayBody).toEqual(firstBody)

    const persisted = await bff.get(`${path}/${firstBody.paymentOrderId}`)
    expect(persisted.status()).toBe(200)
    expect((await persisted.json() as { clientOrderReference: string }).clientOrderReference)
      .toBe(clientOrderReference)

    const listed = await bff.get(path, { params: { clientOrderReference, page: '0', size: '20' } })
    const listedBody = await listed.json() as { totalElements: number, content: Array<{ paymentOrderId: string }> }
    expect(listed.status()).toBe(200)
    expect(listedBody.totalElements).toBe(1)
    expect(listedBody.content.map(order => order.paymentOrderId)).toEqual([firstBody.paymentOrderId])
  } finally {
    await bff.dispose()
  }
})
