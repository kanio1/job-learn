import type { APIRequestContext } from '@playwright/test'
import { z } from 'zod'
import { BffTransport } from '../BffTransport'
import type { EmptyResult, JsonResult } from '../contracts/http-result'
import { problemDetailsSchema, type ProblemDetails } from '../../utils/problem'

const paymentPolicySchema = z.object({
  autoCapture: z.boolean(),
  maxAutoCaptureMinor: z.number(),
  riskThreshold: z.number(),
  refundPolicy: z.enum(['MANUAL', 'AUTOMATIC']),
}).passthrough()
export const tenantSettingsSchema = z.object({
  contactEmail: z.string().nullable().optional(),
  timezone: z.string().optional(),
  webhookBaseUrl: z.string().nullable().optional(),
  paymentPolicy: paymentPolicySchema.optional(),
}).passthrough()
const tenantSettingsUpdateSchema = tenantSettingsSchema.partial()
const userSchema = z.object({ id: z.string(), username: z.string(), enabled: z.boolean(), roles: z.array(z.string()) }).passthrough()
const userListSchema = z.object({ users: z.array(userSchema) }).passthrough()
const userRolesSchema = z.object({ id: z.string(), username: z.string(), roles: z.array(z.string()) }).passthrough()
const paymentViewSchema = z.object({
  id: z.string(),
  name: z.string(),
  resource: z.string(),
  filters: z.record(z.string(), z.union([z.string(), z.number(), z.boolean()])),
  columns: z.array(z.string()),
  isDefault: z.boolean(),
}).passthrough()
const paymentViewsSchema = z.object({ content: z.array(paymentViewSchema) }).passthrough()
const paymentViewUpdateSchema = z.object({ id: z.string(), name: z.string() }).passthrough()
const paymentViewDefaultSchema = z.object({ id: z.string(), isDefault: z.boolean() }).passthrough()

export type TenantSettingsBody = z.infer<typeof tenantSettingsSchema>
export type TenantSettingsUpdate = z.infer<typeof tenantSettingsUpdateSchema>
export type PaymentViewFilters = z.infer<typeof paymentViewSchema>['filters']
export type IdentityResult<T> = JsonResult<T, ProblemDetails>

function ifMatchHeader(ifMatch: string | undefined): Record<string, string> {
  return ifMatch === undefined ? {} : { 'If-Match': ifMatch }
}

/** Identity and user-owned settings/view BFF contracts. */
export class IdentityClient {
  constructor(
    private readonly context: APIRequestContext,
    private readonly transport: BffTransport,
  ) {}

  async getTenantSettings(): Promise<IdentityResult<TenantSettingsBody>> {
    return this.transport.requestJson('GET /api/tenants/current/settings', () => this.context.get('/api/tenants/current/settings'), {
      success: { statuses: [200], schema: tenantSettingsSchema },
      error: { statuses: [401, 403], schema: problemDetailsSchema },
    })
  }

  async updateTenantSettings(settings: TenantSettingsUpdate, ifMatch?: string): Promise<IdentityResult<TenantSettingsBody>> {
    return this.transport.requestJson('PATCH /api/tenants/current/settings', () => this.context.patch('/api/tenants/current/settings', {
      data: settings, headers: ifMatchHeader(ifMatch),
    }), {
      success: { statuses: [200], schema: tenantSettingsSchema },
      error: { statuses: [400, 401, 403, 412, 428], schema: problemDetailsSchema },
    })
  }

