import { expect, test } from '../fixtures'
import { pomAuthFiles } from '../utils/env'
import { App } from '../pages/App'
import { BffClient } from '../api/bff-client'

const OTHER_ITEM = '00000000-0000-0000-0000-0000000000a2'

test('merchant manager sees only Alpha row; probe of other tenant is 404', async ({ browser, playwright }) => {
  const managerContext = await browser.newContext({ storageState: pomAuthFiles.merchantManager })
  const page = await managerContext.newPage()
  const app = new App(page)
  const api = await BffClient.create(playwright, pomAuthFiles.merchantManager)
  try {
    await app.rlsLab.goto()
    await app.rlsLab.expectLoaded()
    await expect(page.getByText('Alpha secret')).toBeVisible()
    await expect(page.getByText('Other tenant secret')).toHaveCount(0)
    await expect(page.getByTestId('rls-lab-compare-panel')).toHaveCount(0)

    const listed = await api.listRlsItems()
    expect(listed.status).toBe(200)
    expect(listed.body?.items?.map(item => item.label)).toEqual(['Alpha secret'])

    await app.rlsLab.probe(OTHER_ITEM)
    await app.problem.expectVisible()
    await app.problem.expectStatusBadge(404)
    await app.problem.expectError('not_found')

    const hidden = await api.getRlsItem(OTHER_ITEM)
    expect(hidden.status).toBe(404)
    expect(hidden.body?.error).toBe('not_found')

    const compare = await api.rlsCompare()
    expect(compare.status).toBe(403)
  }
  finally {
    await api.dispose()
    await managerContext.close()
  }
})

test('platform admin compare shows unprotected leak and zero without GUC', async ({ browser, playwright }) => {
  const adminContext = await browser.newContext({ storageState: pomAuthFiles.platformAdmin })
  const page = await adminContext.newPage()
  const app = new App(page)
  const api = await BffClient.create(playwright, pomAuthFiles.platformAdmin)
  try {
    await app.rlsLab.goto()
    await app.rlsLab.expectLoaded()
    await expect(page.getByTestId('rls-lab-compare-panel')).toBeVisible()
    await app.rlsLab.loadCompare()
    await expect(page.getByTestId('rls-lab-compare-restricted-no-tenant')).toHaveText('0')
    const unprotected = Number(await page.getByTestId('rls-lab-compare-unprotected').innerText())
    expect(unprotected).toBeGreaterThan(0)

    const compare = await api.rlsCompare()
    expect(compare.status).toBe(200)
    expect(compare.body?.restrictedWithoutTenantGuc).toBe(0)
    expect(compare.body?.unprotected).toBeGreaterThanOrEqual(2)
  }
  finally {
    await api.dispose()
    await adminContext.close()
  }
})
