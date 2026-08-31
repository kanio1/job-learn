import type { APIRequestContext } from '@playwright/test'
import { z } from 'zod'
import { BffTransport } from '../BffTransport'
import type { EmptyResult, JsonResult } from '../contracts/http-result'
import { problemDetailsSchema, type ProblemDetails } from '../../utils/problem'

const expirationSchema = z.object({ expiredCount: z.number() }).passthrough()
const auditListSchema = z.object({
  content: z.array(z.object({ id: z.string(), action: z.string(), actorDisplay: z.string(), targetType: z.string() }).passthrough()),
  totalElements: z.number(),
}).passthrough()
const auditEntrySchema = z.object({ id: z.string(), action: z.string() }).passthrough()
const supportCaseSchema = z.object({
  caseId: z.string(),
  caseReference: z.string(),
  status: z.string(),
  version: z.number(),
  assigneeSubject: z.string().nullable(),
}).passthrough()
const supportListSchema = z.object({
  content: z.array(z.object({ caseId: z.string(), status: z.string(), caseReference: z.string() }).passthrough()),
}).passthrough()
const bulkAssignSchema = z.object({
  succeeded: z.number(),
  failed: z.array(z.object({ caseId: z.string(), caseReference: z.string(), error: z.string() }).passthrough()),
}).passthrough()
const opsFeedSchema = z.object({
  eventId: z.string().nullable(),
  occurredAt: z.string().nullable(),
  merchantId: z.string().nullable(),
  paymentOrderId: z.string().nullable(),
  type: z.string().nullable(),
  label: z.string().nullable(),
  malformed: z.boolean(),
}).passthrough()
const notificationsSchema = z.object({
  content: z.array(z.object({
    notificationId: z.string(), eventId: z.string(), eventType: z.string(), title: z.string(), body: z.string(), readAt: z.string().nullable(),
  }).passthrough()),
}).passthrough()
const notificationSchema = z.object({ notificationId: z.string(), readAt: z.string().nullable() }).passthrough()

type QueryValue = string | number | undefined
type SupportCaseDraft = {
  merchantId: string
  title: string
  paymentOrderId?: string
  priority?: string
  caseReference?: string
  assigneeSubject?: string
}
type OpsFeedDraft = {
  eventId?: string
  occurredAt?: string
  merchantId?: string
  paymentOrderId?: string
  type?: string
  label?: string
  raw?: string
}

function queryString(query: Record<string, QueryValue>): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== '') params.set(key, String(value))
  }
  const encoded = params.toString()
  return encoded === '' ? '' : `?${encoded}`
}

function ifMatchHeader(ifMatch: string | undefined): Record<string, string> {
  return ifMatch === undefined ? {} : { 'If-Match': ifMatch }
}

export type OperationsResult<T> = JsonResult<T, ProblemDetails>

/** Operations-domain BFF contracts: support, audit, expiry, feed, notifications. */
export class OperationsClient {
  constructor(
    private readonly context: APIRequestContext,
    private readonly transport: BffTransport,
  ) {}

  async runExpirationSweep(): Promise<OperationsResult<z.infer<typeof expirationSchema>>> {
    return this.transport.requestJson('POST /api/payment-ops/expiration-sweep', () => this.context.post('/api/payment-ops/expiration-sweep', { data: {} }), {
      success: { statuses: [200], schema: expirationSchema },
      error: { statuses: [401, 403], schema: problemDetailsSchema },
    })
  }

