import { uniqueCaseReference, uniqueMerchantReference } from '../data/factories'
import { test, expect, requireApi } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import { etagOf } from '../utils/http'
import { waitForBffRequest } from '../utils/wait-bff'

test.describe('Support bulk assign', () => {
  test('PW-OPS-E2E-150 success count 2 and two failure rows', async ({ app, api }, testInfo) => {
    const client = requireApi(api)
    const merchant = await client.createMerchant(uniqueMerchantReference(testInfo), 'Bulk merchant')
    expectStatus(merchant, 201)
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
    expectStatus(okOne, 201)
    expectStatus(okTwo, 201)
    expectStatus(resolved, 201)

    const resolvedId = resolved.body.caseId!
    let etag = etagOf(resolved.headers) ?? `"v${resolved.body.version ?? 0}"`
    etag = etagOf((await client.patchSupportCase(resolvedId, { status: 'IN_PROGRESS' }, etag)).headers)!
    etag = etagOf((await client.patchSupportCase(resolvedId, { status: 'WAITING' }, etag)).headers)!
    expect((await client.patchSupportCase(resolvedId, { status: 'RESOLVED' }, etag)).status).toBe(200)

    const resolvedTwoId = await test.step('seed two assignable cases and one conflict', async () => {
      const resolvedTwo = await client.createSupportCase({
        merchantId,
        title: 'Bulk resolved 2',
        caseReference: uniqueCaseReference(testInfo),
      })
      expectStatus(resolvedTwo, 201)
      const id = resolvedTwo.body.caseId
      let etagTwo = etagOf(resolvedTwo.headers) ?? `"v${resolvedTwo.body.version ?? 0}"`
      etagTwo = etagOf((await client.patchSupportCase(id, { status: 'IN_PROGRESS' }, etagTwo)).headers)!
      etagTwo = etagOf((await client.patchSupportCase(id, { status: 'WAITING' }, etagTwo)).headers)!
      expect((await client.patchSupportCase(id, { status: 'RESOLVED' }, etagTwo)).status).toBe(200)
      return id
    })
    await test.step('select the cases and bulk assign to the agent', async () => {
      await app.support.goto()
      await app.support.expectLoaded()
      await app.support.openWorkQueue()
      await app.support.selectCases([okOne.body.caseId!, okTwo.body.caseId!, resolvedId, resolvedTwoId])

      await app.support.openBulkAssign()
      await app.support.assignTo('support.agent')
      await expect(app.support.bulkResult()).toBeVisible()
      await expect(app.support.bulkSuccessCount()).toContainText('2')
      await expect(app.support.bulkFailureRows()).toHaveCount(2)
    })
  })

  test('PW-OPS-E2E-151 progress visible then result modal', async ({ app, api }, testInfo) => {
    const client = requireApi(api)
    const merchant = await client.createMerchant(uniqueMerchantReference(testInfo), 'Bulk progress merchant')
    expectStatus(merchant, 201)
    const created = await client.createSupportCase({
      merchantId: merchant.body.merchantId!,
      title: 'Progress case',
      caseReference: uniqueCaseReference(testInfo),
    })
    expectStatus(created, 201)

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    await app.support.selectCases([created.body.caseId!])
    await app.support.openBulkAssign()
    const progress = app.support.bulkProgress()
    await app.support.submitBulkAssign()
    await expect(progress.or(app.support.bulkResult())).toBeVisible()
    await expect(app.support.bulkResult()).toBeVisible()
  })

  test('PW-OPS-E2E-152 Retry posts only failed ids', async ({ app, api, page }, testInfo) => {
    const client = requireApi(api)
    const merchant = await client.createMerchant(uniqueMerchantReference(testInfo), 'Bulk retry merchant')
    expectStatus(merchant, 201)
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
    expectStatus(ok, 201)
    expectStatus(resolved, 201)
    const resolvedId = resolved.body.caseId!
    let etag = etagOf(resolved.headers) ?? `"v${resolved.body.version ?? 0}"`
    etag = etagOf((await client.patchSupportCase(resolvedId, { status: 'IN_PROGRESS' }, etag)).headers)!
    etag = etagOf((await client.patchSupportCase(resolvedId, { status: 'WAITING' }, etag)).headers)!
    expect((await client.patchSupportCase(resolvedId, { status: 'RESOLVED' }, etag)).status).toBe(200)

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    await app.support.selectCases([ok.body.caseId!, resolvedId])
    await app.support.openBulkAssign()
    await app.support.submitBulkAssign()
    await expect(app.support.bulkResult()).toBeVisible()

    const retry = waitForBffRequest(page, {
      method: 'POST',
      pathExact: '/api/support/cases/bulk-assign',
    })
    await app.support.retryFailed()
    const request = await retry
    const payload = request.postDataJSON() as { caseIds?: string[] }
    expect(payload.caseIds).toEqual([resolvedId])
  })

  test('PW-OPS-E2E-153 retry keeps succeeded count visible', async ({ app, api }, testInfo) => {
    const client = requireApi(api)
    const merchant = await client.createMerchant(uniqueMerchantReference(testInfo), 'Bulk retry count')
    expectStatus(merchant, 201)
    const created = await client.createSupportCase({
      merchantId: merchant.body.merchantId!,
      title: 'Retry count',
      caseReference: uniqueCaseReference(testInfo),
    })
    expectStatus(created, 201)
    const caseId = created.body.caseId!
    let etag = etagOf(created.headers) ?? `"v${created.body.version ?? 0}"`
    etag = etagOf((await client.patchSupportCase(caseId, { status: 'IN_PROGRESS' }, etag)).headers)!
    etag = etagOf((await client.patchSupportCase(caseId, { status: 'WAITING' }, etag)).headers)!
    expect((await client.patchSupportCase(caseId, { status: 'RESOLVED' }, etag)).status).toBe(200)

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    await app.support.selectCases([caseId])
    await app.support.openBulkAssign()
    await app.support.submitBulkAssign()
    await expect(app.support.bulkSuccessCount()).toContainText('0')
    await app.support.retryFailed()
    await expect(app.support.bulkResult()).toBeVisible()
    await expect(app.support.bulkSuccessCount()).toBeVisible()
  })
})
