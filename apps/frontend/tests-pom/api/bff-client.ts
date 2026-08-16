import { type APIRequestContext, type Playwright } from '@playwright/test'
import { isProblemDetails, type ProblemDetails } from '../utils/problem'

export type TenantSettingsBody = {
  contactEmail?: string | null
  timezone?: string
  webhookBaseUrl?: string | null
}

export class BffClient {
  private constructor(private readonly context: APIRequestContext) {}

  static async create(playwright: Playwright, storageState: string): Promise<BffClient> {
    const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000'
    const context = await playwright.request.newContext({
      baseURL,
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
    const text = await response.text()
    const body = text ? JSON.parse(text) as { merchantId?: string, merchantReference?: string } : undefined
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
    const text = await response.text()
    let body: ({ merchantId?: string } & ProblemDetails) | undefined
    if (text) {
      try {
        body = JSON.parse(text) as { merchantId?: string } & ProblemDetails
      }
      catch {
        body = undefined
      }
    }
    return { status: response.status(), body, headers: response.headers() }
  }

  async createPaymentOrder(
    merchantId: string,
    payload: { amountMinor: number, currency: string, clientOrderReference: string },
    idempotencyKey: string,
  ) {
    const response = await this.context.post(`/api/merchants/${merchantId}/payment-orders`, {
      data: payload,
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    const body = await response.json() as { paymentOrderId?: string, status?: string } & ProblemDetails
    return { status: response.status(), body, headers: response.headers() }
  }

  async listNotes(merchantId: string, paymentOrderId: string) {
    const response = await this.context.get(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/notes`,
    )
    const text = await response.text()
    const parsed = text ? JSON.parse(text) as { body?: string, id?: string }[] : []
    return { status: response.status(), body: parsed, headers: response.headers() }
  }

  async getPaymentOrder(merchantId: string, paymentOrderId: string) {
    const response = await this.context.get(`/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`)
    const text = await response.text()
    const body = text ? JSON.parse(text) as { status?: string, paymentOrderId?: string } : undefined
    return { status: response.status(), body, headers: response.headers() }
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
    const text = await response.text()
    const body = text
      ? JSON.parse(text) as {
        content?: Array<{ clientOrderReference?: string, amountMinor?: number, status?: string, currency?: string }>
        page?: number
        totalElements?: number
      }
      : undefined
    return { status: response.status(), body, headers: response.headers() }
  }

  async listRlsItems() {
    const response = await this.context.get('/api/rls-lab/items')
    const text = await response.text()
    const body = text ? JSON.parse(text) as { items?: Array<{ itemId?: string, label?: string }> } : undefined
    return { status: response.status(), body }
  }

  async getRlsItem(itemId: string) {
    const response = await this.context.get(`/api/rls-lab/items/${itemId}`)
    const text = await response.text()
    const body = text ? JSON.parse(text) as ProblemDetails & { itemId?: string, label?: string } : undefined
    return { status: response.status(), body }
  }

  async rlsCompare() {
    const response = await this.context.get('/api/rls-lab/compare')
    const text = await response.text()
    const body = text
      ? JSON.parse(text) as ProblemDetails & {
        bypassRoleCount?: number
        restrictedWithoutTenantGuc?: number
        unprotected?: number
      }
      : undefined
    return { status: response.status(), body }
  }

  async authorizePayment(merchantId: string, paymentOrderId: string, etag: string, idempotencyKey: string) {
    const response = await this.context.post(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/authorize`,
      {
        data: {},
        headers: {
          'Idempotency-Key': idempotencyKey,
          'If-Match': etag,
        },
      },
    )
    return { status: response.status(), headers: response.headers(), body: await response.json().catch(() => undefined) }
  }

  async capturePayment(
    merchantId: string,
    paymentOrderId: string,
    etag: string,
    idempotencyKey: string,
    amountMinor: number,
  ) {
    const response = await this.context.post(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/capture`,
      {
        data: { amountMinor },
        headers: {
          'Idempotency-Key': idempotencyKey,
          'If-Match': etag,
        },
      },
    )
    return { status: response.status(), headers: response.headers(), body: await response.json().catch(() => undefined) }
  }

  async getTenantSettings() {
    const response = await this.context.get('/api/tenants/current/settings')
    return { status: response.status(), headers: response.headers(), body: await response.json() as TenantSettingsBody }
  }

  async updateTenantSettings(settings: TenantSettingsBody, ifMatch: string) {
    const response = await this.context.patch('/api/tenants/current/settings', {
      data: settings,
      headers: { 'If-Match': ifMatch },
    })
    return { status: response.status(), headers: response.headers(), body: await response.json().catch(() => undefined) }
  }

  async listUsers() {
    const response = await this.context.get('/api/users')
    return { status: response.status(), body: await response.json() }
  }

  static isProblem(body: unknown): body is ProblemDetails {
    return isProblemDetails(body)
  }
}
