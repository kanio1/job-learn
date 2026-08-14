import { App } from '../pages/App'
import { uniqueExtOrderId } from '../data/factories'
import { test, expect } from '../fixtures'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'

async function requireCheckoutLab(app: App): Promise<void> {
  await app.page.goto('/admin/merchants')
  await expect(app.page.getByTestId('nav-link-merchants')).toBeVisible()
  const nav = app.page.getByTestId('nav-link-checkout-lab')
  if (await nav.count() === 0) {
    test.skip(true, 'Checkout Lab nav is hidden (checkoutLabEnabled=false)')
  }
}

function sessionIdFromHostedHref(href: string | null): string | null {
  if (!href) {
    return null
  }
  const match = href.match(/\/psp\/checkout\/([0-9a-f-]{36})/i)
  return match?.[1] ?? null
}

test('hub opens booking; online pay uses hosted tab and fulfillment oracle', async ({ app, context }, testInfo) => {
  await requireCheckoutLab(app)
  await app.checkoutHub.goto()
  await app.checkoutHub.expectLoaded()
  await app.checkoutHub.openBooking()
  await app.checkoutBooking.expectLoaded()

  const extOrderId = uniqueExtOrderId(testInfo)
  await app.checkoutBooking.fillExtOrderId(extOrderId)
  await app.checkoutBooking.submit()
  await expect(app.page.getByTestId('checkout-open-hosted')).toBeVisible()
  await expect(app.page.getByTestId('fulfillment-status')).toHaveText('AWAITING_PAYMENT')

  const hostedPromise = context.waitForEvent('page')
  await app.checkoutBooking.openHostedCheckout()
  const hostedPage = await hostedPromise
  const hosted = new App(hostedPage)
  await hosted.hostedCheckout.expectLoaded()
  await hosted.hostedCheckout.approve()
  await hosted.hostedCheckout.expectOutcome()
  await hosted.hostedCheckout.returnToMerchant()
  await hostedPage.waitForURL(/\/checkout-lab\/return/)
  await hosted.checkoutReturn.expectLoaded()
  await expect(hosted.checkoutReturn.returnHint()).toContainText('success')
  await expect(hosted.checkoutReturn.fulfillmentStatus()).toHaveText('CONFIRMED', { timeout: 45_000 })

  const sessionId = new URL(hostedPage.url()).searchParams.get('sessionId')
  expect(sessionId, 'hosted return must expose sessionId').toBeTruthy()

  await app.checkoutInspector.goto()
  await app.checkoutInspector.expectLoaded()
  await expect(async () => {
    await app.checkoutInspector.loadSession(sessionId!)
    await expect(app.page.getByTestId('inspector-process-status')).toBeVisible()
  }).toPass({ timeout: 45_000 })
  await expectNoTokenInBrowserStorage(app.page)
})

test('lie return keeps fulfillment unconfirmed', async ({ app }, testInfo) => {
  await requireCheckoutLab(app)
  await app.checkoutBooking.goto()
  await app.checkoutBooking.expectLoaded()
  await app.checkoutBooking.fillExtOrderId(uniqueExtOrderId(testInfo))
  await app.checkoutBooking.submit()
  await expect(app.page.getByTestId('checkout-open-hosted')).toBeVisible()

  const sessionId = sessionIdFromHostedHref(await app.checkoutBooking.hostedCheckoutHref())
  expect(sessionId).toBeTruthy()

  await app.page.goto(`/checkout-lab/return?sessionId=${sessionId}&status=success`)
  await app.checkoutReturn.expectLoaded()
  await expect(app.checkoutReturn.returnHint()).toHaveText('success')
  await expect(app.checkoutReturn.fulfillmentStatus()).not.toHaveText('CONFIRMED')
  await expect(app.checkoutReturn.fulfillmentStatus()).toHaveText(/AWAITING_PAYMENT|UNKNOWN/)
})

test('cash booking confirms fulfillment without hosted checkout', { tag: ['@ux'] }, async ({ app }, testInfo) => {
  await requireCheckoutLab(app)
  await app.checkoutBooking.goto()
  await app.checkoutBooking.expectLoaded()
  await app.checkoutBooking.fillExtOrderId(uniqueExtOrderId(testInfo))
  await app.checkoutBooking.chooseMode('CASH')
  await app.checkoutBooking.submit()
  await expect(app.page.getByTestId('fulfillment-status')).toHaveText('CONFIRMED')
  await expect(app.page.getByTestId('checkout-open-hosted')).toHaveCount(0)
})

test('hosted decline leaves fulfillment cancelled', { tag: ['@ux'] }, async ({ app, context }, testInfo) => {
  await requireCheckoutLab(app)
  await app.checkoutBooking.goto()
  await app.checkoutBooking.expectLoaded()
  await app.checkoutBooking.fillExtOrderId(uniqueExtOrderId(testInfo))
  await app.checkoutBooking.submit()
  await expect(app.page.getByTestId('checkout-open-hosted')).toBeVisible()

  const hostedPromise = context.waitForEvent('page')
  await app.checkoutBooking.openHostedCheckout()
  const hostedPage = await hostedPromise
  const hosted = new App(hostedPage)
  await hosted.hostedCheckout.expectLoaded()
  await hosted.hostedCheckout.decline()
  await hosted.hostedCheckout.expectOutcome()
  await hosted.hostedCheckout.returnToMerchant()
  await hostedPage.waitForURL(/\/checkout-lab\/return/)
  await hosted.checkoutReturn.expectLoaded()
  await expect(hosted.checkoutReturn.returnHint()).toContainText('failure')
  await expect(hosted.checkoutReturn.fulfillmentStatus()).toHaveText('CANCELLED', { timeout: 45_000 })
})
