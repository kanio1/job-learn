/**
 * Property test for HttpStatusBadge HTTP status category mapping.
 *
 * **Validates: Requirements 8.2, 6.3**
 *
 * Feature: payment-operations-dashboard, Property 5: HTTP status category mapping
 *
 * For every HTTP status code in 100–599:
 * - The badge renders the status code in its text.
 * - The badge renders the correct leading-digit category label.
 * - Every code maps to exactly one category (no code falls through to "Unknown").
 */

// @vitest-environment nuxt
import { describe, it, expect } from 'vitest'
import fc from 'fast-check'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import HttpStatusBadge from './HttpStatusBadge.vue'

/**
 * Expected category label per leading digit, mirroring the component implementation.
 */
const EXPECTED_CATEGORY: Record<number, string> = {
  1: 'Informational',
  2: 'Success',
  3: 'Redirection',
  4: 'Client Error',
  5: 'Server Error',
}

/**
 * Mounts the badge for the given status and returns its trimmed text content.
 */
async function renderBadge(status: number): Promise<string> {
  const wrapper = await mountSuspended(HttpStatusBadge, {
    props: { status },
  })
  return wrapper.text().trim()
}

describe('HttpStatusBadge — Property 5: HTTP status category mapping', () => {
  /**
   * Property: for every status code 100–599, the badge text contains the numeric code itself.
   * Validates: Requirements 8.2, 6.3 — status code must be rendered in the badge.
   * fast-check ≥100 iterations.
   */
  it('renders the status code in badge text for every code 100–599 (≥100 iterations)', async () => {
    const statusArb = fc.integer({ min: 100, max: 599 })

    await fc.assert(
      fc.asyncProperty(statusArb, async (status) => {
        const text = await renderBadge(status)
        expect(text).toContain(String(status))
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property: for every status code 100–599, the badge text contains the expected
   * leading-digit category label (case-insensitive match against the known category map).
   * Validates: Requirements 8.2, 6.3 — correct category must be shown.
   * fast-check ≥100 iterations.
   */
  it('renders the correct category label for every code 100–599 (≥100 iterations)', async () => {
    const statusArb = fc.integer({ min: 100, max: 599 })

    await fc.assert(
      fc.asyncProperty(statusArb, async (status) => {
        const leadingDigit = Math.floor(status / 100)
        const expectedLabel = EXPECTED_CATEGORY[leadingDigit]

        const text = await renderBadge(status)
        expect(text.toLowerCase()).toContain(expectedLabel.toLowerCase())
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property: no status code 100–599 falls through to the "Unknown" category.
   * Every code maps to exactly one of the five defined categories.
   * fast-check ≥100 iterations.
   */
  it('every code 100–599 maps to exactly one defined category — never "Unknown" (≥100 iterations)', async () => {
    const statusArb = fc.integer({ min: 100, max: 599 })

    await fc.assert(
      fc.asyncProperty(statusArb, async (status) => {
        const text = await renderBadge(status)
        expect(text.toLowerCase()).not.toContain('unknown')
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Exhaustive example check: one representative code per category.
   * Validates boundary and mid-range codes for each class.
   */
  it('maps representative codes to their categories exhaustively', async () => {
    const examples: Array<{ status: number; expectedLabel: string }> = [
      { status: 100, expectedLabel: 'Informational' },
      { status: 199, expectedLabel: 'Informational' },
      { status: 200, expectedLabel: 'Success' },
      { status: 299, expectedLabel: 'Success' },
      { status: 301, expectedLabel: 'Redirection' },
      { status: 399, expectedLabel: 'Redirection' },
      { status: 400, expectedLabel: 'Client Error' },
      { status: 404, expectedLabel: 'Client Error' },
      { status: 422, expectedLabel: 'Client Error' },
      { status: 499, expectedLabel: 'Client Error' },
      { status: 500, expectedLabel: 'Server Error' },
      { status: 503, expectedLabel: 'Server Error' },
      { status: 599, expectedLabel: 'Server Error' },
    ]

    for (const { status, expectedLabel } of examples) {
      const text = await renderBadge(status)
      expect(text, `status ${status}`).toContain(String(status))
      expect(text.toLowerCase(), `status ${status} category`).toContain(
        expectedLabel.toLowerCase(),
      )
    }
  })
})
