import { expect, test } from '@playwright/test'
import { z } from 'zod'
import {
  createPaymentBody,
  merchantAlphaId,
  merchantManagerBffRequest,
  uniqueLiveReference,
} from '../support/live-merchant-bff'
import {
  expectNoAuthTokenLeak,
  expectNoCacheStore,
  expectProblemDetailsStructure,
} from '../../api/helpers/assert-api'

const backendUuid = z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  'Expected UUID',
)

const paymentOrderSchema = z.object({
  paymentOrderId: backendUuid,
  merchantId: backendUuid,
  clientOrderReference: z.string(),
  amountMinor: z.number().int(),
  currency: z.string(),
  status: z.string(),
})

const evidenceSchema = z.object({
  evidenceId: backendUuid,
  originalFilename: z.string(),
  contentType: z.string(),
  category: z.enum(['INVOICE', 'RECEIPT', 'OTHER']),
})

const problemSchema = z.object({
  type: z.string(),
  title: z.string(),
  status: z.number(),
  detail: z.string().optional(),
  error: z.string().optional(),
}).passthrough()

test('BFF REST contract covers status Location HEAD 304 If-Match multipart and Problem Details', async ({}, testInfo) => {
  const bff = await merchantManagerBffRequest()
  const path = `/api/merchants/${merchantAlphaId}/payment-orders`
  const reference = uniqueLiveReference(testInfo, 'REST')
  const idempotencyKey = uniqueLiveReference(testInfo, 'RESTKEY')

  try {
    await test.step('GET /api/status', async () => {
      const status = await bff.get('/api/status')
      expect(status.ok()).toBe(true)
    })

    const created = await test.step('POST create 201 Location', async () => {
      const response = await bff.post(path, {
        data: createPaymentBody(reference),
        headers: { 'Idempotency-Key': idempotencyKey },
      })
      expect(response.status()).toBe(201)
      expect(response.headers().location).toMatch(/\/api\/merchants\/.+\/payment-orders\/.+/)
      const parsed = paymentOrderSchema.safeParse(await response.json())
      expect(parsed.success, parsed.success ? '' : parsed.error.message).toBe(true)
      return parsed.data!
    })

    const detailPath = `${path}/${created.paymentOrderId}`

    await test.step('GET + If-None-Match 304', async () => {
      const initial = await bff.get(detailPath)
      expect(initial.status()).toBe(200)
      expectNoCacheStore(initial.headers())
      const etag = initial.headers().etag
      expect(etag).toBeTruthy()
      const conditional = await bff.get(detailPath, { headers: { 'If-None-Match': etag } })
      expect(conditional.status()).toBe(304)
      expect(await conditional.text()).toBe('')
    })

    await test.step('HEAD empty body', async () => {
      const head = await bff.head(detailPath)
      expect(head.status()).toBe(200)
      expect(await head.text()).toBe('')
      expect(head.headers().etag).toBeTruthy()
      expectNoAuthTokenLeak(head.headers(), await head.text())
    })

    await test.step('PATCH missing If-Match is 428 Problem Details', async () => {
      const patch = await bff.patch(detailPath, {
        data: { metadata: { note: 'live-rest' } },
        headers: { 'Content-Type': 'application/merge-patch+json' },
      })
      expect(patch.status()).toBe(428)
      const body = await patch.json()
      expect(problemSchema.safeParse(body).success).toBe(true)
      expectProblemDetailsStructure(body, 428)
    })

    await test.step('multipart evidence via APIRequestContext', async () => {
      const upload = await bff.post(`${detailPath}/evidence`, {
        multipart: {
          file: {
            name: 'live-proof.txt',
            mimeType: 'text/plain',
            buffer: Buffer.from('playwright-rest-evidence'),
          },
          category: 'RECEIPT',
        },
      })
      expect(upload.status()).toBe(201)
      const parsed = evidenceSchema.safeParse(await upload.json())
      expect(parsed.success, parsed.success ? '' : parsed.error.message).toBe(true)
      expect(parsed.data?.category).toBe('RECEIPT')
    })

    await test.step('POST export-jobs 202 Location then poll READY', async () => {
      const createdJob = await bff.post(`${path}/export-jobs`)
      expect(createdJob.status()).toBe(202)
      expect(createdJob.headers().location).toMatch(/\/export-jobs\/[0-9a-f-]+/i)
      const job = z.object({ jobId: z.string().uuid(), status: z.string() }).parse(await createdJob.json())
      await expect.poll(async () => {
        const polled = await bff.get(`${path}/export-jobs/${job.jobId}`)
        const body = await polled.json() as { status?: string }
        return body.status
      }, { timeout: 15_000 }).toBe('READY')
      const csv = await bff.get(`${path}/export-jobs/${job.jobId}/content`)
      expect(csv.status()).toBe(200)
      expect(await csv.text()).toContain('paymentOrderId')
    })
  } finally {
    await bff.dispose()
  }
})
