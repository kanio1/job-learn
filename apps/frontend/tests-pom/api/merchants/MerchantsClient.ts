import type { APIRequestContext } from '@playwright/test'
import { z } from 'zod'
import { BffTransport } from '../BffTransport'
import type { JsonResult } from '../contracts/http-result'
import { problemDetailsSchema, type ProblemDetails } from '../../utils/problem'

export const merchantSchema = z.object({
  merchantId: z.string(),
  merchantReference: z.string(),
  displayName: z.string(),
  status: z.string(),
  version: z.number().optional(),
  contactPhone: z.string().nullable().optional(),
  contactAddress: z.string().nullable().optional(),
}).passthrough()

export type Merchant = z.infer<typeof merchantSchema>

const merchantListSchema = z.object({
  content: z.array(z.object({
    merchantId: z.string(),
    merchantReference: z.string(),
    status: z.string(),
  }).passthrough()),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  totalPages: z.number(),
}).passthrough()
const merchantPatchSchema = merchantSchema.extend({ version: z.number() })
const merchantErrorSchema = z.object({ error: z.string() }).passthrough()
const importPreviewSchema = z.object({
  previewId: z.string(),
  validCount: z.number(),
  warningCount: z.number(),
  rejectedCount: z.number(),
  rows: z.array(z.object({ status: z.string(), reason: z.string().nullable() }).passthrough()),
}).passthrough()
const importCommitSchema = z.object({ createdCount: z.number() }).passthrough()
const searchSchema = z.object({
  merchants: z.array(z.object({ merchantId: z.string(), merchantReference: z.string(), displayName: z.string() }).passthrough()),
  payments: z.array(z.object({ paymentOrderId: z.string(), merchantId: z.string(), clientOrderReference: z.string() }).passthrough()),
}).passthrough()
const orgTreeSchema = z.object({
  nodes: z.array(z.object({ id: z.string(), type: z.string(), label: z.string(), reference: z.string(), lazy: z.boolean() }).passthrough()),
}).passthrough()

export type MerchantFailure = ProblemDetails | z.infer<typeof merchantErrorSchema>
export type MerchantResult<T> = JsonResult<T, MerchantFailure>

type MerchantPatch = {
  displayName?: string
  contactPhone?: string | null
  contactAddress?: string | null
}

function queryString(query: Record<string, string | number | undefined>): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== '') {
      params.set(key, String(value))
    }
  }
  const encoded = params.toString()
  return encoded === '' ? '' : `?${encoded}`
}

function ifMatchHeader(ifMatch: string | undefined): Record<string, string> {
  return ifMatch === undefined ? {} : { 'If-Match': ifMatch }
}

/** Merchant-registry BFF contract. This client knows no fixtures or UI objects. */
export class MerchantsClient {
  constructor(
    private readonly context: APIRequestContext,
    private readonly transport: BffTransport,
  ) {}

  async get(merchantId: string): Promise<JsonResult<Merchant, ProblemDetails>> {
    const endpoint = `GET /api/merchants/${merchantId}`
    return this.transport.requestJson(
      endpoint,
      () => this.context.get(`/api/merchants/${merchantId}`),
      {
        success: { statuses: [200], schema: merchantSchema },
        error: { statuses: [400, 401, 403, 404], schema: problemDetailsSchema },
      },
    )
  }

