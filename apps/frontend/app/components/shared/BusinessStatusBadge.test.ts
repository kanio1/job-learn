/**
 * Property test for BusinessStatusBadge distinguishability.
 *
 * **Validates: Requirements 2.7, 8.1**
 *
 * Feature: payment-operations-dashboard, Property 2: Status badges are distinguishable without color
 *
 * Each Merchant and Payment_Status value must render a distinct, non-empty text label
 * so that statuses remain distinguishable when color is removed.
 */

// @vitest-environment nuxt
import { describe, it, expect } from 'vitest'
import fc from 'fast-check'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import BusinessStatusBadge from './BusinessStatusBadge.vue'

const MERCHANT_STATUSES = ['ACTIVE', 'PENDING', 'SUSPENDED'] as const
const PAYMENT_STATUSES = [
  'CREATED',
  'AUTHORIZED',
  'CAPTURED',
  'CANCELLED',
  'EXPIRED',
  'REFUNDED',
] as const
const ALL_STATUSES = [...MERCHANT_STATUSES, ...PAYMENT_STATUSES] as const

type KnownStatus = (typeof ALL_STATUSES)[number]

/**
 * Mounts the badge and returns the trimmed visible text.
 */
async function renderBadgeText(status: string): Promise<string> {
  const wrapper = await mountSuspended(BusinessStatusBadge, {
    props: { status },
  })
  return wrapper.text().trim()
}

describe('BusinessStatusBadge — Property 2: Status badges are distinguishable without color', () => {
  /**
   * Property: every known status renders a non-empty text label.
   * Runs ≥100 iterations by sampling from the full status union.
   */
  it('renders a non-empty label for every known status (≥100 iterations)', async () => {
    const statusArb = fc.constantFrom(...ALL_STATUSES)

    await fc.assert(
      fc.asyncProperty(statusArb, async (status: KnownStatus) => {
        const text = await renderBadgeText(status)
        expect(text.length).toBeGreaterThan(0)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property: all known status labels are unique — no two statuses share the same label.
   * This is an exhaustive check over the full set (9 statuses), verifying the
   * distinguishability contract without relying on color.
   */
  it('all status labels are distinct across the full Merchant + Payment_Status union', async () => {
    const renderedLabels = await Promise.all(
      ALL_STATUSES.map(async (status) => {
        const text = await renderBadgeText(status)
        return { status, text }
      }),
    )

    const labels = renderedLabels.map(({ text }) => text)
    const uniqueLabels = new Set(labels)

    // Every status must have a unique label
    expect(uniqueLabels.size).toBe(ALL_STATUSES.length)

    // Extra diagnostic: surface duplicate pairs when the assertion fails
    if (uniqueLabels.size !== ALL_STATUSES.length) {
      const duplicates = renderedLabels.filter(
        ({ text }, i) => renderedLabels.findIndex((r) => r.text === text) !== i,
      )
      throw new Error(
        `Duplicate labels found: ${duplicates.map(({ status, text }) => `${status}="${text}"`).join(', ')}`,
      )
    }
  })

  /**
   * Property: for any sampled pair of distinct statuses, their rendered labels differ.
   * fast-check generates pairs; ≥100 iterations.
   */
  it('any two distinct statuses render different labels (≥100 iterations)', async () => {
    const pairArb = fc
      .tuple(fc.constantFrom(...ALL_STATUSES), fc.constantFrom(...ALL_STATUSES))
      .filter(([a, b]) => a !== b)

    await fc.assert(
      fc.asyncProperty(pairArb, async ([statusA, statusB]) => {
        const [labelA, labelB] = await Promise.all([
          renderBadgeText(statusA),
          renderBadgeText(statusB),
        ])
        expect(labelA).not.toBe(labelB)
      }),
      { numRuns: 100 },
    )
  })
})
