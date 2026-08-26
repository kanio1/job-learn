import { test, expect } from '../fixtures'

test.describe.configure({ mode: 'serial' })

test('live 503 retry uses waitForResponse not fulfill', async ({ app }) => {
  test.setTimeout(45_000)
  await app.networkLab.goto()
  await app.networkLab.expectLoaded()
  const first = app.page.waitForResponse(response =>
    response.url().includes('/api/network-lab/trigger-503-retry') && response.request().method() === 'POST')
  await app.networkLab.trigger503()
  expect((await first).status()).toBe(503)
  const second = app.page.waitForResponse(response =>
    response.url().includes('/api/network-lab/trigger-503-retry') && response.request().method() === 'POST')
  await app.networkLab.trigger503()
  expect((await second).status()).toBe(200)
  const third = app.page.waitForResponse(response =>
    response.url().includes('/api/network-lab/trigger-503-retry') && response.request().method() === 'POST')
  await app.networkLab.trigger503()
  expect((await third).status()).toBe(503)
})

test('retry window expires to a fresh 503 without a 200', async ({ app, page }) => {
  test.setTimeout(45_000)
  await app.networkLab.goto()
  await app.networkLab.expectLoaded()
  await expect.poll(async () => {
    const window = await page.request.get('/api/network-lab/retry-window')
    expect(window.status()).toBe(200)
    const body = await window.json() as { remainingMs?: number }
    return body.remainingMs ?? -1
  }, { timeout: 20_000 }).toBe(0)
  const first = page.waitForResponse(response =>
    response.url().includes('/api/network-lab/trigger-503-retry') && response.request().method() === 'POST')
  await app.networkLab.trigger503()
  expect((await first).status()).toBe(503)
  await expect.poll(async () => {
    const window = await page.request.get('/api/network-lab/retry-window')
    expect(window.status()).toBe(200)
    const body = await window.json() as { remainingMs?: number }
    return body.remainingMs ?? -1
  }, { timeout: 20_000 }).toBe(0)
  const afterTtl = await page.request.post('/api/network-lab/trigger-503-retry')
  expect(afterTtl.status()).toBe(503)
})

test('lie fulfillment returns success JSON that is not a persistence oracle', async ({ app }) => {
  await app.networkLab.goto()
  await app.networkLab.expectLoaded()
  const lie = app.page.waitForResponse(response =>
    response.url().includes('/api/network-lab/lie-fulfillment') && response.request().method() === 'GET')
  await app.networkLab.triggerLie()
  const response = await lie
  expect(response.status()).toBe(200)
  const body = await response.json() as { status?: string, warning?: string }
  expect(body.status).toBe('success')
  expect(body.warning ?? '').toMatch(/not an oracle/i)
  await expect(app.page.getByTestId('network-lab-result')).toContainText('success')
})

test('OPTIONS cors-cookie is 204 with lab origin ACAO', { tag: ['@security'] }, async ({ page }) => {
  const response = await page.request.fetch('/api/network-lab/cors-cookie', {
    method: 'OPTIONS',
    headers: { Origin: 'http://localhost:3000' },
  })
  expect(response.status()).toBe(204)
  expect(response.headers()['access-control-allow-origin']).toBe('http://localhost:3000')
  expect(response.headers()['access-control-allow-credentials']).toBe('true')
})

test('GET cors-cookie with evil Origin still returns lab ACAO', { tag: ['@security'] }, async ({ page }) => {
  const response = await page.request.get('/api/network-lab/cors-cookie', {
    headers: { Origin: 'https://evil.example' },
  })
  expect(response.status()).toBe(200)
  const body = await response.json() as { hostedNote?: string }
  expect(body.hostedNote ?? '').toMatch(/hosted checkout/i)
  expect(response.headers()['access-control-allow-origin']).toBe('http://localhost:3000')
  expect(response.headers()['access-control-allow-origin']).not.toBe('https://evil.example')
})

test('network lab lie is not a 200 success body when the browser is offline', async ({ app, context }) => {
  await app.networkLab.goto()
  await app.networkLab.expectLoaded()
  await context.setOffline(true)
  await app.page.evaluate(() => window.dispatchEvent(new Event('offline')))
  try {
    await app.networkLab.triggerLie()
    await expect(app.networkLab.errorState()).toBeVisible()
    await expect(app.networkLab.result()).toHaveCount(0)
  }
  finally {
    await context.setOffline(false)
    await app.page.evaluate(() => window.dispatchEvent(new Event('online')))
  }
})
