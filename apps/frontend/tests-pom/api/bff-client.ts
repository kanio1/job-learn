import { type APIRequestContext, type Playwright } from '@playwright/test'
import { isProblemDetails, type ProblemDetails } from '../utils/problem'
import { parseJson, parseJsonText } from '../utils/http'
import { pomNodeBaseURL } from '../utils/env'

export type TenantSettingsBody = {
  contactEmail?: string | null
  timezone?: string
  webhookBaseUrl?: string | null
}

export class BffClient {
  /** Node REST stays on IPv4. Browser OIDC uses localhost (EG-W2-02). */
  static readonly DEFAULT_BASE_URL = 'http://127.0.0.1:3000'

  private constructor(private readonly context: APIRequestContext) {}

  static async create(playwright: Playwright, storageState: string, baseURL?: string): Promise<BffClient> {
    const context = await playwright.request.newContext({
      baseURL: baseURL || pomNodeBaseURL(),
      storageState,
      ignoreHTTPSErrors: process.env.PLAYWRIGHT_TLS_INSECURE === '1',
    })
    return new BffClient(context)
  }

  async dispose(): Promise<void> {
    await this.context.dispose()
  }

  async getMerchant(merchantId: string) {
    const response = await this.context.get(`/api/merchants/${merchantId}`)
    const body = await parseJson<{ merchantId?: string, merchantReference?: string } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async listMerchants() {
    const response = await this.context.get('/api/merchants')
    const body = await parseJson<{ merchants?: Array<{ merchantId?: string, merchantReference?: string }> } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async createMerchant(
    merchantReference: string,
    displayName: string,
    tenantReference: string | null = 'TENANT_ALPHA',
  ) {
    const data: Record<string, string> = { merchantReference, displayName }
    if (tenantReference !== null) {
      data.tenantReference = tenantReference
    }
    const response = await this.context.post('/api/merchants', { data })
    const body = await parseJson<{ merchantId?: string } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async createPaymentOrder(
    merchantId: string,
    payload: { amountMinor: number, currency: string, clientOrderReference: string },
    idempotencyKey?: string | null,
  ) {
    const headers: Record<string, string> = {}
    if (idempotencyKey !== undefined && idempotencyKey !== null) {
      headers['Idempotency-Key'] = idempotencyKey
    }
    const response = await this.context.post(`/api/merchants/${merchantId}/payment-orders`, {
      data: payload,
      headers,
    })
    const body = await parseJson<{ paymentOrderId?: string, status?: string } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async listNotes(merchantId: string, paymentOrderId: string) {
    const response = await this.context.get(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/notes`,
    )
    const parsed = await parseJson<{ body?: string, id?: string }[]>(response) ?? []
    return { status: response.status(), body: parsed, headers: response.headers() }
  }

  async getPaymentOrder(
    merchantId: string,
    paymentOrderId: string,
    headers: Record<string, string> = {},
  ) {
    const response = await this.context.get(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`,
      Object.keys(headers).length > 0 ? { headers } : undefined,
    )
    const text = await response.text()
    const body = parseJsonText<{
      status?: string
      paymentOrderId?: string
      clientOrderReference?: string
      expiresAt?: string | null
    }>(text)
    return { status: response.status(), body, headers: response.headers(), raw: text }
  }

  async headPaymentOrder(merchantId: string, paymentOrderId: string) {
    const response = await this.context.head(`/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`)
    return { status: response.status(), headers: response.headers(), raw: await response.text() }
  }

  async listPaymentOrders(
    merchantId: string,
    query: Record<string, string | number | undefined> = {},
  ) {
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== '') {
        params.set(key, String(value))
      }
    }
    const suffix = params.toString() ? `?${params.toString()}` : ''
    const response = await this.context.get(`/api/merchants/${merchantId}/payment-orders${suffix}`)
    const body = parseJsonText<{
      content?: Array<{
        paymentOrderId?: string
        clientOrderReference?: string
        amountMinor?: number
        status?: string
        currency?: string
      }>
      page?: number
      totalElements?: number
    }>(await response.text())
    return { status: response.status(), body, headers: response.headers() }
  }

  async listRlsItems() {
    const response = await this.context.get('/api/rls-lab/items')
    const body = parseJsonText<{ items?: Array<{ itemId?: string, label?: string }> }>(await response.text())
    return { status: response.status(), body }
  }

  async getRlsItem(itemId: string) {
    const response = await this.context.get(`/api/rls-lab/items/${itemId}`)
    const body = parseJsonText<ProblemDetails & { itemId?: string, label?: string }>(await response.text())
    return { status: response.status(), body }
  }

  async rlsCompare() {
    const response = await this.context.get('/api/rls-lab/compare')
    const body = parseJsonText<ProblemDetails & {
      bypassRoleCount?: number
      restrictedWithoutTenantGuc?: number
      unprotected?: number
    }>(await response.text())
    return { status: response.status(), body }
  }

  async authorizePayment(
    merchantId: string,
    paymentOrderId: string,
    etag: string | undefined,
    idempotencyKey: string,
  ) {
    return this.postLifecycle(merchantId, paymentOrderId, 'authorize', etag, idempotencyKey)
  }

  async capturePayment(
    merchantId: string,
    paymentOrderId: string,
    etag: string | undefined,
    idempotencyKey: string,
    amountMinor: number,
  ) {
    const headers: Record<string, string> = { 'Idempotency-Key': idempotencyKey }
    if (etag !== undefined) {
      headers['If-Match'] = etag
    }
    const response = await this.context.post(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/capture`,
      {
        data: { amountMinor },
        headers,
      },
    )
    return { status: response.status(), headers: response.headers(), body: await response.json().catch(() => undefined) }
  }

  async cancelPayment(
    merchantId: string,
    paymentOrderId: string,
    etag: string | undefined,
    idempotencyKey: string,
  ) {
    return this.postLifecycle(merchantId, paymentOrderId, 'cancel', etag, idempotencyKey)
  }

  async refundPayment(
    merchantId: string,
    paymentOrderId: string,
    etag: string,
    idempotencyKey: string,
  ) {
    return this.postLifecycle(merchantId, paymentOrderId, 'refund', etag, idempotencyKey, { amountMinor: 1 })
  }

  async activateMerchant(merchantId: string) {
    const response = await this.context.post(`/api/merchants/${merchantId}/activate`)
    return {
      status: response.status(),
      body: await parseJson<{ error?: string }>(response),
      headers: response.headers(),
    }
  }

  async suspendMerchant(merchantId: string) {
    const response = await this.context.post(`/api/merchants/${merchantId}/suspend`)
    return { status: response.status(), body: await response.json().catch(() => undefined), headers: response.headers() }
  }

  private async postLifecycle(
    merchantId: string,
    paymentOrderId: string,
    action: 'authorize' | 'capture' | 'cancel' | 'refund',
    etag: string | undefined,
    idempotencyKey: string,
    data: Record<string, string | number | boolean | null> = {},
  ) {
    const headers: Record<string, string> = { 'Idempotency-Key': idempotencyKey }
    if (etag !== undefined) {
      headers['If-Match'] = etag
    }
    const response = await this.context.post(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/${action}`,
      { data, headers },
    )
    return { status: response.status(), headers: response.headers(), body: await response.json().catch(() => undefined) }
  }

  async patchPaymentOrder(
    merchantId: string,
    paymentOrderId: string,
    data: Record<string, string | number | boolean | null>,
    ifMatch?: string,
  ) {
    const headers: Record<string, string> = { 'Content-Type': 'application/merge-patch+json' }
    if (ifMatch !== undefined) {
      headers['If-Match'] = ifMatch
    }
    const response = await this.context.patch(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`,
      { data, headers },
    )
    return { status: response.status(), headers: response.headers(), body: await response.json().catch(() => undefined) }
  }

  async uploadEvidence(
    merchantId: string,
    paymentOrderId: string,
    file: { name: string, mimeType: string, buffer: Buffer },
    category = 'RECEIPT',
  ) {
    const response = await this.context.post(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence`,
      { multipart: { file, category } },
    )
    const body = await parseJson<{ evidenceId?: string, category?: string }>(response)
    return { status: response.status(), headers: response.headers(), body }
  }

  async createExportJob(merchantId: string) {
    const response = await this.context.post(`/api/merchants/${merchantId}/payment-orders/export-jobs`)
    const body = await parseJson<{ jobId?: string, status?: string }>(response)
    return { status: response.status(), headers: response.headers(), body }
  }

  async getExportJob(merchantId: string, jobId: string) {
    const response = await this.context.get(`/api/merchants/${merchantId}/payment-orders/export-jobs/${jobId}`)
    const body = await parseJson<{ jobId?: string, status?: string }>(response)
    return { status: response.status(), headers: response.headers(), body }
  }

  async getExportJobContent(merchantId: string, jobId: string) {
    const response = await this.context.get(`/api/merchants/${merchantId}/payment-orders/export-jobs/${jobId}/content`)
    return { status: response.status(), headers: response.headers(), raw: await response.text() }
  }

  async getTenantSettings() {
    const response = await this.context.get('/api/tenants/current/settings')
    return { status: response.status(), headers: response.headers(), body: await parseJson<TenantSettingsBody>(response) }
  }

  async updateTenantSettings(settings: TenantSettingsBody, ifMatch: string) {
    const response = await this.context.patch('/api/tenants/current/settings', {
      data: settings,
      headers: { 'If-Match': ifMatch },
    })
    return { status: response.status(), headers: response.headers(), body: await response.json().catch(() => undefined) }
  }

  async listUsers(query?: { search?: string, status?: 'enabled' | 'disabled' }) {
    const response = await this.context.get('/api/users', { params: query })
    const body = await parseJson<{
      users?: Array<{ id?: string, username?: string, enabled?: boolean, roles?: string[] }>
    }>(response)
    return { status: response.status(), body }
  }

  async createUser(payload: {
    username: string
    email: string
    temporaryPassword: string
    tenantId?: string
    roles: string[]
  }) {
    const response = await this.context.post('/api/users', { data: payload })
    const body = await parseJson<{ id?: string, username?: string, enabled?: boolean, roles?: string[] }>(response)
    return { status: response.status(), body }
  }

  async updateUser(userId: string, payload: { enabled?: boolean, email?: string }) {
    const response = await this.context.patch(`/api/users/${encodeURIComponent(userId)}`, { data: payload })
    const body = await parseJson<{ id?: string, username?: string, enabled?: boolean, roles?: string[] }>(response)
    return { status: response.status(), body }
  }

  async assignUserRoles(userId: string, payload: { assign: string[], remove: string[] }) {
    const response = await this.context.post(`/api/users/${encodeURIComponent(userId)}/roles`, { data: payload })
    const body = await parseJson<{ id?: string, username?: string, roles?: string[] }>(response)
    return { status: response.status(), body }
  }

  static isProblem(body: unknown): body is ProblemDetails {
    return isProblemDetails(body)
  }
}
