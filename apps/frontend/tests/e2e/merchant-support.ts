import type { Page, Route } from '@playwright/test'
import { randomUUID } from 'node:crypto'

export interface Merchant {
  merchantId: string
  merchantReference: string
  displayName: string
  status: 'DRAFT' | 'ACTIVE' | 'SUSPENDED'
  createdAt: string
  updatedAt: string
}

export async function mockAuthenticatedSession(page: Page, username = 'platform.operator') {
  await page.route('**/api/_auth/session', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ loggedIn: true, user: { username } })
    })
  })
}

/**
 * Dismiss the vite-plugin-checker overlay that appears when there are TypeScript
 * errors in the project. The overlay intercepts pointer events and blocks clicks.
 * Safe to call even if the overlay is not present.
 */
export async function dismissCheckerOverlay(page: Page) {
  await page.evaluate(() => {
    const overlay = document.querySelector('vite-plugin-checker-error-overlay')
    if (overlay) overlay.remove()
  })
}

export async function mockMerchantApi(page: Page, initial: Merchant[] = []) {
  const merchants = [...initial]

  await page.route(/\/api\/merchants\/[^/]+\/activate$/, async route => {
    const merchant = merchantByRoute(route, merchants)
    if (!merchant) return notFound(route)
    if (merchant.status !== 'DRAFT') return invalidTransition(route)
    merchant.status = 'ACTIVE'
    merchant.updatedAt = new Date().toISOString()
    await json(route, 200, merchant)
  })

  await page.route(/\/api\/merchants\/[^/]+\/suspend$/, async route => {
    const merchant = merchantByRoute(route, merchants)
    if (!merchant) return notFound(route)
    if (merchant.status !== 'ACTIVE') return invalidTransition(route)
    merchant.status = 'SUSPENDED'
    merchant.updatedAt = new Date().toISOString()
    await json(route, 200, merchant)
  })

  await page.route(/\/api\/merchants\/[^/]+$/, async route => {
    const merchant = merchantByRoute(route, merchants)
    if (!merchant) return notFound(route)
    await json(route, 200, merchant)
  })

  await page.route('**/api/merchants', async route => {
    if (route.request().method() === 'GET') {
      await json(route, 200, { merchants })
      return
    }

    const body = route.request().postDataJSON() as { merchantReference: string; displayName: string }
    const reference = body.merchantReference.trim().toUpperCase()
    if (merchants.some(merchant => merchant.merchantReference === reference)) {
      await json(route, 409, { error: 'duplicate_merchant_reference', message: 'Duplicate merchant reference' })
      return
    }

    const now = new Date().toISOString()
    const merchant: Merchant = {
      merchantId: randomUUID(),
      merchantReference: reference,
      displayName: body.displayName.trim(),
      status: 'DRAFT',
      createdAt: now,
      updatedAt: now
    }
    merchants.unshift(merchant)
    await json(route, 201, merchant)
  })
}

export function uniqueReference(label: string) {
  return `MERCH-${label}-${randomUUID().slice(0, 8).toUpperCase()}`
}

export function merchant(reference: string, status: Merchant['status'] = 'DRAFT'): Merchant {
  const now = new Date().toISOString()
  return {
    merchantId: randomUUID(),
    merchantReference: reference,
    displayName: `${reference} Display`,
    status,
    createdAt: now,
    updatedAt: now
  }
}

async function json(route: Route, status: number, body: unknown) {
  await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
}

function merchantByRoute(route: Route, merchants: Merchant[]) {
  const id = route.request().url().match(/\/api\/merchants\/([^/]+)/)?.[1]
  return merchants.find(merchant => merchant.merchantId === id)
}

async function notFound(route: Route) {
  await json(route, 404, { error: 'not_found', message: 'Merchant not found' })
}

async function invalidTransition(route: Route) {
  await json(route, 409, { error: 'invalid_transition', message: 'Invalid merchant transition' })
}
