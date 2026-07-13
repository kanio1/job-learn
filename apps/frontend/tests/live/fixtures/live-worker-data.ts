import { request as playwrightRequest, test as base, type APIRequestContext, type WorkerInfo } from '@playwright/test'
import { merchantAlphaId } from '../support/live-merchant-bff'

interface OwnedPayment {
  paymentOrderId: string
  clientOrderReference: string
  idempotencyKey: string
}

export interface LiveWorkerData {
  runId: string
  workerIndex: number
  ownershipKey: string
  createPayment(scenario: string): Promise<OwnedPayment>
  ownedReferences(): readonly string[]
}

function requiredRunId(): string {
  const runId = process.env.PLAYWRIGHT_LIVE_RUN_ID
  if (!runId || !/^[A-Za-z0-9-]{3,32}$/.test(runId)) {
    throw new Error('Live parallel suite requires PLAYWRIGHT_LIVE_RUN_ID (3-32 alphanumeric/hyphen characters).')
  }
  return runId
}

function ownerKey(runId: string, workerInfo: WorkerInfo): string {
  return `PW-LIVE-${runId}-W${workerInfo.workerIndex}`
}

async function authenticatedBffRequest(): Promise<APIRequestContext> {
  return playwrightRequest.newContext({
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000',
    storageState: 'tests/.auth/live-merchant-manager.json',
  })
}

export const test = base.extend<{ liveWorkerData: LiveWorkerData }>({
  liveWorkerData: [async ({}, use, workerInfo) => {
    const runId = requiredRunId()
    const ownershipKey = ownerKey(runId, workerInfo)
    const request = await authenticatedBffRequest()
    const references: string[] = []

    await use({
      runId,
      workerIndex: workerInfo.workerIndex,
      ownershipKey,
      async createPayment(scenario: string): Promise<OwnedPayment> {
        const reference = `${ownershipKey}-${scenario}`
        const idempotencyKey = `IDEM-${ownershipKey}-${scenario}`
        const response = await request.post(`/api/merchants/${merchantAlphaId}/payment-orders`, {
          data: {
            amountMinor: 4321,
            currency: 'PLN',
            clientOrderReference: reference,
          },
          headers: { 'Idempotency-Key': idempotencyKey },
        })
        if (response.status() !== 201) {
          throw new Error(`worker-owned payment creation failed with ${response.status()}`)
        }
        const body = await response.json() as { paymentOrderId?: string }
        if (!body.paymentOrderId) {
          throw new Error('worker-owned payment creation returned no paymentOrderId')
        }
        references.push(reference)
        return { paymentOrderId: body.paymentOrderId, clientOrderReference: reference, idempotencyKey }
      },
      ownedReferences: () => [...references],
    })

    // Deterministic retention: no DELETE endpoint exists and a global reset would
    // erase another worker's records. The isolated compose volume is recreated
    // before a live assurance run; retained owner-prefixed data aids diagnosis.
    await request.dispose()
  }, { scope: 'worker' }],
})

export { expect } from '@playwright/test'
