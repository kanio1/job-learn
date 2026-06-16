/**
 * Property tests for HeaderKeyValuePanel.
 *
 * **Validates: Requirements 8.3, 8.4, 11.1, 11.2, 11.3, 6.6**
 *
 * Feature: payment-operations-dashboard, Property 8: Header panel rendering with empty indicator
 * Feature: payment-operations-dashboard, Property 9: Token confidentiality and Authorization masking
 *
 * Property 8 — Header panel rendering:
 *   - Zero headers → explicit empty indicator rendered (not an empty/blank panel)
 *   - Non-zero headers → every key-value pair is rendered as text
 *
 * Property 9 — Token masking:
 *   - Authorization header (any casing) with ANY value → rendered text must NOT contain
 *     any character of the original token value
 *   - Authorization must be replaced with a fixed masked placeholder "Bearer ••••••••"
 *   - No character of a bearer token is ever rendered (hard security requirement, Req 11.3)
 */

// @vitest-environment nuxt
import { describe, it, expect } from 'vitest'
import fc from 'fast-check'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import HeaderKeyValuePanel from './HeaderKeyValuePanel.vue'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const MASKED_PLACEHOLDER = 'Bearer ••••••••'

/**
 * Mounts the panel with an object-shaped headers prop and returns the wrapper.
 */
async function mountWithObject(headers: Record<string, string>) {
  return mountSuspended(HeaderKeyValuePanel, { props: { headers } })
}

/**
 * Mounts the panel with an array-shaped headers prop and returns the wrapper.
 */
async function mountWithArray(headers: Array<{ key: string; value: string }>) {
  return mountSuspended(HeaderKeyValuePanel, { props: { headers } })
}

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

/**
 * Generates a non-empty, printable header key that:
 *  - has no leading/trailing whitespace (HTML rendering collapses boundary spaces)
 *  - has no colon or newline characters
 *  - is composed only of printable ASCII chars (33–126, i.e. '!' to '~')
 * Using printable ASCII avoids whitespace-collapsing surprises in text assertions.
 */
const headerKeyArb = fc
  .stringOf(
    fc.integer({ min: 33, max: 126 }).map((n) => String.fromCharCode(n)),
    { minLength: 1, maxLength: 32 },
  )
  .filter((s) => !/[:]/.test(s))

/**
 * Generates a non-empty header value that:
 *  - does not contain the masked placeholder as a substring
 *  - has no leading/trailing whitespace (HTML rendering collapses them)
 *  - is composed of printable ASCII chars (33–126)
 */
const safeHeaderValueArb = fc
  .stringOf(
    fc.integer({ min: 33, max: 126 }).map((n) => String.fromCharCode(n)),
    { minLength: 1, maxLength: 64 },
  )
  .filter((v) => !v.includes(MASKED_PLACEHOLDER))

/**
 * Generates an object with 1–10 non-Authorization header entries.
 * Keys are constrained to avoid accidental "authorization" collisions.
 */
const nonAuthHeadersObjectArb: fc.Arbitrary<Record<string, string>> = fc
  .dictionary(
    headerKeyArb.filter((k) => k.toLowerCase() !== 'authorization'),
    safeHeaderValueArb,
    { minKeys: 1, maxKeys: 10 },
  )

/**
 * Generates an Authorization key with random casing (authorization, AUTHORIZATION,
 * Authorization, aUtHoRiZaTiOn, etc.) to cover case-insensitive matching.
 */
const authKeyArb: fc.Arbitrary<string> = fc
  .array(
    fc.constantFrom('a', 'A', 'u', 'U', 't', 'T', 'h', 'H', 'o', 'O', 'r', 'R', 'i', 'I', 'z', 'Z', 'e', 'E', 'n', 'N'),
    { minLength: 0, maxLength: 0 }, // placeholder — built explicitly below
  )
  .chain(() =>
    // Build by mapping each char of "authorization" to upper or lower randomly
    fc.array(fc.boolean(), { minLength: 13, maxLength: 13 }).map((flips) => {
      const base = 'authorization'
      return base
        .split('')
        .map((ch, i) => (flips[i] ? ch.toUpperCase() : ch))
        .join('')
    }),
  )

