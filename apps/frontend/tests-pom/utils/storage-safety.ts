import { readFileSync } from 'node:fs'
import type { Page } from '@playwright/test'
import { expect } from '@playwright/test'
import { expectNoTokenInText } from './network'

/** Token must stay in the sealed Nuxt session cookie, never in Web Storage. */
export async function expectNoTokenInBrowserStorage(page: Page): Promise<void> {
  const [localHasJwt, sessionHasJwt] = await page.evaluate(() => {
    const local = JSON.stringify(Object.entries(localStorage))
    const session = JSON.stringify(Object.entries(sessionStorage))
    return [
      local.includes('eyJ') || local.includes('Bearer '),
      session.includes('eyJ') || session.includes('Bearer '),
    ]
  })

  expect(localHasJwt, 'localStorage must not contain a JWT or Bearer token').toBe(false)
  expect(sessionHasJwt, 'sessionStorage must not contain a JWT or Bearer token').toBe(false)
}

export async function expectSessionCookieHttpOnly(page: Page): Promise<void> {
  const cookies = await page.context().cookies()
  const session = cookies.find(cookie => cookie.name === 'nuxt-session')
  expect(session, 'nuxt-session cookie must be present').toBeTruthy()
  expect(session!.httpOnly, 'nuxt-session must be HttpOnly').toBe(true)
}

export function expectNoJwtInStorageStateFile(path: string): void {
  const raw = readFileSync(path, 'utf8')
  expectNoTokenInText(raw, `storageState ${path}`)
}
