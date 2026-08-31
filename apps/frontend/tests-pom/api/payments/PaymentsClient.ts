import type { APIRequestContext } from '@playwright/test'
import { z } from 'zod'
import { BffTransport } from '../BffTransport'
import type { BinaryResult, EmptyResult, JsonResult, TextResult } from '../contracts/http-result'
import { problemDetailsSchema, type ProblemDetails } from '../../utils/problem'

const paymentOrderSchema = z.object({
  paymentOrderId: z.string(),
  status: z.string(),
  clientOrderReference: z.string(),
  expiresAt: z.string().nullable(),
}).passthrough()
const paymentOrderListSchema = z.object({
  content: z.array(z.object({
    paymentOrderId: z.string(),
    clientOrderReference: z.string(),
    amountMinor: z.number(),
    status: z.string(),
    currency: z.string(),
    createdAt: z.string(),
  }).passthrough()),
  page: z.number(),
  totalElements: z.number(),
}).passthrough()
const paymentStatusSchema = z.object({ status: z.string() }).passthrough()
const paymentSummarySchema = z.object({
  totalOrders: z.number(),
  totalAmountMinor: z.number(),
  byStatus: z.array(z.object({ status: z.string(), orderCount: z.number() }).passthrough()),
}).passthrough()
const paymentHistorySchema = z.object({
  content: z.array(z.object({ fromStatus: z.string().nullable(), toStatus: z.string(), action: z.string().nullable() }).passthrough()),
}).passthrough()
const refundApprovalSchema = z.object({ approvalId: z.string(), status: z.string() }).passthrough()
const noteSchema = z.object({ id: z.string(), body: z.string() }).passthrough()
const notesSchema = z.array(noteSchema)
const challengeSchema = z.object({ challengeId: z.string(), pin: z.string(), expiresAt: z.string() }).passthrough()
const challengeVerificationSchema = z.object({ challengeId: z.string(), verifiedAt: z.string() }).passthrough()
const evidenceSchema = z.object({ evidenceId: z.string(), category: z.string() }).passthrough()
const exportJobSchema = z.object({ jobId: z.string(), status: z.string() }).passthrough()

export type PaymentOrder = z.infer<typeof paymentOrderSchema>
export type PaymentResult<T> = JsonResult<T, ProblemDetails>

type PaymentOrderDraft = {
  amountMinor: number
  currency: string
  clientOrderReference: string
}
type PaymentOrderPatch = { metadata?: { note?: string } }
type PaymentQuery = Record<string, string | number | undefined>
type PaymentHeaders = Record<string, string>

function queryString(query: PaymentQuery): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== '') {
      params.set(key, String(value))
    }
  }
  const encoded = params.toString()
  return encoded === '' ? '' : `?${encoded}`
}

function lifecycleHeaders(idempotencyKey: string, etag: string | undefined): PaymentHeaders {
  return etag === undefined
    ? { 'Idempotency-Key': idempotencyKey }
    : { 'Idempotency-Key': idempotencyKey, 'If-Match': etag }
}

function mergePatchHeaders(ifMatch: string | undefined): PaymentHeaders {
  return ifMatch === undefined
    ? { 'Content-Type': 'application/merge-patch+json' }
    : { 'Content-Type': 'application/merge-patch+json', 'If-Match': ifMatch }
}

/** Payment-order and lifecycle BFF contracts. */
export class PaymentsClient {
  constructor(
    private readonly context: APIRequestContext,
    private readonly transport: BffTransport,
  ) {}

  async createOrder(
    merchantId: string,
    payload: PaymentOrderDraft,
    idempotencyKey?: string | null,
    correlationId?: string,
  ): Promise<PaymentResult<PaymentOrder>> {
    const headers: PaymentHeaders = {}
    if (idempotencyKey !== undefined && idempotencyKey !== null) headers['Idempotency-Key'] = idempotencyKey
    if (correlationId !== undefined) headers['X-Correlation-ID'] = correlationId
    const endpoint = `POST /api/merchants/${merchantId}/payment-orders`
    return this.transport.requestJson(
      endpoint,
      () => this.context.post(`/api/merchants/${merchantId}/payment-orders`, { data: payload, headers }),
      {
        success: { statuses: [200, 201], schema: paymentOrderSchema },
        error: { statuses: [400, 401, 403, 404, 409, 422], schema: problemDetailsSchema },
      },
    )
  }