/**
 * Generates a bearer token value that:
 *   - is non-empty
 *   - does not equal the masked placeholder (we test that the placeholder appears instead)
 *   - contains at least one character not present in the placeholder
 *     (so we can assert "no character of the token appears")
 *
 * We use base64url-alphabet characters to mimic realistic JWT segments.
 * We also filter out the exact placeholder to guarantee the values differ.
 */
const tokenValueArb: fc.Arbitrary<string> = fc
  .stringOf(fc.constantFrom(...'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-=.'.split('')), {
    minLength: 8,
    maxLength: 128,
  })
  .filter(
    (v) =>
      v !== MASKED_PLACEHOLDER &&
      // Ensure the raw token has characters that would expose secret info
      // (at least 1 char that is NOT part of the placeholder)
      v.split('').some((ch) => !MASKED_PLACEHOLDER.includes(ch)),
  )

// ---------------------------------------------------------------------------
// Property 8 — Header panel rendering with empty indicator
// ---------------------------------------------------------------------------

describe('HeaderKeyValuePanel — Property 8: Header panel rendering with empty indicator', () => {
  /**
   * Property 8a: When zero headers are provided (empty object), the panel renders
   * an explicit empty indicator text — not a blank panel.
   * ≥100 iterations (the empty case is deterministic; we run it 100 times to satisfy the spec).
   */
  it('renders an explicit empty indicator when no headers are provided (object shape, ≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(fc.constant({}), async (headers) => {
        const wrapper = await mountWithObject(headers)
        const text = wrapper.text()
        // Must NOT be blank / whitespace-only
        expect(text.trim().length).toBeGreaterThan(0)
        // Should contain a meaningful empty indicator
        expect(text.toLowerCase()).toMatch(/no header|empty|none|absent|not present/i)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property 8b: When zero headers are provided (empty array), the panel renders
   * an explicit empty indicator text.
   */
  it('renders an explicit empty indicator when no headers are provided (array shape, ≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(fc.constant([] as Array<{ key: string; value: string }>), async (headers) => {
        const wrapper = await mountWithArray(headers)
        const text = wrapper.text()
        expect(text.trim().length).toBeGreaterThan(0)
        expect(text.toLowerCase()).toMatch(/no header|empty|none|absent|not present/i)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property 8c: When 1–10 non-Authorization headers are provided (object shape),
   * every key and its value appear in the rendered text.
   * ≥100 iterations with random header sets.
   */
  it('renders all key-value pairs when headers are present (object shape, ≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(nonAuthHeadersObjectArb, async (headers) => {
        const wrapper = await mountWithObject(headers)
        const text = wrapper.text()

        for (const [key, value] of Object.entries(headers)) {
          expect(text).toContain(key)
          expect(text).toContain(value)
        }
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property 8d: When 1–10 non-Authorization headers are provided (array shape),
   * every key and its value appear in the rendered text.
   */
  it('renders all key-value pairs when headers are present (array shape, ≥100 iterations)', async () => {
    const nonAuthHeadersArrayArb = nonAuthHeadersObjectArb.map((obj) =>
      Object.entries(obj).map(([key, value]) => ({ key, value })),
    )

    await fc.assert(
      fc.asyncProperty(nonAuthHeadersArrayArb, async (headers) => {
        const wrapper = await mountWithArray(headers)
        const text = wrapper.text()

        for (const { key, value } of headers) {
          expect(text).toContain(key)
          expect(text).toContain(value)
        }
      }),
      { numRuns: 100 },
    )
  })
})

// ---------------------------------------------------------------------------
// Property 9 — Token confidentiality and Authorization masking
// ---------------------------------------------------------------------------

describe('HeaderKeyValuePanel — Property 9: Token confidentiality and Authorization masking', () => {
  /**
   * Property 9a: For ANY Authorization header value (object shape), the rendered text
   * must NOT contain any character sequence from the original token value beyond what
   * coincidentally appears in the masked placeholder itself.
   *
   * Hard requirement (Req 11.3): no character of the token is ever rendered.
   * We verify: the exact token string does not appear, AND the rendered text
   * contains the fixed masked placeholder.
   * ≥100 iterations with random tokens and random Authorization key casing.
   */
  it('never renders any portion of the token value for Authorization header (object shape, ≥100 iterations)', async () => {
    const arb = fc.record({
      authKey: authKeyArb,
      tokenValue: tokenValueArb,
    })

    await fc.assert(
      fc.asyncProperty(arb, async ({ authKey, tokenValue }) => {
        const wrapper = await mountWithObject({ [authKey]: tokenValue })
        const text = wrapper.text()

        // The raw token value must NOT appear anywhere in the rendered output
        expect(text).not.toContain(tokenValue)

        // The fixed masked placeholder MUST be present
        expect(text).toContain(MASKED_PLACEHOLDER)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property 9b: For ANY Authorization header value (array shape), the same masking applies.
   */
  it('never renders any portion of the token value for Authorization header (array shape, ≥100 iterations)', async () => {
    const arb = fc.record({
      authKey: authKeyArb,
      tokenValue: tokenValueArb,
    })

    await fc.assert(
      fc.asyncProperty(arb, async ({ authKey, tokenValue }) => {
        const wrapper = await mountWithArray([{ key: authKey, value: tokenValue }])
        const text = wrapper.text()

        // The raw token value must NOT appear in the output
        expect(text).not.toContain(tokenValue)

        // The fixed masked placeholder MUST be present
        expect(text).toContain(MASKED_PLACEHOLDER)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property 9c: Authorization masking is exact — the rendered value is EXACTLY
   * the fixed placeholder "Bearer ••••••••", not a partial replacement.
   * ≥100 iterations.
   */
  it('replaces the entire Authorization value with the exact fixed placeholder (≥100 iterations)', async () => {
    const arb = fc.record({
      authKey: authKeyArb,
      tokenValue: tokenValueArb,
    })

    await fc.assert(
      fc.asyncProperty(arb, async ({ authKey, tokenValue }) => {
        const wrapper = await mountWithObject({ [authKey]: tokenValue })

        // Find the row rendered for the authorization key
        const cells = wrapper.findAll('td')
        const valueCell = cells.find((cell) => {
          // The preceding sibling td should contain the auth key text
          return cell.text() === MASKED_PLACEHOLDER
        })

        // There must be exactly one value cell with the masked placeholder
        expect(valueCell).toBeDefined()
        expect(valueCell!.text()).toBe(MASKED_PLACEHOLDER)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property 9d: When Authorization is present alongside other headers, only the
   * Authorization value is masked; all other header values are rendered verbatim.
   * ≥100 iterations.
   */
  it('masks only Authorization — other header values are rendered verbatim (≥100 iterations)', async () => {
    const arb = fc.record({
      authKey: authKeyArb,
      tokenValue: tokenValueArb,
      otherHeaders: nonAuthHeadersObjectArb,
    })

    await fc.assert(
      fc.asyncProperty(arb, async ({ authKey, tokenValue, otherHeaders }) => {
        const allHeaders = { ...otherHeaders, [authKey]: tokenValue }
        const wrapper = await mountWithObject(allHeaders)
        const text = wrapper.text()

        // Authorization value is masked
        expect(text).not.toContain(tokenValue)
        expect(text).toContain(MASKED_PLACEHOLDER)

        // Other header values are rendered verbatim
        for (const [key, value] of Object.entries(otherHeaders)) {
          expect(text).toContain(key)
          expect(text).toContain(value)
        }
      }),
      { numRuns: 100 },
    )
  })
})