  async listAudit(query: Record<string, QueryValue> = {}): Promise<OperationsResult<z.infer<typeof auditListSchema>>> {
    const endpoint = `GET /api/audit${queryString(query)}`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: auditListSchema },
      error: { statuses: [400, 401, 403], schema: problemDetailsSchema },
    })
  }

  async getAuditEntry(eventId: string): Promise<OperationsResult<z.infer<typeof auditEntrySchema>>> {
    const endpoint = `GET /api/audit/${encodeURIComponent(eventId)}`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: auditEntrySchema },
      error: { statuses: [401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async createCase(payload: SupportCaseDraft): Promise<OperationsResult<z.infer<typeof supportCaseSchema>>> {
    return this.transport.requestJson('POST /api/support/cases', () => this.context.post('/api/support/cases', { data: payload }), {
      success: { statuses: [201], schema: supportCaseSchema },
      error: { statuses: [400, 401, 403, 404, 422], schema: problemDetailsSchema },
    })
  }

  async getCase(caseId: string): Promise<OperationsResult<z.infer<typeof supportCaseSchema>>> {
    const endpoint = `GET /api/support/cases/${caseId}`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: supportCaseSchema },
      error: { statuses: [401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async listCases(query: Record<string, string | undefined> = {}): Promise<OperationsResult<z.infer<typeof supportListSchema>>> {
    const endpoint = `GET /api/support/cases${queryString(query)}`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: supportListSchema },
      error: { statuses: [400, 401, 403], schema: problemDetailsSchema },
    })
  }

  async patchCase(caseId: string, payload: { status?: string, assigneeSubject?: string | null }, ifMatch?: string): Promise<OperationsResult<z.infer<typeof supportCaseSchema>>> {
    const endpoint = `PATCH /api/support/cases/${caseId}`
    return this.transport.requestJson(endpoint, () => this.context.patch(endpoint.replace('PATCH ', ''), { data: payload, headers: ifMatchHeader(ifMatch) }), {
      success: { statuses: [200], schema: supportCaseSchema },
      error: { statuses: [400, 401, 403, 404, 409, 412, 428], schema: problemDetailsSchema },
    })
  }

  async bulkAssignCases(caseIds: string[], assigneeSubject: string): Promise<OperationsResult<z.infer<typeof bulkAssignSchema>>> {
    return this.transport.requestJson('POST /api/support/cases/bulk-assign', () => this.context.post('/api/support/cases/bulk-assign', { data: { caseIds, assigneeSubject } }), {
      success: { statuses: [200], schema: bulkAssignSchema },
      error: { statuses: [400, 401, 403, 422], schema: problemDetailsSchema },
    })
  }

  async injectFeed(payload: OpsFeedDraft): Promise<OperationsResult<z.infer<typeof opsFeedSchema>>> {
    return this.transport.requestJson('POST /api/ops/feed/inject', () => this.context.post('/api/ops/feed/inject', { data: payload }), {
      success: { statuses: [201], schema: opsFeedSchema },
      error: { statuses: [400, 401, 403], schema: problemDetailsSchema },
    })
  }

  async disconnectFeed(): Promise<EmptyResult> {
    const endpoint = 'POST /api/ops/feed/disconnect-me'
    const result = await this.transport.requestJson(endpoint, () => this.context.post('/api/ops/feed/disconnect-me'), {
      success: { statuses: [], schema: z.never() },
      error: { statuses: [401, 403], schema: problemDetailsSchema },
      empty: { statuses: [204] },
    })
    if (result.kind !== 'empty') throw new Error(`${endpoint} must return an empty response`)
    return result
  }

  async listNotifications(unreadOnly = false): Promise<OperationsResult<z.infer<typeof notificationsSchema>>> {
    const endpoint = unreadOnly ? 'GET /api/notifications?unreadOnly=true' : 'GET /api/notifications'
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: notificationsSchema },
      error: { statuses: [401, 403], schema: problemDetailsSchema },
    })
  }

  async markNotificationRead(notificationId: string): Promise<OperationsResult<z.infer<typeof notificationSchema>>> {
    const endpoint = `POST /api/notifications/${notificationId}/read`
    return this.transport.requestJson(endpoint, () => this.context.post(endpoint.replace('POST ', '')), {
      success: { statuses: [200], schema: notificationSchema },
      error: { statuses: [401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async markAllNotificationsRead(): Promise<EmptyResult> {
    const endpoint = 'POST /api/notifications/read-all'
    const result = await this.transport.requestJson(endpoint, () => this.context.post('/api/notifications/read-all'), {
      success: { statuses: [], schema: z.never() },
      error: { statuses: [401, 403], schema: problemDetailsSchema },
      empty: { statuses: [204] },
    })
    if (result.kind !== 'empty') throw new Error(`${endpoint} must return an empty response`)
    return result
  }
}