  async get(
    merchantId: string,
    paymentOrderId: string,
  ): Promise<PaymentResult<PaymentOrder>>
  async get(
    merchantId: string,
    paymentOrderId: string,
    headers: PaymentHeaders,
  ): Promise<PaymentResult<PaymentOrder> | EmptyResult>
  async get(
    merchantId: string,
    paymentOrderId: string,
    headers: PaymentHeaders = {},
  ): Promise<PaymentResult<PaymentOrder> | EmptyResult> {
    const endpoint = `GET /api/merchants/${merchantId}/payment-orders/${paymentOrderId}`
    return this.transport.requestJson(
      endpoint,
      () => this.context.get(`/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`, Object.keys(headers).length === 0 ? undefined : { headers }),
      {
        success: { statuses: [200], schema: paymentOrderSchema },
        error: { statuses: [400, 401, 403, 404], schema: problemDetailsSchema },
        empty: { statuses: [304] },
      },
    )
  }

  async head(merchantId: string, paymentOrderId: string): Promise<EmptyResult> {
    const endpoint = `HEAD /api/merchants/${merchantId}/payment-orders/${paymentOrderId}`
    const result = await this.transport.requestJson(
      endpoint,
      () => this.context.head(`/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`),
      {
        success: { statuses: [], schema: z.never() },
        error: { statuses: [400, 401, 403, 404], schema: problemDetailsSchema },
        empty: { statuses: [200] },
      },
    )
    if (result.kind !== 'empty') throw new Error(`${endpoint} must return an empty response`)
    return result
  }

