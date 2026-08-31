import { expect, test } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { expectProblem } from '../utils/http'

const OTHER_ITEM = '00000000-0000-0000-0000-0000000000a2'

test('merchant manager sees only Alpha row; probe of other tenant is 404', async ({ actors }) => {
  const { app, api } = await actors.open('merchantManager')
  await app.rlsLab.goto()
  await app.rlsLab.expectLoaded()
  await expect(app.rlsLab.item('Alpha secret')).toBeVisible()
  await expect(app.rlsLab.item('Other tenant secret')).toHaveCount(0)
  await expect(app.rlsLab.comparePanel()).toHaveCount(0)
  const listed = await api.labs.listRlsItems()
  expectStatus(listed, 200)
  expect(listed.body.items.map(item => item.label)).toEqual(['Alpha secret'])
  await app.rlsLab.probe(OTHER_ITEM)
  await app.problem.expectVisible()
  await expect(app.problem.statusBadge(404)).toBeVisible()
  await expect(app.problem.errorCode()).toHaveText('not_found')
  expectProblem((await api.labs.getRlsItem(OTHER_ITEM)).body, 404, 'not_found')
  expect((await api.labs.rlsCompare()).status).toBe(403)
})

test('platform admin compare shows unprotected leak and zero without GUC', async ({ actors }) => {
  const { app, api } = await actors.open('platformAdmin')
  await app.rlsLab.goto()
  await app.rlsLab.expectLoaded()
  await expect(app.rlsLab.comparePanel()).toBeVisible()
  await app.rlsLab.loadCompare()
  await expect(app.rlsLab.restrictedWithoutTenant()).toHaveText('0')
  expect(Number(await app.rlsLab.unprotectedCount().innerText())).toBeGreaterThan(0)
  const compare = await api.labs.rlsCompare()
  expectStatus(compare, 200)
  expect(compare.body.restrictedWithoutTenantGuc).toBe(0)
  expect(compare.body.unprotected).toBeGreaterThanOrEqual(2)
})
