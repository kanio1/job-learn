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

function sessionCookie(page: Page) {
  return page.context().cookies(page.url()).then(cookies => cookies.find(cookie => cookie.name === 'nuxt-session'))
}

/** App logout (deep Sign out or idle Unlock / shallow): sealed blob is gone. Empty value is as-built, not RFC 6265 delete. */
export async function expectSessionCookieCleared(page: Page): Promise<void> {
  const session = await sessionCookie(page)
  expect(session?.value ?? '', 'nuxt-session must not carry a sealed session after app logout').toBe('')
}

/** RFC 6265 delete: cookie absent (Max-Age=0 / past Expires). Do not use for path A while the product leaves an empty session cookie. */
export async function expectSessionCookieDeleted(page: Page): Promise<void> {
  const session = await sessionCookie(page)
  expect(session, 'nuxt-session must be absent after RFC cookie delete').toBeUndefined()
}

export async function expectSessionCookieHttpOnly(page: Page): Promise<void> {
  const session = await sessionCookie(page)
  expect(session, 'nuxt-session cookie must be present').toBeTruthy()
  expect(session!.httpOnly, 'nuxt-session must be HttpOnly').toBe(true)
}

/** RFC 6265 §6.1 — UA may drop cookies ≥ 4096 bytes. Do not decode the sealed blob. */
export async function expectSessionCookieUnderUaLimit(page: Page): Promise<void> {
  const session = await sessionCookie(page)
  expect(session, 'nuxt-session cookie must be present').toBeTruthy()
  expect(session!.name.length + session!.value.length, 'nuxt-session must fit under the 4 KB UA limit').toBeLessThan(4096)
  expect(session!.value.includes('id_token'), 'sealed session must not store id_token').toBe(false)
}

/** Playwright 1.61 cookies().sameSite is "Lax" | "Strict" | "None", not the cookie-policy JSON. */
export async function expectSessionCookieSameSiteLax(page: Page): Promise<void> {
  const session = await sessionCookie(page)
  expect(session, 'nuxt-session cookie must be present').toBeTruthy()
  expect(session!.sameSite, 'nuxt-session SameSite must come from the live cookie').toBe('Lax')
}

export async function expectSessionCookieSecure(page: Page, secure: boolean): Promise<void> {
  const cookies = await page.context().cookies()
  const session = cookies.find(cookie => cookie.name === 'nuxt-session')
  expect(session, 'nuxt-session cookie must be present').toBeTruthy()
  expect(session!.secure, `nuxt-session Secure must be ${secure}`).toBe(secure)
}

/** Sealed cookie blobs may coincidentally contain "eyJ"; only Web Storage origins are checked. */
export function expectNoJwtInStorageStateFile(path: string): void {
  const parsed = JSON.parse(readFileSync(path, 'utf8')) as {
    origins?: { localStorage?: { name: string, value: string }[], sessionStorage?: { name: string, value: string }[] }[]
  }
  const blobs = (parsed.origins ?? []).flatMap((origin) => {
    const local = (origin.localStorage ?? []).map(entry => `${entry.name}=${entry.value}`)
    const session = (origin.sessionStorage ?? []).map(entry => `${entry.name}=${entry.value}`)
    return [...local, ...session]
  })
  expectNoTokenInText(blobs.join('\n'), `storageState origins ${path}`)
}
