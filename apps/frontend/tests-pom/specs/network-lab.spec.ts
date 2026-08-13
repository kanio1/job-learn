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
