import type { Browser, BrowserContext, BrowserContextOptions, Page } from '@playwright/test'
import { BffClient, type Playwright, type StorageState } from '../api/bff-client'
import { App } from '../pages/App'
import { pomAuthFiles, pomBrowserBaseURL, pomNodeBaseURL } from '../utils/env'

export type Persona =
  | 'guest'
  | 'platformAdmin'
  | 'platformAdminSession'
  | 'platformOperator'
  | 'tenantAdmin'
  | 'merchantManager'
  | 'supportAgent'
  | 'readOnlyUser'
  | 'merchantDenied'

export type Actor = {
  page: Page
  app: App
  api: BffClient
}

export type ActorOptions = {
  baseURL?: string
  ignoreHTTPSErrors?: boolean
}

type OwnedActor = Actor & { context: BrowserContext }

const guestStorageState: StorageState = { cookies: [], origins: [] }

function storageStateFor(persona: Persona): StorageState {
  switch (persona) {
    case 'guest': return guestStorageState
    case 'platformAdmin': return pomAuthFiles.platformAdmin
    case 'platformAdminSession': return pomAuthFiles.platformAdminSession
    case 'platformOperator': return pomAuthFiles.platformOperator
    case 'tenantAdmin': return pomAuthFiles.tenantAdmin
    case 'merchantManager': return pomAuthFiles.merchantManager
    case 'supportAgent': return pomAuthFiles.supportAgent
    case 'readOnlyUser': return pomAuthFiles.readOnlyUser
    case 'merchantDenied': return pomAuthFiles.merchantDenied
  }
}

/** Owns every extra persona context and its matching BFF context for one test. */
export class ActorFactory {
  private readonly owned: OwnedActor[] = []

  constructor(
    private readonly browser: Browser,
    private readonly playwright: Playwright,
  ) {}

  async open(persona: Persona, options: ActorOptions = {}): Promise<Actor> {
    return this.openStorageState(storageStateFor(persona), options)
  }

  /** Duplicate the fixture-selected session while preserving factory teardown. */
  async openStorageState(storageState: StorageState, options: ActorOptions = {}): Promise<Actor> {
    const contextOptions: BrowserContextOptions = {
      storageState,
      baseURL: options.baseURL ?? pomBrowserBaseURL(),
    }
    if (options.ignoreHTTPSErrors !== undefined) {
      contextOptions.ignoreHTTPSErrors = options.ignoreHTTPSErrors
    }
    const context = await this.browser.newContext(contextOptions)
    try {
      const page = await context.newPage()
      const api = await BffClient.create(this.playwright, storageState, pomNodeBaseURL())
      const actor: OwnedActor = { context, page, app: new App(page), api }
      this.owned.push(actor)
      return actor
    }
    catch (error) {
      await context.close()
      throw error
    }
  }

  async dispose(): Promise<void> {
    const actors = this.owned.splice(0).reverse()
    await Promise.allSettled(actors.map(async actor => {
      await actor.api.dispose()
      await actor.context.close()
    }))
  }
}
