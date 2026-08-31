import type { APIRequestContext, BrowserContextOptions, PlaywrightWorkerArgs } from '@playwright/test'
import { BffTransport } from './BffTransport'
import { expectStatus } from './contracts/assertions'
import { IdentityClient } from './identity/IdentityClient'
import { LabsClient } from './labs/LabsClient'
import { MerchantsClient } from './merchants/MerchantsClient'
import { OperationsClient } from './operations/OperationsClient'
import { PaymentsClient } from './payments/PaymentsClient'
import { pomNodeBaseURL } from '../utils/env'

/** Exact type supplied by Playwright's `playwright` worker fixture. */
export type Playwright = PlaywrightWorkerArgs['playwright']
export type StorageState = NonNullable<BrowserContextOptions['storageState']>
export type { TenantSettingsBody } from './identity/IdentityClient'
export type { EventLabListRow } from './labs/LabsClient'
export { expectStatus }

/**
 * Composition root for one actor's authenticated BFF request context.
 * Transport owns response handling; each domain client owns its contracts.
 */
export class BffClient {
  /** Node REST stays on IPv4. Browser OIDC uses localhost (EG-W2-02). */
  static readonly DEFAULT_BASE_URL = 'http://127.0.0.1:3000'

  private readonly transport: BffTransport
  readonly merchants: MerchantsClient
  readonly payments: PaymentsClient
  readonly operations: OperationsClient
  readonly identity: IdentityClient
  readonly labs: LabsClient

  private constructor(context: APIRequestContext) {
    this.transport = new BffTransport(context)
    this.merchants = new MerchantsClient(context, this.transport)
    this.payments = new PaymentsClient(context, this.transport)
    this.operations = new OperationsClient(context, this.transport)
    this.identity = new IdentityClient(context, this.transport)
    this.labs = new LabsClient(context, this.transport)
  }

  static async create(playwright: Playwright, storageState: StorageState, baseURL?: string): Promise<BffClient> {
    const context = await playwright.request.newContext({
      baseURL: baseURL ?? pomNodeBaseURL(),
      storageState,
      ignoreHTTPSErrors: process.env.PLAYWRIGHT_TLS_INSECURE === '1',
    })
    return new BffClient(context)
  }

  async dispose(): Promise<void> {
    await this.transport.dispose()
  }
}