  async listUsers(query: { search?: string, status?: 'enabled' | 'disabled', role?: string } = {}): Promise<IdentityResult<z.infer<typeof userListSchema>>> {
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries(query)) if (value !== undefined && value !== '') params.set(key, value)
    const endpoint = `GET /api/users${params.size === 0 ? '' : `?${params.toString()}`}`
    return this.transport.requestJson(endpoint, () => this.context.get(endpoint.replace('GET ', '')), {
      success: { statuses: [200], schema: userListSchema },
      error: { statuses: [400, 401, 403], schema: problemDetailsSchema },
    })
  }

  async createUser(payload: { username: string, email: string, temporaryPassword: string, tenantId?: string, roles: string[] }): Promise<IdentityResult<z.infer<typeof userSchema>>> {
    return this.transport.requestJson('POST /api/users', () => this.context.post('/api/users', { data: payload }), {
      success: { statuses: [201], schema: userSchema },
      error: { statuses: [400, 401, 403, 409], schema: problemDetailsSchema },
    })
  }

  async updateUser(userId: string, payload: { enabled?: boolean, email?: string }): Promise<IdentityResult<z.infer<typeof userSchema>>> {
    const endpoint = `PATCH /api/users/${encodeURIComponent(userId)}`
    return this.transport.requestJson(endpoint, () => this.context.patch(endpoint.replace('PATCH ', ''), { data: payload }), {
      success: { statuses: [200], schema: userSchema },
      error: { statuses: [400, 401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async assignUserRoles(userId: string, payload: { assign: string[], remove: string[] }): Promise<IdentityResult<z.infer<typeof userRolesSchema>>> {
    const endpoint = `POST /api/users/${encodeURIComponent(userId)}/roles`
    return this.transport.requestJson(endpoint, () => this.context.post(endpoint.replace('POST ', ''), { data: payload }), {
      success: { statuses: [200], schema: userRolesSchema },
      error: { statuses: [400, 401, 403, 404], schema: problemDetailsSchema },
    })
  }

  async listPaymentViews(): Promise<IdentityResult<z.infer<typeof paymentViewsSchema>>> {
    return this.transport.requestJson('GET /api/users/me/payment-views', () => this.context.get('/api/users/me/payment-views'), {
      success: { statuses: [200], schema: paymentViewsSchema },
      error: { statuses: [401, 403], schema: problemDetailsSchema },
    })
  }

  async createPaymentView(payload: { name: string, filters: PaymentViewFilters, columns?: string[], isDefault?: boolean }): Promise<IdentityResult<z.infer<typeof paymentViewSchema>>> {
    return this.transport.requestJson('POST /api/users/me/payment-views', () => this.context.post('/api/users/me/payment-views', { data: payload }), {
      success: { statuses: [201], schema: paymentViewSchema },
      error: { statuses: [400, 401, 403, 409], schema: problemDetailsSchema },
    })
  }

  async updatePaymentView(id: string, payload: { name: string, filters: PaymentViewFilters, columns?: string[] }): Promise<IdentityResult<z.infer<typeof paymentViewUpdateSchema>>> {
    const endpoint = `PUT /api/users/me/payment-views/${id}`
    return this.transport.requestJson(endpoint, () => this.context.put(endpoint.replace('PUT ', ''), { data: payload }), {
      success: { statuses: [200], schema: paymentViewUpdateSchema },
      error: { statuses: [400, 401, 403, 404, 409], schema: problemDetailsSchema },
    })
  }

  async deletePaymentView(id: string): Promise<IdentityResult<never> | EmptyResult> {
    const endpoint = `DELETE /api/users/me/payment-views/${id}`
    const result = await this.transport.requestJson(endpoint, () => this.context.delete(endpoint.replace('DELETE ', '')), {
      success: { statuses: [], schema: z.never() },
      error: { statuses: [401, 403, 404], schema: problemDetailsSchema },
      empty: { statuses: [204] },
    })
    return result
  }

  async setDefaultPaymentView(id: string): Promise<IdentityResult<z.infer<typeof paymentViewDefaultSchema>>> {
    const endpoint = `POST /api/users/me/payment-views/${id}/default`
    return this.transport.requestJson(endpoint, () => this.context.post(endpoint.replace('POST ', '')), {
      success: { statuses: [200], schema: paymentViewDefaultSchema },
      error: { statuses: [401, 403, 404, 409], schema: problemDetailsSchema },
    })
  }
}
