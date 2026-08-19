import { test, expect } from '../fixtures'

test('live 503 retry uses waitForResponse not fulfill', async ({ app }) => {
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
