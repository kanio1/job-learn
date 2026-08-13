import { expect, test, type BrowserContext } from '@playwright/test'
import { mockAuthenticatedSession, dismissCheckerOverlay } from './merchant-support'

const SESSION_ID = '11111111-1111-4111-8111-111111111111'
const BOOKING_ID = '22222222-2222-4222-8222-222222222222'
const FULFILLMENT_ID = '33333333-3333-4333-8333-333333333333'
const CASH_BOOKING_ID = '44444444-4444-4444-8444-444444444444'
const SIMULATE_TOKEN = 'a'.repeat(64)

async function mockCheckoutLab(context: BrowserContext, fulfillmentStatus: string) {
  await context.route('**/api/checkout-lab/bookings', async route => {
    if (route.request().method() !== 'POST') {
      await route.fallback()
      return
    }
    const body = route.request().postDataJSON() as { mode?: string, extOrderId?: string }
    if (body.mode === 'CASH' || String(body.extOrderId || '').startsWith('CASH-')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: CASH_BOOKING_ID,
          mode: 'CASH',
          fulfillmentStatus: 'CONFIRMED',
          sessionId: null,
          redirectUri: null,
          validityUntil: null,
        }),
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      headers: { Location: `http://127.0.0.1:3000/psp/checkout/${SESSION_ID}` },
      body: JSON.stringify({
        bookingId: BOOKING_ID,
        mode: 'ONLINE',
        fulfillmentStatus: 'AWAITING_PAYMENT',
        sessionId: SESSION_ID,
        redirectUri: `http://127.0.0.1:3000/psp/checkout/${SESSION_ID}`,
        validityUntil: new Date(Date.now() + 60_000).toISOString(),
      }),
    })
  })
  await context.route('**/api/checkout-lab/hosted/sessions/**', async route => {
    const url = route.request().url()
    if (route.request().method() !== 'GET'
      || !url.includes(SESSION_ID)
      || url.includes('/fulfillment')
      || url.includes('/simulate')) {
      await route.fallback()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        sessionId: SESSION_ID,
        extOrderId: 'BOOK-PW',
        status: 'CREATED',
        amountMinor: 1999,
        currency: 'PLN',
        validityUntil: new Date(Date.now() + 60_000).toISOString(),
        continueUrl: `http://127.0.0.1:3000/checkout-lab/return?sessionId=${SESSION_ID}&status=success`,
        simulateToken: SIMULATE_TOKEN,
        simulateTokenExpiresAt: new Date(Date.now() + 60_000).toISOString(),
      }),
    })
  })
  await context.route('**/api/checkout-lab/hosted/sessions/**/simulate', async route => {
    if (route.request().method() !== 'POST' || !route.request().url().includes(SESSION_ID)) {
      await route.fallback()
      return
    }
    const token = route.request().headers()['lab-simulate-token']
    expect(token).toBe(SIMULATE_TOKEN)
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        sessionId: SESSION_ID,
        extOrderId: 'BOOK-PW',
        status: 'COMPLETED',
        amountMinor: 1999,
        currency: 'PLN',
        continueUrl: `http://127.0.0.1:3000/checkout-lab/return?sessionId=${SESSION_ID}&status=success`,
        simulateToken: null,
        simulateTokenExpiresAt: null,
      }),
    })
  })
  await context.route('**/api/checkout-lab/hosted/sessions/**/fulfillment', async route => {
    if (!route.request().url().includes(SESSION_ID)) {
      await route.fallback()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        fulfillmentId: FULFILLMENT_ID,
        sessionId: SESSION_ID,
        status: fulfillmentStatus,
      }),
    })
  })
}

test('hub teaches identity worlds and opens booking', async ({ page, context }) => {
  await mockAuthenticatedSession(page)
  await mockCheckoutLab(context, 'AWAITING_PAYMENT')
  await page.goto('/admin/checkout-lab')
  await expect(page.getByText('Hosted capability')).toBeVisible()
  await page.getByTestId('checkout-lab-open-booking').click()
  await expect(page.getByTestId('checkout-booking-submit')).toBeVisible()
})

test('sidebar nav link opens checkout lab hub', async ({ page, context }) => {
  await mockAuthenticatedSession(page)
  await mockCheckoutLab(context, 'AWAITING_PAYMENT')
  await page.goto('/admin/merchants')
  await page.getByTestId('nav-link-checkout-lab').click()
  await expect(page.getByText('Three identity worlds')).toBeVisible()
})

test('error lab card opens checkout lab hub', async ({ page, context }) => {
  await mockAuthenticatedSession(page)
  await mockCheckoutLab(context, 'AWAITING_PAYMENT')
  // /error-lab is SSR — page.route() does not mock the server-side session check.
  // Land on an SPA admin page first, then client-navigate via the sidebar.
  await page.goto('/admin/merchants')
  await expect(page.getByTestId('nav-link-error-lab')).toBeVisible()
  await page.getByTestId('nav-link-error-lab').click()
  await dismissCheckerOverlay(page)
  await expect(page.getByRole('heading', { name: 'Error Lab' })).toBeVisible()
  await page.getByTestId('checkout-lab-from-error-lab').click()
  await expect(page.getByText('Hosted capability')).toBeVisible()
})