  async list(query: Record<string, string | number | undefined> = {}): Promise<MerchantResult<z.infer<typeof merchantListSchema>>> {
    const endpoint = `GET /api/merchants${queryString(query)}`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: merchantListSchema },
      error: { statuses: [400, 401, 403], schema: problemDetailsSchema },
    })
  }

  async create(
    merchantReference: string,
    displayName: string,
    tenantReference: string | null = 'TENANT_ALPHA',
  ): Promise<MerchantResult<Merchant>> {
    const data = tenantReference === null
      ? { merchantReference, displayName }
      : { merchantReference, displayName, tenantReference }
    return this.transport.requestJson('POST /api/merchants', () => this.context.post('/api/merchants', { data }), {
      success: { statuses: [201], schema: merchantSchema },
      error: { statuses: [400, 401, 403, 409], schema: z.union([problemDetailsSchema, merchantErrorSchema]) },
    })
  }

  async patch(merchantId: string, body: MerchantPatch, ifMatch?: string): Promise<MerchantResult<z.infer<typeof merchantPatchSchema>>> {
    const endpoint = `PATCH /api/merchants/${merchantId}`
    return this.transport.requestJson(
      endpoint,
      () => this.context.patch(`/api/merchants/${merchantId}`, { data: body, headers: ifMatchHeader(ifMatch) }),
      {
        success: { statuses: [200], schema: merchantPatchSchema },
        error: { statuses: [400, 401, 403, 404, 409, 412, 428], schema: z.union([problemDetailsSchema, merchantErrorSchema]) },
      },
    )
  }

  async patchDisplayName(merchantId: string, displayName: string, ifMatch?: string): Promise<MerchantResult<z.infer<typeof merchantPatchSchema>>> {
    return this.patch(merchantId, { displayName }, ifMatch)
  }

  async activate(merchantId: string, ifMatch?: string): Promise<MerchantResult<Merchant>> {
    const endpoint = `POST /api/merchants/${merchantId}/activate`
    return this.transport.requestJson(
      endpoint,
      () => this.context.post(`/api/merchants/${merchantId}/activate`, { headers: ifMatchHeader(ifMatch) }),
      {
        success: { statuses: [200], schema: merchantSchema },
        error: { statuses: [400, 401, 403, 404, 409, 412, 428], schema: z.union([problemDetailsSchema, merchantErrorSchema]) },
      },
    )
  }

  async suspend(merchantId: string, ifMatch?: string): Promise<MerchantResult<Merchant>> {
    const endpoint = `POST /api/merchants/${merchantId}/suspend`
    return this.transport.requestJson(
      endpoint,
      () => this.context.post(`/api/merchants/${merchantId}/suspend`, { headers: ifMatchHeader(ifMatch) }),
      {
        success: { statuses: [200], schema: merchantSchema },
        error: { statuses: [400, 401, 403, 404, 409, 412, 428], schema: z.union([problemDetailsSchema, merchantErrorSchema]) },
      },
    )
  }

  async previewImport(file: { name: string, mimeType: string, buffer: Buffer }): Promise<MerchantResult<z.infer<typeof importPreviewSchema>>> {
    return this.transport.requestJson(
      'POST /api/merchants/import/preview',
      () => this.context.post('/api/merchants/import/preview', { multipart: { file } }),
      {
        success: { statuses: [200], schema: importPreviewSchema },
        error: { statuses: [400, 401, 403, 409, 422], schema: z.union([problemDetailsSchema, merchantErrorSchema]) },
      },
    )
  }

  async commitImport(previewId: string): Promise<MerchantResult<z.infer<typeof importCommitSchema>>> {
    return this.transport.requestJson(
      'POST /api/merchants/import/commit',
      () => this.context.post('/api/merchants/import/commit', { data: { previewId } }),
      {
        success: { statuses: [200], schema: importCommitSchema },
        error: { statuses: [400, 401, 403, 404, 409, 422], schema: z.union([problemDetailsSchema, merchantErrorSchema]) },
      },
    )
  }

  async search(q: string): Promise<MerchantResult<z.infer<typeof searchSchema>>> {
    const endpoint = `GET /api/search?q=${encodeURIComponent(q)}`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: searchSchema },
      error: { statuses: [400, 401, 403], schema: problemDetailsSchema },
    })
  }

  async orgTree(parent?: string): Promise<MerchantResult<z.infer<typeof orgTreeSchema>>> {
    const endpoint = parent === undefined ? 'GET /api/org-tree' : `GET /api/org-tree?parent=${encodeURIComponent(parent)}`
    const url = parent === undefined ? '/api/org-tree' : `/api/org-tree?parent=${encodeURIComponent(parent)}`
    return this.transport.requestJson(endpoint, () => this.context.get(url), {
      success: { statuses: [200], schema: orgTreeSchema },
      error: { statuses: [400, 401, 403], schema: problemDetailsSchema },
    })
  }
}
