import { randomUUID } from 'node:crypto'
import { merchantAlphaId } from '../auth/accounts'
import { test, expect, requireApi } from '../fixtures'

async function injectActionable(
  api: { injectOpsFeed: (payload: Record<string, string>) => Promise<{ status: number, body?: { eventId?: string } }> },
  label: string,
) {
  const eventId = randomUUID()
  const injected = await api.injectOpsFeed({
    eventId,
    type: 'PAYMENT_FAILED',
    label,
    occurredAt: new Date().toISOString(),
    merchantId: merchantAlphaId,
    paymentOrderId: randomUUID(),
  })
  expect(injected.status).toBe(201)
  return eventId
}

test.describe('Notification center', { tag: ['@ops-notifications'] }, () => {
  test('PW-OPS-E2E-190…194 badge 3→4, popover, mark read, persist, read-all', async ({
    app,
    api,
    page,
  }) => {
    const client = requireApi(api)
    expect((await client.markAllNotificationsRead()).status).toBe(204)
    await injectActionable(client, 'PO-190-A  FAILED')
    await injectActionable(client, 'PO-190-B  FAILED')
    const third = await injectActionable(client, 'PO-190-C  FAILED')
    await app.overview.goto()
    await app.overview.expectLoaded()
    await app.overview.notifications.expectBadge(3)

    const fourth = await injectActionable(client, 'PO-190-D  FAILED')
    await expect.poll(async () => {
      await app.overview.goto()
      await app.overview.expectLoaded()
      return app.overview.notifications.unreadBadge().textContent()
    }).toBe('4')

    await app.overview.notifications.open()
    const listed = await client.listNotifications()
    expect(listed.status).toBe(200)
    const fourthId = listed.body.content?.find(item => item.eventId === fourth)?.notificationId
    expect(fourthId).toBeTruthy()
    await expect(app.overview.notifications.item(fourthId!)).toBeVisible()
    await expect(app.overview.notifications.markReadButton(fourthId!)).toBeInViewport()
    await app.overview.notifications.markRead(fourthId!)
    await app.overview.notifications.expectBadge(3)

    await page.reload()
    await app.overview.expectLoaded()
    await app.overview.notifications.expectBadge(3)

    await app.overview.notifications.open()
    await page.getByTestId('notification-read-all').click()
    await app.overview.notifications.expectBadge(0)

    expect(third).toBeTruthy()
  })
})