test('cash booking confirms without hosted checkout button', async ({ page, context }) => {
  await mockAuthenticatedSession(page)
  await mockCheckoutLab(context, 'CONFIRMED')
  await page.goto('/admin/checkout-lab/booking')
  await page.getByTestId('checkout-booking-ext-order').fill(`CASH-${Date.now()}`)
  await page.getByTestId('checkout-booking-submit').click()
  await expect(page.getByTestId('fulfillment-status')).toHaveText('CONFIRMED')
  await expect(page.getByTestId('checkout-open-hosted')).toHaveCount(0)
})

test('multi-tab pay uses hosted approve and return fulfillment oracle', async ({ page, context }) => {
  await mockAuthenticatedSession(page)
  await mockCheckoutLab(context, 'CONFIRMED')
  await page.goto('/admin/checkout-lab/booking')
  await page.getByTestId('checkout-booking-submit').click()
  await expect(page.getByTestId('checkout-open-hosted')).toBeVisible()
  const newPagePromise = context.waitForEvent('page')
  await page.getByTestId('checkout-open-hosted').click()
  const hosted = await newPagePromise
  await hosted.waitForLoadState('networkidle')
  await expect(hosted.getByTestId('psp-approve')).toBeVisible()
  await hosted.getByTestId('psp-approve').click()
  await expect(hosted.getByTestId('psp-outcome')).toBeVisible()
  await hosted.goto(`/checkout-lab/return?sessionId=${SESSION_ID}&status=success`)
  await expect(hosted.getByTestId('fulfillment-status')).toHaveText('CONFIRMED')
  await hosted.close()
})

test('inspector shows Lab-Signature after load', async ({ page, context }) => {
  await mockAuthenticatedSession(page)
  await context.route(`**/api/checkout-lab/sessions/${SESSION_ID}/events`, async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([{
        eventId: 'evt_pw_1',
        sessionId: SESSION_ID,
        eventType: 'checkout.session.completed',
        signatureHeader: 't=1700000000,v1=abc123',
        processStatus: 'DONE',
        attempts: 1,
        ackStatus: 202,
        payload: { id: 'evt_pw_1' },
        receivedAt: new Date().toISOString(),
        lastError: null,
      }]),
    })
  })
  await context.route(`**/api/checkout-lab/sessions/${SESSION_ID}/deliveries`, async route => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' })
  })
  await context.route('**/api/checkout-lab/anomalies', async route => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' })
  })
  await page.goto('/admin/checkout-lab/inspector')
  await page.getByTestId('inspector-session-id').fill(SESSION_ID)
  await page.getByTestId('inspector-load').click()
  await expect(page.getByTestId('inspector-process-status')).toHaveText('DONE')
  await expect(page.getByText('Lab-Signature')).toBeVisible()
})

test('lie return keeps fulfillment unconfirmed', async ({ browser }) => {
  const context = await browser.newContext()
  await mockCheckoutLab(context, 'AWAITING_PAYMENT')
  const page = await context.newPage()
  await page.goto(`/checkout-lab/return?sessionId=${SESSION_ID}&status=success`)
  await expect(page.getByTestId('return-hint')).toHaveText('success')
  await expect(page.getByTestId('fulfillment-status')).toHaveText('AWAITING_PAYMENT')
  await context.close()
})

test('hosted checkout is reachable without a dashboard session', async ({ browser }) => {
  const context = await browser.newContext()
  await mockCheckoutLab(context, 'AWAITING_PAYMENT')
  const page = await context.newPage()
  await page.goto(`/psp/checkout/${SESSION_ID}`)
  await expect(page.getByTestId('psp-hosted-checkout')).toBeVisible()
  await expect(page.getByTestId('psp-approve')).toBeVisible()
  await expect(page.getByTestId('session-lab-idle-lock')).toHaveCount(0)
  await context.close()
})

test('widget iframe approve uses frameLocator', async ({ page, context }) => {
  await mockAuthenticatedSession(page)
  await mockCheckoutLab(context, 'AWAITING_PAYMENT')
  await page.goto('/admin/checkout-lab/widget')
  const sessionField = page.locator('[data-testid="widget-session-id"] input, input[data-testid="widget-session-id"]').first()
  await sessionField.fill(SESSION_ID)
  await page.getByTestId('widget-load').click()
  const frame = page.frameLocator('[data-testid="checkout-lab-widget-frame"]')
  await expect(frame.getByTestId('psp-approve')).toBeVisible()
  await frame.getByTestId('psp-approve').click()
  await expect(frame.getByTestId('psp-outcome')).toBeVisible()
})
