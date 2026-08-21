import { uniqueCaseReference, uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { etagOf } from '../utils/http'
import { waitForBffRequest } from '../utils/wait-bff'

test.describe('Support bulk assign', () => {
  test('PW-OPS-E2E-150 success count 2 and two failure rows', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const merchant = await client.createMerchant(uniqueMerchantReference(testInfo), 'Bulk merchant')
    expect(merchant.status).toBe(201)
    const merchantId = merchant.body.merchantId!

    const okOne = await client.createSupportCase({
      merchantId,
      title: 'Bulk OK 1',
      caseReference: uniqueCaseReference(testInfo),
    })
    const okTwo = await client.createSupportCase({
      merchantId,
      title: 'Bulk OK 2',
      caseReference: uniqueCaseReference(testInfo),
    })
    const resolved = await client.createSupportCase({
      merchantId,
      title: 'Bulk resolved',
      caseReference: uniqueCaseReference(testInfo),
    })
    expect(okOne.status).toBe(201)
    expect(okTwo.status).toBe(201)
    expect(resolved.status).toBe(201)

    const resolvedId = resolved.body.caseId!
    let etag = etagOf(resolved.headers) ?? `"v${resolved.body.version ?? 0}"`
    etag = etagOf((await client.patchSupportCase(resolvedId, { status: 'IN_PROGRESS' }, etag)).headers)!
    etag = etagOf((await client.patchSupportCase(resolvedId, { status: 'WAITING' }, etag)).headers)!
    expect((await client.patchSupportCase(resolvedId, { status: 'RESOLVED' }, etag)).status).toBe(200)

    const resolvedTwo = await client.createSupportCase({
      merchantId,
      title: 'Bulk resolved 2',
      caseReference: uniqueCaseReference(testInfo),
    })
    expect(resolvedTwo.status).toBe(201)
    const resolvedTwoId = resolvedTwo.body.caseId!
    let etagTwo = etagOf(resolvedTwo.headers) ?? `"v${resolvedTwo.body.version ?? 0}"`
    etagTwo = etagOf((await client.patchSupportCase(resolvedTwoId, { status: 'IN_PROGRESS' }, etagTwo)).headers)!
    etagTwo = etagOf((await client.patchSupportCase(resolvedTwoId, { status: 'WAITING' }, etagTwo)).headers)!
    expect((await client.patchSupportCase(resolvedTwoId, { status: 'RESOLVED' }, etagTwo)).status).toBe(200)

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    await app.support.board.card(okOne.body.caseId!).select().click()
    await app.support.board.card(okTwo.body.caseId!).select().click()
    await app.support.board.card(resolvedId).select().click()
    await app.support.board.card(resolvedTwoId).select().click()

    await page.getByTestId('support-bulk-assign').click()
    await page.getByTestId('bulk-assign-assignee').fill('support.agent')
    await page.getByTestId('bulk-assign-submit').click()
    await expect(page.getByTestId('bulk-assign-result')).toBeVisible()
    await expect(page.getByTestId('bulk-success-count')).toContainText('2')
    await expect(page.locator('[data-testid^="bulk-failure-row-"]')).toHaveCount(2)
  })

  test('PW-OPS-E2E-151 progress visible then result modal', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const merchant = await client.createMerchant(uniqueMerchantReference(testInfo), 'Bulk progress merchant')
    expect(merchant.status).toBe(201)
    const created = await client.createSupportCase({
      merchantId: merchant.body.merchantId!,
      title: 'Progress case',
      caseReference: uniqueCaseReference(testInfo),
    })
    expect(created.status).toBe(201)

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    await app.support.board.card(created.body.caseId!).select().click()
    await page.getByTestId('support-bulk-assign').click()
    const progress = page.getByTestId('bulk-assign-progress')
    await page.getByTestId('bulk-assign-submit').click()
    await expect(progress.or(page.getByTestId('bulk-assign-result'))).toBeVisible()
    await expect(page.getByTestId('bulk-assign-result')).toBeVisible()
  })

  test('PW-OPS-E2E-152 Retry posts only failed ids', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const merchant = await client.createMerchant(uniqueMerchantReference(testInfo), 'Bulk retry merchant')
    expect(merchant.status).toBe(201)
    const merchantId = merchant.body.merchantId!
    const ok = await client.createSupportCase({
      merchantId,
      title: 'Retry OK',
      caseReference: uniqueCaseReference(testInfo),
    })
    const resolved = await client.createSupportCase({
      merchantId,
      title: 'Retry resolved',
      caseReference: uniqueCaseReference(testInfo),
    })
    expect(ok.status).toBe(201)
    expect(resolved.status).toBe(201)
    const resolvedId = resolved.body.caseId!
    let etag = etagOf(resolved.headers) ?? `"v${resolved.body.version ?? 0}"`
    etag = etagOf((await client.patchSupportCase(resolvedId, { status: 'IN_PROGRESS' }, etag)).headers)!
    etag = etagOf((await client.patchSupportCase(resolvedId, { status: 'WAITING' }, etag)).headers)!
    expect((await client.patchSupportCase(resolvedId, { status: 'RESOLVED' }, etag)).status).toBe(200)

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    await app.support.board.card(ok.body.caseId!).select().click()
    await app.support.board.card(resolvedId).select().click()
    await page.getByTestId('support-bulk-assign').click()
    await page.getByTestId('bulk-assign-submit').click()
    await expect(page.getByTestId('bulk-assign-result')).toBeVisible()

    const retry = waitForBffRequest(page, {
      method: 'POST',
      pathExact: '/api/support/cases/bulk-assign',
    })
    await page.getByTestId('bulk-retry-failed').click()
    const request = await retry
    const payload = request.postDataJSON() as { caseIds?: string[] }
    expect(payload.caseIds).toEqual([resolvedId])
  })

  test('PW-OPS-E2E-153 retry keeps succeeded count visible', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const merchant = await client.createMerchant(uniqueMerchantReference(testInfo), 'Bulk retry count')
    expect(merchant.status).toBe(201)
    const created = await client.createSupportCase({
      merchantId: merchant.body.merchantId!,
      title: 'Retry count',
      caseReference: uniqueCaseReference(testInfo),
    })
    expect(created.status).toBe(201)
    const caseId = created.body.caseId!
    let etag = etagOf(created.headers) ?? `"v${created.body.version ?? 0}"`
    etag = etagOf((await client.patchSupportCase(caseId, { status: 'IN_PROGRESS' }, etag)).headers)!
    etag = etagOf((await client.patchSupportCase(caseId, { status: 'WAITING' }, etag)).headers)!
    expect((await client.patchSupportCase(caseId, { status: 'RESOLVED' }, etag)).status).toBe(200)

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    await app.support.board.card(caseId).select().click()
    await page.getByTestId('support-bulk-assign').click()
    await page.getByTestId('bulk-assign-submit').click()
    await expect(page.getByTestId('bulk-success-count')).toContainText('0')
    await page.getByTestId('bulk-retry-failed').click()
    await expect(page.getByTestId('bulk-assign-result')).toBeVisible()
    await expect(page.getByTestId('bulk-success-count')).toBeVisible()
  })
})
