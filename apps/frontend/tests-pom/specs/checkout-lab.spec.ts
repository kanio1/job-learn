import { App } from '../pages/App'
import { uniqueExtOrderId } from '../data/factories'
import { test, expect } from '../fixtures'
import { parseJsonWithSchema } from '../utils/http'
import { expectNoTokenInBrowserStorage } from '../utils/storage-safety'
import { z } from 'zod'

const fulfillmentSchema = z.object({ status: z.string().optional() }).passthrough()

// Hosted PSP confirmation is an asynchronous integration contract with an
// explicit 45 s fulfillment oracle below; the suite default is too short.
test.describe.configure({ timeout: 60_000 })

async function requireCheckoutLab(app: App): Promise<void> {
  await app.merchants.goto()
  await expect(app.sidebar.merchants()).toBeVisible()
  await expect(app.sidebar.checkoutLab()).toBeVisible()
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
  await expect(app.checkoutBooking.hostedCheckoutLink()).toBeVisible()
  await expect(app.checkoutBooking.fulfillmentStatus()).toHaveText('AWAITING_PAYMENT')

  const hostedPromise = context.waitForEvent('page')
  await app.checkoutBooking.openHostedCheckout()
  const hostedPage = await hostedPromise
  const hosted = new App(hostedPage)
  await hosted.hostedCheckout.expectLoaded()
  await expect(hosted.idle.lock()).toHaveCount(0)
  await hosted.hostedCheckout.approve()
  await expect(hosted.hostedCheckout.outcome()).toBeVisible()
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
    await expect(app.checkoutInspector.processStatus()).toBeVisible()
  }).toPass({ timeout: 45_000 })
  await expectNoTokenInBrowserStorage(app.page)
})

test('approve without return still confirms fulfillment (PAY_NO_RETURN)', async ({ app, context }, testInfo) => {
  await requireCheckoutLab(app)
  await app.checkoutBooking.goto()
  await app.checkoutBooking.expectLoaded()
  await app.checkoutBooking.fillExtOrderId(uniqueExtOrderId(testInfo))
  await app.checkoutBooking.submit()
  await expect(app.checkoutBooking.hostedCheckoutLink()).toBeVisible()

  const sessionId = sessionIdFromHostedHref(await app.checkoutBooking.hostedCheckoutHref())
  expect(sessionId).toBeTruthy()

  const hostedPromise = context.waitForEvent('page')
  await app.checkoutBooking.openHostedCheckout()
  const hostedPage = await hostedPromise
  const hosted = new App(hostedPage)
  await hosted.hostedCheckout.expectLoaded()
  await hosted.hostedCheckout.approve()
  await expect(hosted.hostedCheckout.outcome()).toBeVisible()
  await hostedPage.close()
  await expect(app.page).not.toHaveURL(/\/checkout-lab\/return/)

  await expect.poll(async () => {
    const fulfillment = await app.page.request.get(`/api/checkout-lab/hosted/sessions/${sessionId}/fulfillment`)
    if (fulfillment.status() !== 200) {
      return fulfillment.status()
    }
    return parseJsonWithSchema(await fulfillment.text(), fulfillmentSchema, 'GET checkout fulfillment').status
  }, { timeout: 45_000 }).toBe('CONFIRMED')
})

test('lie return keeps fulfillment unconfirmed', async ({ app }, testInfo) => {
  await requireCheckoutLab(app)
  await app.checkoutBooking.goto()
  await app.checkoutBooking.expectLoaded()
  await app.checkoutBooking.fillExtOrderId(uniqueExtOrderId(testInfo))
  await app.checkoutBooking.submit()
  await expect(app.checkoutBooking.hostedCheckoutLink()).toBeVisible()

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
  await expect(app.checkoutBooking.fulfillmentStatus()).toHaveText('CONFIRMED')
  await expect(app.checkoutBooking.hostedCheckoutLink()).toHaveCount(0)
})

test('hosted decline leaves fulfillment cancelled', { tag: ['@ux'] }, async ({ app, context }, testInfo) => {
  await requireCheckoutLab(app)
  await app.checkoutBooking.goto()
  await app.checkoutBooking.expectLoaded()
  await app.checkoutBooking.fillExtOrderId(uniqueExtOrderId(testInfo))
  await app.checkoutBooking.submit()
  await expect(app.checkoutBooking.hostedCheckoutLink()).toBeVisible()

  const hostedPromise = context.waitForEvent('page')
  await app.checkoutBooking.openHostedCheckout()
  const hostedPage = await hostedPromise
  const hosted = new App(hostedPage)
  await hosted.hostedCheckout.expectLoaded()
  await hosted.hostedCheckout.decline()
  await expect(hosted.hostedCheckout.outcome()).toBeVisible()
  await hosted.hostedCheckout.returnToMerchant()
  await hostedPage.waitForURL(/\/checkout-lab\/return/)
  await hosted.checkoutReturn.expectLoaded()
  await expect(hosted.checkoutReturn.returnHint()).toContainText('failure')
  await expect(hosted.checkoutReturn.fulfillmentStatus()).toHaveText('CANCELLED', { timeout: 45_000 })
})

test('widget iframe Approve confirms fulfillment', async ({ app }, testInfo) => {
  await requireCheckoutLab(app)
  await app.checkoutBooking.goto()
  await app.checkoutBooking.expectLoaded()
  await app.checkoutBooking.fillExtOrderId(uniqueExtOrderId(testInfo))
  await app.checkoutBooking.submit()
  await expect(app.checkoutBooking.hostedCheckoutLink()).toBeVisible()

  const sessionId = sessionIdFromHostedHref(await app.checkoutBooking.hostedCheckoutHref())
  expect(sessionId).toBeTruthy()

  await app.checkoutWidget.goto()
  await app.checkoutWidget.expectLoaded()
  await app.checkoutWidget.loadSession(sessionId!)
  await app.checkoutWidget.approveInFrame()
  await expect(app.checkoutWidget.outcomeInFrame()).toContainText(/approved/i)

  await expect.poll(async () => {
    const fulfillment = await app.page.request.get(`/api/checkout-lab/hosted/sessions/${sessionId}/fulfillment`)
    if (fulfillment.status() !== 200) {
      return fulfillment.status()
    }
    return parseJsonWithSchema(await fulfillment.text(), fulfillmentSchema, 'GET checkout fulfillment').status
  }, { timeout: 45_000 }).toBe('CONFIRMED')
})
