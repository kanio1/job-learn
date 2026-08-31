import type { APIRequestContext } from '@playwright/test'
import { z } from 'zod'
import { BffTransport } from '../BffTransport'
import type { JsonResult } from '../contracts/http-result'
import { problemDetailsSchema, type ProblemDetails } from '../../utils/problem'

const rlsItemsSchema = z.object({
  items: z.array(z.object({ itemId: z.string(), label: z.string() }).passthrough()),
}).passthrough()
const rlsItemSchema = z.object({ itemId: z.string(), label: z.string() }).passthrough()
const rlsCompareSchema = z.object({
  bypassRoleCount: z.number(),
  restrictedWithoutTenantGuc: z.number(),
  unprotected: z.number(),
}).passthrough()
const eventLabRowSchema = z.object({
  eventId: z.string(),
  id: z.string(),
  targetId: z.string(),
  status: z.enum(['PROCESSED', 'RETRYING', 'DEAD']),
}).passthrough()
const eventLabRowsSchema = z.array(eventLabRowSchema)
const eventLabInjectSchema = z.object({ eventId: z.string() }).passthrough()

export type EventLabListRow = z.infer<typeof eventLabRowSchema>
export type LabsResult<T> = JsonResult<T, ProblemDetails>

/** Isolated test-lab BFF contracts (RLS and Event Lab). */
export class LabsClient {
  constructor(
    private readonly context: APIRequestContext,
    private readonly transport: BffTransport,
  ) {}

  async listRlsItems(): Promise<LabsResult<z.infer<typeof rlsItemsSchema>>> {
    return this.transport.requestJson('GET /api/rls-lab/items', () => this.context.get('/api/rls-lab/items'), {
      success: { statuses: [200], schema: rlsItemsSchema },
      error: { statuses: [401, 403], schema: problemDetailsSchema },
    })
  }

  async getRlsItem(itemId: string): Promise<LabsResult<z.infer<typeof rlsItemSchema>>> {
    const path = `/api/rls-lab/items/${encodeURIComponent(itemId)}`
    return this.transport.requestJson(`GET ${path}`, () => this.context.get(path), {
      success: { statuses: [200], schema: rlsItemSchema },
      error: { statuses: [401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async rlsCompare(): Promise<LabsResult<z.infer<typeof rlsCompareSchema>>> {
    return this.transport.requestJson('GET /api/rls-lab/compare', () => this.context.get('/api/rls-lab/compare'), {
      success: { statuses: [200], schema: rlsCompareSchema },
      error: { statuses: [401, 403], schema: problemDetailsSchema },
    })
  }

  async listEventLab(query: Record<string, string | undefined> = {}): Promise<LabsResult<EventLabListRow[]>> {
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries(query)) if (value) params.set(key, value)
    const path = `/api/event-lab${params.size === 0 ? '' : `?${params.toString()}`}`
    return this.transport.requestJson(`GET ${path}`, () => this.context.get(path), {
      success: { statuses: [200], schema: eventLabRowsSchema },
      error: { statuses: [400, 401, 403], schema: problemDetailsSchema },
    })
  }

  async getEventLabDetail(id: string): Promise<LabsResult<EventLabListRow>> {
    const path = `/api/event-lab/${encodeURIComponent(id)}`
    return this.transport.requestJson(`GET ${path}`, () => this.context.get(path), {
      success: { statuses: [200], schema: eventLabRowSchema },
      error: { statuses: [401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async injectDuplicate(eventId: string): Promise<LabsResult<z.infer<typeof eventLabInjectSchema>>> {
    return this.transport.requestJson('POST /api/event-lab/inject/duplicate', () => this.context.post('/api/event-lab/inject/duplicate', { data: { eventId } }), {
      success: { statuses: [201], schema: eventLabInjectSchema },
      error: { statuses: [400, 401, 403], schema: problemDetailsSchema },
    })
  }

  async injectPoison(eventId: string): Promise<LabsResult<z.infer<typeof eventLabInjectSchema>>> {
    return this.transport.requestJson('POST /api/event-lab/inject/poison', () => this.context.post('/api/event-lab/inject/poison', { data: { eventId } }), {
      success: { statuses: [201], schema: eventLabInjectSchema },
      error: { statuses: [400, 401, 403], schema: problemDetailsSchema },
    })
  }
}
