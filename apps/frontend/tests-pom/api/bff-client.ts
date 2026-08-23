import { type APIRequestContext, type Playwright } from '@playwright/test'
import { isProblemDetails, type ProblemDetails } from '../utils/problem'
import { parseJson, parseJsonText } from '../utils/http'
import { pomNodeBaseURL } from '../utils/env'

export type PaymentPolicyBody = {
  autoCapture: boolean
  maxAutoCaptureMinor: number
  riskThreshold: number
  refundPolicy: 'MANUAL' | 'AUTOMATIC'
}

export type TenantSettingsBody = {
  contactEmail?: string | null
  timezone?: string
  webhookBaseUrl?: string | null
  paymentPolicy?: PaymentPolicyBody
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

  async listMerchants(query: Record<string, string | number | undefined> = {}) {
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== '') {
        params.set(key, String(value))
      }
    }
    const suffix = params.toString() ? `?${params.toString()}` : ''
    const response = await this.context.get(`/api/merchants${suffix}`)
    const body = await parseJson<{
      content?: Array<{ merchantId?: string, merchantReference?: string, status?: string }>
      page?: number
      size?: number
      totalElements?: number
      totalPages?: number
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async searchEntities(q: string) {
    const response = await this.context.get(`/api/search?q=${encodeURIComponent(q)}`)
    const body = await parseJson<{
      merchants?: Array<{ merchantId?: string, merchantReference?: string, displayName?: string }>
      payments?: Array<{ paymentOrderId?: string, merchantId?: string, clientOrderReference?: string }>
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async getOrgTree(parent?: string) {
    const suffix = parent ? `?parent=${encodeURIComponent(parent)}` : ''
    const response = await this.context.get(`/api/org-tree${suffix}`)
    const body = await parseJson<{
      nodes?: Array<{
        id?: string
        type?: string
        label?: string
        reference?: string
        lazy?: boolean
      }>
    } & ProblemDetails>(response)
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
    amountMinor = 1,
  ) {
    return this.postLifecycle(merchantId, paymentOrderId, 'refund', etag, idempotencyKey, { amountMinor })
  }

  async getPaymentOrdersSummary(
    merchantId: string,
    query: Record<string, string | undefined> = {},
  ) {
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== '') {
        params.set(key, value)
      }
    }
    const suffix = params.toString() ? `?${params.toString()}` : ''
    const response = await this.context.get(`/api/merchants/${merchantId}/payment-orders/summary${suffix}`)
    const body = parseJsonText<{
      totalOrders?: number
      totalAmountMinor?: number
      byStatus?: Array<{ status?: string, orderCount?: number }>
    }>(await response.text())
    return { status: response.status(), body, headers: response.headers() }
  }

  async getPaymentOrderHistory(merchantId: string, paymentOrderId: string) {
    const response = await this.context.get(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/history`,
    )
    const body = parseJsonText<{
      content?: Array<{ fromStatus?: string | null, toStatus?: string, action?: string | null }>
    }>(await response.text())
    return { status: response.status(), body, headers: response.headers() }
  }

  async createRefundApproval(
    merchantId: string,
    paymentOrderId: string,
    payload: { amountMinor?: number, reason?: string } = {},
  ) {
    const response = await this.context.post(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-approvals`,
      { data: payload },
    )
    const body = await parseJson<{ approvalId?: string, status?: string } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async approveRefundApproval(
    merchantId: string,
    paymentOrderId: string,
    approvalId: string,
    etag: string | undefined,
    idempotencyKey: string,
  ) {
    const headers: Record<string, string> = { 'Idempotency-Key': idempotencyKey }
    if (etag !== undefined) {
      headers['If-Match'] = etag
    }
    const response = await this.context.post(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-approvals/${approvalId}/approve`,
      { data: {}, headers },
    )
    return { status: response.status(), headers: response.headers(), body: await response.json().catch(() => undefined) }
  }

  async postNote(merchantId: string, paymentOrderId: string, bodyText: string) {
    const response = await this.context.post(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/notes`,
      { data: { body: bodyText } },
    )
    const body = await parseJson<{ id?: string, body?: string } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async getEvidence(merchantId: string, paymentOrderId: string, evidenceId: string) {
    const response = await this.context.get(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence/${evidenceId}`,
    )
    const buffer = Buffer.from(await response.body())
    return { status: response.status(), headers: response.headers(), raw: buffer }
  }

  async runExpirationSweep() {
    const response = await this.context.post('/api/payment-ops/expiration-sweep', { data: {} })
    const body = await parseJson<{ expiredCount?: number } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async listAudit(query: Record<string, string | number | undefined> = {}) {
    const response = await this.context.get('/api/audit', { params: query })
    const body = await parseJson<{
      content?: Array<{ id?: string, action?: string, actorDisplay?: string, targetType?: string }>
      totalElements?: number
    }>(response)
    return { status: response.status(), body }
  }

  async getAuditEntry(eventId: string) {
    const response = await this.context.get(`/api/audit/${encodeURIComponent(eventId)}`)
    const body = await parseJson<{ id?: string, action?: string } & ProblemDetails>(response)
    return { status: response.status(), body }
  }

  async patchMerchantDisplayName(merchantId: string, displayName: string, ifMatch?: string) {
    return this.patchMerchant(merchantId, { displayName }, ifMatch)
  }

  async patchMerchant(
    merchantId: string,
    body: { displayName?: string, contactPhone?: string | null, contactAddress?: string | null },
    ifMatch?: string,
  ) {
    const headers: Record<string, string> = {}
    if (ifMatch !== undefined) {
      headers['If-Match'] = ifMatch
    }
    const response = await this.context.patch(`/api/merchants/${merchantId}`, {
      data: body,
      headers,
    })
    return {
      status: response.status(),
      body: await parseJson<{
        displayName?: string
        merchantReference?: string
        merchantId?: string
        contactPhone?: string | null
        contactAddress?: string | null
        version?: number
      } & ProblemDetails>(response),
      headers: response.headers(),
    }
  }

  async activateMerchant(merchantId: string, ifMatch?: string) {
    const headers: Record<string, string> = {}
    if (ifMatch !== undefined) {
      headers['If-Match'] = ifMatch
    }
    const response = await this.context.post(`/api/merchants/${merchantId}/activate`, { headers })
    return {
      status: response.status(),
      body: await parseJson<{ error?: string }>(response),
      headers: response.headers(),
    }
  }

  async suspendMerchant(merchantId: string, ifMatch?: string) {
    const headers: Record<string, string> = {}
    if (ifMatch !== undefined) {
      headers['If-Match'] = ifMatch
    }
    const response = await this.context.post(`/api/merchants/${merchantId}/suspend`, { headers })
    return { status: response.status(), body: await response.json().catch(() => undefined), headers: response.headers() }
  }

  async previewMerchantImport(file: { name: string, mimeType: string, buffer: Buffer }) {
    const response = await this.context.post('/api/merchants/import/preview', {
      multipart: { file },
    })
    const body = await parseJson<{
      previewId?: string
      validCount?: number
      warningCount?: number
      rejectedCount?: number
      rows?: Array<{ status?: string, reason?: string }>
    }>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async commitMerchantImport(previewId: string) {
    const response = await this.context.post('/api/merchants/import/commit', {
      data: { previewId },
    })
    const body = await parseJson<{ createdCount?: number, error?: string }>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async createSupportCase(payload: {
    merchantId: string
    title: string
    paymentOrderId?: string
    priority?: string
    caseReference?: string
    assigneeSubject?: string
  }) {
    const response = await this.context.post('/api/support/cases', { data: payload })
    const body = await parseJson<{
      caseId?: string
      caseReference?: string
      status?: string
      version?: number
      assigneeSubject?: string | null
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async getSupportCase(caseId: string) {
    const response = await this.context.get(`/api/support/cases/${caseId}`)
    const body = await parseJson<{
      caseId?: string
      caseReference?: string
      status?: string
      version?: number
      assigneeSubject?: string | null
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async listSupportCases(query: Record<string, string | undefined> = {}) {
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== '') {
        params.set(key, value)
      }
    }
    const suffix = params.toString() ? `?${params.toString()}` : ''
    const response = await this.context.get(`/api/support/cases${suffix}`)
    const body = await parseJson<{
      content?: Array<{ caseId?: string, status?: string, caseReference?: string }>
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async patchSupportCase(
    caseId: string,
    payload: { status?: string, assigneeSubject?: string | null },
    ifMatch?: string,
  ) {
    const headers: Record<string, string> = {}
    if (ifMatch !== undefined) {
      headers['If-Match'] = ifMatch
    }
    const response = await this.context.patch(`/api/support/cases/${caseId}`, {
      data: payload,
      headers,
    })
    const body = await parseJson<{
      caseId?: string
      status?: string
      version?: number
      assigneeSubject?: string | null
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async bulkAssignSupportCases(caseIds: string[], assigneeSubject: string) {
    const response = await this.context.post('/api/support/cases/bulk-assign', {
      data: { caseIds, assigneeSubject },
    })
    const body = await parseJson<{
      succeeded?: number
      failed?: Array<{ caseId?: string, caseReference?: string, error?: string }>
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async createChallenge(merchantId: string, paymentOrderId: string) {
    const response = await this.context.post(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-challenges`,
      { data: {} },
    )
    const body = await parseJson<{
      challengeId?: string
      pin?: string
      expiresAt?: string
      error?: string
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async verifyChallenge(
    merchantId: string,
    paymentOrderId: string,
    challengeId: string,
    pin: string,
  ) {
    const response = await this.context.post(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/refund-challenges/${challengeId}/verify`,
      { data: { pin } },
    )
    const body = await parseJson<{
      challengeId?: string
      verifiedAt?: string
      error?: string
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
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
    data: Record<string, unknown>,
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

  async updateTenantSettings(settings: TenantSettingsBody, ifMatch?: string) {
    const headers: Record<string, string> = {}
    if (ifMatch !== undefined) {
      headers['If-Match'] = ifMatch
    }
    const response = await this.context.patch('/api/tenants/current/settings', {
      data: settings,
      headers,
    })
    return { status: response.status(), headers: response.headers(), body: await response.json().catch(() => undefined) }
  }

  async listUsers(query?: { search?: string, status?: 'enabled' | 'disabled', role?: string }) {
    const response = await this.context.get('/api/users', { params: query })
    const body = await parseJson<{
      users?: Array<{ id?: string, username?: string, enabled?: boolean, roles?: string[] }>
    } & ProblemDetails>(response)
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

  async listEventLab(query: Record<string, string | undefined> = {}) {
    const params = new URLSearchParams()
    for (const [k, v] of Object.entries(query)) if (v) params.set(k, v)
    const q = params.toString() ? `?${params.toString()}` : ''
    const response = await this.context.get(`/api/event-lab${q}`)
    return { status: response.status(), body: await response.json().catch(() => undefined), headers: response.headers(), raw: await response.text().catch(() => '') }
  }

  async getEventLabDetail(id: string) {
    const response = await this.context.get(`/api/event-lab/${encodeURIComponent(id)}`)
    return { status: response.status(), body: await response.json().catch(() => undefined), headers: response.headers() }
  }

  async injectDuplicate(eventId: string) {
    const response = await this.context.post('/api/event-lab/inject/duplicate', { data: { eventId } })
    return { status: response.status(), body: await response.json().catch(() => undefined), headers: response.headers() }
  }

  async injectPoison(eventId: string) {
    const response = await this.context.post('/api/event-lab/inject/poison', { data: { eventId } })
    return { status: response.status(), body: await response.json().catch(() => undefined), headers: response.headers() }
  }

  async injectOpsFeed(payload: {
    eventId?: string
    occurredAt?: string
    merchantId?: string
    paymentOrderId?: string
    type?: string
    label?: string
    raw?: string
  }) {
    const response = await this.context.post('/api/ops/feed/inject', { data: payload })
    const body = await parseJson<{
      eventId?: string
      occurredAt?: string
      merchantId?: string
      paymentOrderId?: string
      type?: string
      label?: string
      malformed?: boolean
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async disconnectOpsFeed() {
    const response = await this.context.post('/api/ops/feed/disconnect-me')
    return { status: response.status() }
  }

  async listNotifications(unreadOnly = false) {
    const suffix = unreadOnly ? '?unreadOnly=true' : ''
    const response = await this.context.get(`/api/notifications${suffix}`)
    const body = await parseJson<{
      content?: Array<{
        notificationId?: string
        eventId?: string
        eventType?: string
        title?: string
        body?: string
        readAt?: string | null
      }>
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async markNotificationRead(notificationId: string) {
    const response = await this.context.post(`/api/notifications/${notificationId}/read`)
    const body = await parseJson<{ notificationId?: string, readAt?: string | null } & ProblemDetails>(response)
    return { status: response.status(), body }
  }

  async markAllNotificationsRead() {
    const response = await this.context.post('/api/notifications/read-all')
    return { status: response.status() }
  }

  async listPaymentViews() {
    const response = await this.context.get('/api/users/me/payment-views')
    const body = await parseJson<{
      content?: Array<{
        id?: string
        name?: string
        resource?: string
        filters?: Record<string, unknown>
        columns?: string[]
        isDefault?: boolean
      }>
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async createPaymentView(payload: {
    name: string
    filters: Record<string, unknown>
    columns?: string[]
    isDefault?: boolean
  }) {
    const response = await this.context.post('/api/users/me/payment-views', { data: payload })
    const body = await parseJson<{
      id?: string
      name?: string
      resource?: string
      filters?: Record<string, unknown>
      isDefault?: boolean
    } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async updatePaymentView(
    id: string,
    payload: { name: string, filters: Record<string, unknown>, columns?: string[] },
  ) {
    const response = await this.context.put(`/api/users/me/payment-views/${id}`, { data: payload })
    const body = await parseJson<{ id?: string, name?: string } & ProblemDetails>(response)
    return { status: response.status(), body, headers: response.headers() }
  }

  async deletePaymentView(id: string) {
    const response = await this.context.delete(`/api/users/me/payment-views/${id}`)
    return { status: response.status() }
  }

  async setDefaultPaymentView(id: string) {
    const response = await this.context.post(`/api/users/me/payment-views/${id}/default`)
    const body = await parseJson<{ id?: string, isDefault?: boolean } & ProblemDetails>(response)
    return { status: response.status(), body }
  }

  static isProblem(body: unknown): body is ProblemDetails {
    return isProblemDetails(body)
  }
}
