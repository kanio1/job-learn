import { uniqueCaseReference, uniqueMerchantReference } from '../data/factories'
import { test, expect } from '../fixtures'
import { etagOf } from '../utils/http'
import { waitForBffResponse } from '../utils/wait-bff'
import { type BffClient, expectStatus } from '../api/bff-client'

async function seedNewCase(
  api: BffClient,
  merchantId: string,
  testInfo: import('@playwright/test').TestInfo,
  title: string,
) {
  const created = await api.operations.createCase({
    merchantId,
    title,
    caseReference: uniqueCaseReference(testInfo),
  })
  expectStatus(created, 201)
  return {
    caseId: created.body.caseId!,
    caseReference: created.body.caseReference!,
    etag: etagOf(created.headers) ?? `"v${created.body.version ?? 0}"`,
  }
}

test.describe('Support work queue', { tag: ['@kanban'] }, () => {
  test('PW-OPS-E2E-110 menu Move NEW→IN_PROGRESS; PATCH 200; reload', async ({
    app,
    api,
    page,
  }, testInfo) => {
    const client = api
    const merchant = await client.merchants.create(
      uniqueMerchantReference(testInfo),
      'Support kanban merchant',
    )
    expectStatus(merchant, 201)
    const merchantId = merchant.body.merchantId!
    const seeded = await seedNewCase(client, merchantId, testInfo, 'Menu move')

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    await expect(app.support.board.card(seeded.caseId).root()).toBeVisible()

    const patched = waitForBffResponse(page, {
      method: 'PATCH',
      pathExact: `/api/support/cases/${seeded.caseId}`,
    })
    await app.support.board.card(seeded.caseId).moveTo('IN_PROGRESS')
    expect((await patched).status()).toBe(200)

    await page.reload()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    await expect(app.support.board.cardInColumn(seeded.caseId, 'IN_PROGRESS')).toBeVisible()
  })

  test('PW-OPS-E2E-111 dragTo NEW onto IN_PROGRESS', { tag: ['@flaky'] }, async ({
    app,
    api,
    page,
  }, testInfo) => {
    const client = api
    const merchant = await client.merchants.create(
      uniqueMerchantReference(testInfo),
      'Support drag merchant',
    )
    expectStatus(merchant, 201)
    const seeded = await seedNewCase(client, merchant.body.merchantId!, testInfo, 'Drag move')

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    const patched = waitForBffResponse(page, {
      method: 'PATCH',
      pathExact: `/api/support/cases/${seeded.caseId}`,
    })
    await app.support.board.card(seeded.caseId).dragToColumn(app.support.board.column('IN_PROGRESS'))
    expect((await patched).status()).toBe(200)
    await expect(app.support.board.cardInColumn(seeded.caseId, 'IN_PROGRESS')).toBeVisible()
  })

  test('PW-OPS-E2E-112 second context PATCH yields 412 rollback and toast', async ({
    app,
    api,
    page,
    actors,
  }, testInfo) => {
    const client = api
    const merchant = await client.merchants.create(
      uniqueMerchantReference(testInfo),
      'Support conflict merchant',
    )
    expectStatus(merchant, 201)
    const seeded = await seedNewCase(client, merchant.body.merchantId!, testInfo, 'Conflict move')

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    await expect(app.support.board.card(seeded.caseId).root()).toBeVisible()

    const operatorApi = (await actors.open('platformOperator')).api
    const raced = await operatorApi.operations.patchCase(
      seeded.caseId,
      { status: 'IN_PROGRESS' },
      seeded.etag,
    )
    expectStatus(raced, 200)

    const patched = waitForBffResponse(page, {
      method: 'PATCH',
      pathExact: `/api/support/cases/${seeded.caseId}`,
    })
    await app.support.board.card(seeded.caseId).moveTo('IN_PROGRESS')
    expect((await patched).status()).toBe(412)
    await expect(app.support.board.cardInColumn(seeded.caseId, 'NEW')).toBeVisible()
    await expect(app.support.errorToast()).toBeVisible()
  })

  test('PW-OPS-E2E-113 board fragment matches ARIA snapshot', async ({ app, api }, testInfo) => {
    const client = api
    const merchant = await client.merchants.create(
      uniqueMerchantReference(testInfo),
      'Support aria merchant',
    )
    expectStatus(merchant, 201)
    await seedNewCase(client, merchant.body.merchantId!, testInfo, 'Aria case')

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    await expect(app.support.board.root()).toMatchAriaSnapshot(`
      - region "NEW column"
      - region "IN_PROGRESS column"
      - region "WAITING column"
      - region "RESOLVED column"
    `)
  })

  test('PW-OPS-E2E-114 illegal drop onto RESOLVED stays in NEW', async ({
    app,
    api,
    page,
  }, testInfo) => {
    const client = api
    const merchant = await client.merchants.create(
      uniqueMerchantReference(testInfo),
      'Support illegal merchant',
    )
    expectStatus(merchant, 201)
    const seeded = await seedNewCase(client, merchant.body.merchantId!, testInfo, 'Illegal drop')

    await app.support.goto()
    await app.support.expectLoaded()
    await app.support.openWorkQueue()
    const patched = waitForBffResponse(page, {
      method: 'PATCH',
      pathExact: `/api/support/cases/${seeded.caseId}`,
    })
    await app.support.board.card(seeded.caseId).dragToColumn(app.support.board.column('RESOLVED'))
    expect((await patched).status()).toBe(409)
    await expect(app.support.board.cardInColumn(seeded.caseId, 'NEW')).toBeVisible()
  })
})
