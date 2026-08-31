import { test, expect } from '../fixtures'
import { parseJsonWithSchema } from '../utils/http'
import {
  expectNoTokenInBrowserStorage,
  expectSessionCookieHttpOnly,
  expectSessionCookieSameSiteLax,
  expectSessionCookieUnderUaLimit,
} from '../utils/storage-safety'
import { z } from 'zod'

// Success is `{ status: 'ok' }`; Nitro's 403 problem has numeric HTTP status.
const csrfResponseSchema = z.object({ status: z.union([z.string(), z.number()]).optional(), error: z.string().optional() }).passthrough()
const csrfTokenSchema = z.object({ token: z.string().optional() }).passthrough()

test('session lab shows HttpOnly policy vs empty document.cookie', { tag: ['@security'] }, async ({ app }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  const jsCookies = await app.sessionLab.jsCookies().innerText()
  expect(jsCookies.includes('nuxt-session'), 'HttpOnly nuxt-session must not appear in document.cookie').toBe(false)
  await expect(app.sessionLab.cookiePolicy()).toContainText('nuxt-session')
  await expect(app.sessionLab.cookiePolicy()).toContainText('httpOnly')
  const policy = await app.page.request.get('/api/session-lab/cookie-policy')
  expect(policy.status()).toBe(200)
  await expectSessionCookieHttpOnly(app.page)
  await expectSessionCookieSameSiteLax(app.page)
  await expectSessionCookieUnderUaLimit(app.page)
  await expectNoTokenInBrowserStorage(app.page)
})

test('idle lock uses page.clock without waitForTimeout', { tag: ['@security'] }, async ({ app }) => {
  await app.page.clock.install()
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  await app.page.clock.fastForward(121_000)
  await expect(app.idle.lock()).toBeVisible()
  await app.idle.unlock()
  await expect(app.page).toHaveURL(/\/login/)
  await app.page.goto('/admin/merchants')
  await expect(app.page).toHaveURL(/\/login/)
})

test('idle lock stays hidden at TTL-1s then appears after +2s', { tag: ['@security'] }, async ({ app }) => {
  await app.page.clock.install()
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  await app.page.clock.fastForward(119_000)
  await expect(app.idle.lock()).toHaveCount(0)
  await app.page.clock.fastForward(2_000)
  await expect(app.idle.lock()).toBeVisible()
})

test('idle overlay appears on merchants then Unlock returns to login', { tag: ['@security'] }, async ({ app }) => {
  await app.page.clock.install()
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await app.page.clock.fastForward(121_000)
  await expect(app.idle.lock()).toBeVisible()
  await app.idle.unlock()
  await expect(app.page).toHaveURL(/\/login/)
  await app.login.expectLoaded()
})

test('csrf demo without token returns 403 csrf_failed', { tag: ['@security'] }, async ({ app }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  const responsePromise = app.page.waitForResponse(response =>
    response.url().includes('/api/session-lab/csrf-demo') && response.request().method() === 'POST')
  await app.sessionLab.csrfFail()
  const response = await responsePromise
  expect(response.status()).toBe(403)
  const body = await response.json()
  expect(body.error).toBe('csrf_failed')
})

test('csrf demo with token returns ok', { tag: ['@security'] }, async ({ app }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  const responsePromise = app.page.waitForResponse(response =>
    response.url().includes('/api/session-lab/csrf-demo') && response.request().method() === 'POST')
  await app.sessionLab.csrfOk()
  const response = await responsePromise
  expect(response.status()).toBe(200)
  const body = parseJsonWithSchema(await response.text(), csrfResponseSchema, 'POST /api/session-lab/csrf-demo')
  expect(body.error).not.toBe('csrf_failed')
  expect(body.status).toBe('ok')
})

test('csrf demo with a wrong X-CSRF-Token returns 403 csrf_failed', { tag: ['@security'] }, async ({ app, page }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  const issued = await page.request.get('/api/session-lab/csrf')
  expect(issued.status()).toBe(200)
  const mismatched = await page.request.post('/api/session-lab/csrf-demo', {
    headers: { 'X-CSRF-Token': 'not-the-cookie-token' },
  })
  expect(mismatched.status()).toBe(403)
  const body = parseJsonWithSchema(await mismatched.text(), csrfResponseSchema, 'POST /api/session-lab/csrf-demo')
  expect(body.error).toBe('csrf_failed')
})

test('mrl-csrf is visible on document.cookie after GET csrf', { tag: ['@security'] }, async ({ app, page }) => {
  await app.sessionLab.goto()
  await app.sessionLab.expectLoaded()
  const issued = await page.request.get('/api/session-lab/csrf')
  expect(issued.status()).toBe(200)
  const token = parseJsonWithSchema(await issued.text(), csrfTokenSchema, 'GET /api/session-lab/csrf').token
  expect(token).toBeTruthy()
  await page.reload()
  await app.sessionLab.expectLoaded()
  const jsCookies = await app.sessionLab.jsCookies().innerText()
  expect(jsCookies.includes('mrl-csrf'), 'non-HttpOnly mrl-csrf must appear in document.cookie').toBe(true)
})
