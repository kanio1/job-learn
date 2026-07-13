import { expect, test } from '../fixtures/live-worker-data'

test.describe.configure({ mode: 'parallel' })

test('worker-owned payment allocation has a distinct, diagnosable owner key', async ({ liveWorkerData }) => {
  const payment = await liveWorkerData.createPayment('ALLOCATE-A')

  expect(payment.clientOrderReference).toBe(`${liveWorkerData.ownershipKey}-ALLOCATE-A`)
  expect(payment.idempotencyKey).toBe(`IDEM-${liveWorkerData.ownershipKey}-ALLOCATE-A`)
  expect(liveWorkerData.ownedReferences()).toEqual([payment.clientOrderReference])
})

test('parallel worker creates an independently owned resource without collisions', async ({ liveWorkerData }) => {
  const payment = await liveWorkerData.createPayment('ALLOCATE-B')

  expect(payment.clientOrderReference).toBe(`${liveWorkerData.ownershipKey}-ALLOCATE-B`)
  expect(payment.idempotencyKey).toBe(`IDEM-${liveWorkerData.ownershipKey}-ALLOCATE-B`)
  expect(liveWorkerData.ownedReferences()).toEqual([payment.clientOrderReference])
})