  async list(merchantId: string, query: PaymentQuery = {}): Promise<PaymentResult<z.infer<typeof paymentOrderListSchema>>> {
    const endpoint = `GET /api/merchants/${merchantId}/payment-orders${queryString(query)}`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: paymentOrderListSchema },
      error: { statuses: [400, 401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async authorize(merchantId: string, paymentOrderId: string, etag: string | undefined, idempotencyKey: string): Promise<PaymentResult<z.infer<typeof paymentStatusSchema>>> {
    return this.lifecycle(merchantId, paymentOrderId, 'authorize', etag, idempotencyKey)
  }

  async capture(merchantId: string, paymentOrderId: string, etag: string | undefined, idempotencyKey: string, amountMinor: number): Promise<PaymentResult<z.infer<typeof paymentStatusSchema>>> {
    return this.lifecycle(merchantId, paymentOrderId, 'capture', etag, idempotencyKey, { amountMinor })
  }

  async cancel(merchantId: string, paymentOrderId: string, etag: string | undefined, idempotencyKey: string): Promise<PaymentResult<z.infer<typeof paymentStatusSchema>>> {
    return this.lifecycle(merchantId, paymentOrderId, 'cancel', etag, idempotencyKey)
  }

  async refund(merchantId: string, paymentOrderId: string, etag: string, idempotencyKey: string, amountMinor = 1): Promise<PaymentResult<z.infer<typeof paymentStatusSchema>>> {
    return this.lifecycle(merchantId, paymentOrderId, 'refund', etag, idempotencyKey, { amountMinor })
  }

  async summary(merchantId: string, query: Record<string, string | undefined> = {}): Promise<PaymentResult<z.infer<typeof paymentSummarySchema>>> {
    const endpoint = `GET /api/merchants/${merchantId}/payment-orders/summary${queryString(query)}`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: paymentSummarySchema },
      error: { statuses: [400, 401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async history(merchantId: string, paymentOrderId: string): Promise<PaymentResult<z.infer<typeof paymentHistorySchema>>> {
    const endpoint = `GET /api/merchants/${merchantId}/payment-orders/${paymentOrderId}/history`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: paymentHistorySchema },
      error: { statuses: [400, 401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async createRefundApproval(merchantId: string, paymentOrderId: string, payload: { amountMinor?: number, reason?: string } = {}): Promise<PaymentResult<z.infer<typeof refundApprovalSchema>>> {
    const endpoint = `POST /api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-approvals`
    return this.transport.requestJson(endpoint, () => this.context.post(`/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-approvals`, { data: payload }), {
      success: { statuses: [201], schema: refundApprovalSchema },
      error: { statuses: [400, 401, 403, 404, 409, 422], schema: problemDetailsSchema },
    })
  }

  async approveRefundApproval(merchantId: string, paymentOrderId: string, approvalId: string, etag: string | undefined, idempotencyKey: string): Promise<PaymentResult<z.infer<typeof paymentStatusSchema>>> {
    const endpoint = `POST /api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-approvals/${approvalId}/approve`
    return this.transport.requestJson(endpoint, () => this.context.post(`/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-approvals/${approvalId}/approve`, {
      data: {}, headers: lifecycleHeaders(idempotencyKey, etag),
    }), {
      success: { statuses: [200], schema: paymentStatusSchema },
      error: { statuses: [400, 401, 403, 404, 409, 412, 422, 428], schema: problemDetailsSchema },
    })
  }

  async listNotes(merchantId: string, paymentOrderId: string): Promise<PaymentResult<z.infer<typeof notesSchema>>> {
    const endpoint = `GET /api/merchants/${merchantId}/payment-orders/${paymentOrderId}/notes`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: notesSchema },
      error: { statuses: [401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async postNote(merchantId: string, paymentOrderId: string, body: string): Promise<PaymentResult<z.infer<typeof noteSchema>>> {
    const endpoint = `POST /api/merchants/${merchantId}/payment-orders/${paymentOrderId}/notes`
    return this.transport.requestJson(endpoint, () => this.context.post(endpoint.replace('POST ', ''), { data: { body } }), {
      success: { statuses: [201], schema: noteSchema },
      error: { statuses: [400, 401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async getEvidence(merchantId: string, paymentOrderId: string, evidenceId: string): Promise<BinaryResult> {
    const endpoint = `GET /api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence/${evidenceId}`
    return this.transport.requestBinary(endpoint, () => this.context.get(endpoint.replace('GET ', '')))
  }

  async createChallenge(merchantId: string, paymentOrderId: string): Promise<PaymentResult<z.infer<typeof challengeSchema>>> {
    const endpoint = `POST /api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-challenges`
    return this.transport.requestJson(endpoint, () => this.context.post(endpoint.replace('POST ', ''), { data: {} }), {
      success: { statuses: [201], schema: challengeSchema },
      error: { statuses: [400, 401, 403, 404, 409], schema: problemDetailsSchema },
    })
  }

  async verifyChallenge(merchantId: string, paymentOrderId: string, challengeId: string, pin: string): Promise<PaymentResult<z.infer<typeof challengeVerificationSchema>>> {
    const endpoint = `POST /api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-challenges/${challengeId}/verify`
    return this.transport.requestJson(endpoint, () => this.context.post(endpoint.replace('POST ', ''), { data: { pin } }), {
      success: { statuses: [200], schema: challengeVerificationSchema },
      error: { statuses: [400, 401, 403, 404, 409, 422], schema: problemDetailsSchema },
    })
  }

  async patch(merchantId: string, paymentOrderId: string, data: PaymentOrderPatch, ifMatch?: string): Promise<PaymentResult<z.infer<typeof paymentStatusSchema>>> {
    const headers = mergePatchHeaders(ifMatch)
    const endpoint = `PATCH /api/merchants/${merchantId}/payment-orders/${paymentOrderId}`
    return this.transport.requestJson(endpoint, () => this.context.patch(`/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`, { data, headers }), {
      success: { statuses: [200], schema: paymentStatusSchema },
      error: { statuses: [400, 401, 403, 404, 412, 428], schema: problemDetailsSchema },
    })
  }

  async uploadEvidence(merchantId: string, paymentOrderId: string, file: { name: string, mimeType: string, buffer: Buffer }, category = 'RECEIPT'): Promise<PaymentResult<z.infer<typeof evidenceSchema>>> {
    const endpoint = `POST /api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence`
    return this.transport.requestJson(endpoint, () => this.context.post(endpoint.replace('POST ', ''), { multipart: { file, category } }), {
      success: { statuses: [201], schema: evidenceSchema },
      error: { statuses: [400, 401, 403, 404, 413, 422], schema: problemDetailsSchema },
    })
  }

  async createExportJob(merchantId: string): Promise<PaymentResult<z.infer<typeof exportJobSchema>>> {
    const endpoint = `POST /api/merchants/${merchantId}/payment-orders/export-jobs`
    return this.transport.requestJson(endpoint, () => this.context.post(endpoint.replace('POST ', '')), {
      success: { statuses: [202], schema: exportJobSchema },
      error: { statuses: [401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async getExportJob(merchantId: string, jobId: string): Promise<PaymentResult<z.infer<typeof exportJobSchema>>> {
    const endpoint = `GET /api/merchants/${merchantId}/payment-orders/export-jobs/${jobId}`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: exportJobSchema },
      error: { statuses: [401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async getExportJobContent(merchantId: string, jobId: string): Promise<TextResult> {
    const endpoint = `GET /api/merchants/${merchantId}/payment-orders/export-jobs/${jobId}/content`
    return this.transport.requestText(endpoint, () => this.context.get(endpoint.replace('GET ', '')))
  }

  private async lifecycle(
    merchantId: string,
    paymentOrderId: string,
    action: 'authorize' | 'capture' | 'cancel' | 'refund',
    etag: string | undefined,
    idempotencyKey: string,
    data: { amountMinor?: number } = {},
  ): Promise<PaymentResult<z.infer<typeof paymentStatusSchema>>> {
    const endpoint = `POST /api/merchants/${merchantId}/payment-orders/${paymentOrderId}/${action}`
    return this.transport.requestJson(endpoint, () => this.context.post(endpoint.replace('POST ', ''), { data, headers: lifecycleHeaders(idempotencyKey, etag) }), {
      success: { statuses: [200], schema: paymentStatusSchema },
      error: { statuses: [400, 401, 403, 404, 409, 412, 422, 428], schema: problemDetailsSchema },
    })
  }
}
