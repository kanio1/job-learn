import { test, expect } from '../fixtures'

test('mirror lab nav and pages are absent when the public flag is off', async ({ app }) => {
  await app.merchants.goto()
  await app.merchants.expectLoaded()
  await expect(app.sidebar.mirrorLab()).toHaveCount(0)

  await app.page.goto('/admin/mirror-lab')
  await app.mirrorHub.expectNotFound()

  await app.page.goto('/admin/session-lab')
  await expect(app.sessionLab.jsCookies()).toHaveCount(0)
  await app.sessionLab.expectNotFound()
})

test('session-lab BFF returns 404 when the public flag is off', async ({ page }) => {
  const csrf = await page.request.get('/api/session-lab/csrf')
  expect(csrf.status()).toBe(404)
})

test('mirror-lab statements BFF returns 404 when the public flag is off', async ({ page }) => {
  const csv = await page.request.get('/api/mirror-lab/statements')
  expect(csv.status()).toBe(404)
  const pdf = await page.request.get('/api/mirror-lab/statements', { params: { format: 'pdf' } })
  expect(pdf.status()).toBe(404)
})

test('visual and network lab deep links are 404 when the public flag is off', async ({ app }) => {
  await app.page.goto('/admin/visual-lab')
  await app.visualLab.expectNotFound()
  await app.page.goto('/admin/network-lab')
  await app.networkLab.expectNotFound()
})
