import { randomUUID } from 'node:crypto'
import { merchantAlphaId } from '../auth/accounts'
import { test, expect } from '../fixtures'
import { expectStatus } from '../api/bff-client'
import type { BffClient } from '../api/bff-client'

async function injectActionable(
  api: BffClient,
  label: string,
) {
  const eventId = randomUUID()
  const injected = await api.operations.injectFeed({
    eventId,
    type: 'PAYMENT_FAILED',
    label,
    occurredAt: new Date().toISOString(),
    merchantId: merchantAlphaId,
    paymentOrderId: randomUUID(),
  })
  expectStatus(injected, 201)
  return eventId
}

test.describe('Notification center', { tag: ['@ops-notifications'] }, () => {
  test('PW-OPS-E2E-190…194 badge 3→4, popover, mark read, persist, read-all', async ({
    app,
    api,
    page,
  }) => {
    const client = api
    let fourthId: string | undefined
    await test.step('seed three unread payment-failed notifications', async () => {
      expect((await client.operations.markAllNotificationsRead()).status).toBe(204)
      await injectActionable(client, 'PO-190-A  FAILED')
      await injectActionable(client, 'PO-190-B  FAILED')
      await injectActionable(client, 'PO-190-C  FAILED')
      await app.overview.goto()
      await app.overview.expectLoaded()
      await expect(app.overview.notifications.unreadBadge()).toHaveText('3')
    })

    await test.step('surface the fourth notification in the overview badge', async () => {
      const fourth = await injectActionable(client, 'PO-190-D  FAILED')
      await expect.poll(async () => {
        await app.overview.goto()
        await app.overview.expectLoaded()
        return app.overview.notifications.unreadBadge().textContent()
      }).toBe('4')

      await app.overview.notifications.open()
      const listed = await client.operations.listNotifications()
      expectStatus(listed, 200)
      fourthId = listed.body.content?.find(item => item.eventId === fourth)?.notificationId
      expect(fourthId).toBeTruthy()
      await expect(app.overview.notifications.item(fourthId!)).toBeVisible()
    })

    await test.step('mark the notification read, persist it, then clear the inbox', async () => {
      await expect(app.overview.notifications.markReadButton(fourthId!)).toBeInViewport()
      await app.overview.notifications.markRead(fourthId!)
      await expect(app.overview.notifications.unreadBadge()).toHaveText('3')

      await page.reload()
      await app.overview.expectLoaded()
      await expect(app.overview.notifications.unreadBadge()).toHaveText('3')

      await app.overview.notifications.open()
      await app.overview.notifications.readAllButton().click()
      await expect(app.overview.notifications.bell()).toBeVisible()
      await expect(app.overview.notifications.unreadBadge()).toHaveCount(0)
    })

    expect(fourthId).toBeTruthy()
  })
})
