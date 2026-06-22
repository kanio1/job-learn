/**
 * Property test for ProblemDetailsCard rendering with empty indicators.
 *
 * **Validates: Requirements 8.5**
 *
 * Feature: payment-operations-dashboard, Property 7: Problem details rendering with empty indicators
 *
 * For any combination of present/absent standard problem+json members
 * (type, title, status, detail, instance):
 *   - Present members are rendered as visible text in the card
 *   - Absent/null/undefined members show an explicit empty indicator ("—") rather than blank space
 *   - Extension members (via .passthrough()) are preserved and do not break rendering
 *
 * Uses fast-check to generate arbitrary combinations of present/absent members.
 * Minimum 100 iterations.
 */

// @vitest-environment nuxt
import { describe, it, expect } from 'vitest'
import fc from 'fast-check'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { defineComponent, h } from 'vue'
import ProblemDetailsCard from './ProblemDetailsCard.vue'
import type { ProblemDetails } from '~/types/api'

const EMPTY_INDICATOR = '—'

/**
 * A stub for HttpStatusBadge that renders the status code as plain text
 * (Nuxt auto-imports are not available in unit test environment).
 */
const HttpStatusBadgeStub = defineComponent({
  props: { status: { type: Number, required: true } },
  render() {
    return h('span', { class: 'http-status-stub' }, String(this.status))
  },
})

/**
 * Mounts ProblemDetailsCard with the given problem prop and a stubbed HttpStatusBadge.
 */
async function renderCard(problem: ProblemDetails) {
  return mountSuspended(ProblemDetailsCard, {
    props: { problem },
    global: {
      stubs: {
        HttpStatusBadge: HttpStatusBadgeStub,
        // UCard and UIcon are Nuxt UI — stub them to pass through slot content
        UCard: { template: '<div><slot name="header" /><slot /></div>' },
        UIcon: { template: '<span />' },
      },
    },
  })
}

/**
 * Arbitrary for a non-empty string without leading/trailing whitespace.
 * Restricted to printable ASCII chars that are safe for exact text matching.
 */
const safeStringArb = fc
  .string({ minLength: 1, maxLength: 60 })
  .filter((s) => s.trim() === s && s.length > 0)

/**
 * Arbitrary for an HTTP status code in the valid problem+json range.
 */
const httpStatusArb = fc.integer({ min: 100, max: 599 })

/**
 * Arbitrary that produces either a concrete value or undefined (absent).
 */
function presentOrAbsent<T>(valueArb: fc.Arbitrary<T>): fc.Arbitrary<T | undefined> {
  return fc.oneof(valueArb, fc.constant(undefined))
}

/**
 * Arbitrary for a ProblemDetails object with any combination of
 * present/absent standard members.
 */
const problemDetailsArb: fc.Arbitrary<ProblemDetails> = fc.record({
  type: presentOrAbsent(safeStringArb),
  title: presentOrAbsent(safeStringArb),
  status: presentOrAbsent(httpStatusArb),
  detail: presentOrAbsent(safeStringArb),
  instance: presentOrAbsent(safeStringArb),
})

/**
 * Arbitrary for an extension member key that does not clash with standard members.
 * Keys are simple alphanumeric identifiers for safe DOM text matching.
 */
const extensionKeyArb = fc
  .stringMatching(/^[a-z][a-z0-9]{0,15}$/)
  .filter((k) => !['type', 'title', 'status', 'detail', 'instance'].includes(k))

describe('ProblemDetailsCard — Property 7: Problem details rendering with empty indicators', () => {
  /**
   * Property: the card always renders data-testid="problem-details-card".
   */
  it('always renders the problem-details-card test-id element (≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(problemDetailsArb, async (problem) => {
        const wrapper = await renderCard(problem)
        expect(wrapper.find('[data-testid="problem-details-card"]').exists()).toBe(true)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property: when `type` is a non-empty string, it appears in the rendered text.
   */
  it('renders a present "type" value as visible text (≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(
        problemDetailsArb.filter((p) => typeof p.type === 'string' && p.type.length > 0),
        async (problem) => {
          const wrapper = await renderCard(problem)
          expect(wrapper.text()).toContain(problem.type as string)
        },
      ),
      { numRuns: 100 },
    )
  })

  /**
   * Property: when `title` is a non-empty string, it appears in the rendered text.
   */
  it('renders a present "title" value as visible text (≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(
        problemDetailsArb.filter((p) => typeof p.title === 'string' && p.title.length > 0),
        async (problem) => {
          const wrapper = await renderCard(problem)
          expect(wrapper.text()).toContain(problem.title as string)
        },
      ),
      { numRuns: 100 },
    )
  })

  /**
   * Property: when `detail` is a non-empty string, it appears in the rendered text.
   */
  it('renders a present "detail" value as visible text (≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(
        problemDetailsArb.filter((p) => typeof p.detail === 'string' && p.detail.length > 0),
        async (problem) => {
          const wrapper = await renderCard(problem)
          expect(wrapper.text()).toContain(problem.detail as string)
        },
      ),
      { numRuns: 100 },
    )
  })

  /**
   * Property: when `instance` is a non-empty string, it appears in the rendered text.
   */
  it('renders a present "instance" value as visible text (≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(
        problemDetailsArb.filter((p) => typeof p.instance === 'string' && p.instance.length > 0),
        async (problem) => {
          const wrapper = await renderCard(problem)
          expect(wrapper.text()).toContain(problem.instance as string)
        },
      ),
      { numRuns: 100 },
    )
  })

  /**
   * Property: when `status` is a valid integer, the status code is visible via
   * the HttpStatusBadge stub (rendered as plain text).
   */
  it('renders a present "status" value visibly via the status badge (≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(
        problemDetailsArb.filter((p) => typeof p.status === 'number'),
        async (problem) => {
          const wrapper = await renderCard(problem)
          expect(wrapper.text()).toContain(String(problem.status))
        },
      ),
      { numRuns: 100 },
    )
  })

  /**
   * Property: when `type` is absent (undefined), the empty indicator "—" appears.
   */
  it('shows empty indicator "—" when "type" is absent (≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(
        problemDetailsArb.map((p) => ({ ...p, type: undefined })),
        async (problem) => {
          const wrapper = await renderCard(problem)
          expect(wrapper.text()).toContain(EMPTY_INDICATOR)
        },
      ),
      { numRuns: 100 },
    )
  })

  /**
   * Property: when `title` is absent (undefined), the empty indicator "—" appears.
   */
  it('shows empty indicator "—" when "title" is absent (≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(
        problemDetailsArb.map((p) => ({ ...p, title: undefined })),
        async (problem) => {
          const wrapper = await renderCard(problem)
          expect(wrapper.text()).toContain(EMPTY_INDICATOR)
        },
      ),
      { numRuns: 100 },
    )
  })

  /**
   * Property: when `detail` is absent (undefined), the empty indicator "—" appears.
   */
  it('shows empty indicator "—" when "detail" is absent (≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(
        problemDetailsArb.map((p) => ({ ...p, detail: undefined })),
        async (problem) => {
          const wrapper = await renderCard(problem)
          expect(wrapper.text()).toContain(EMPTY_INDICATOR)
        },
      ),
      { numRuns: 100 },
    )
  })

  /**
   * Property: when `instance` is absent (undefined), the empty indicator "—" appears.
   */
  it('shows empty indicator "—" when "instance" is absent (≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(
        problemDetailsArb.map((p) => ({ ...p, instance: undefined })),
        async (problem) => {
          const wrapper = await renderCard(problem)
          expect(wrapper.text()).toContain(EMPTY_INDICATOR)
        },
      ),
      { numRuns: 100 },
    )
  })

  /**
   * Property: when `status` is absent (undefined), the empty indicator "—" appears.
   * The component uses v-if for status (renders HttpStatusBadge when present, "—" when absent).
   */
  it('shows empty indicator "—" when "status" is absent (≥100 iterations)', async () => {
    await fc.assert(
      fc.asyncProperty(
        problemDetailsArb.map((p) => ({ ...p, status: undefined })),
        async (problem) => {
          const wrapper = await renderCard(problem)
          expect(wrapper.text()).toContain(EMPTY_INDICATOR)
        },
      ),
      { numRuns: 100 },
    )
  })

  /**
   * Property: when ALL standard members are absent, the empty indicator appears
   * at least 5 times (once per field row).
   */
  it('shows empty indicator for every absent standard member when all are absent (≥100 iterations)', async () => {
    const allAbsentProblem: ProblemDetails = {
      type: undefined,
      title: undefined,
      status: undefined,
      detail: undefined,
      instance: undefined,
    }

    await fc.assert(
      fc.asyncProperty(fc.constant(allAbsentProblem), async (problem) => {
        const wrapper = await renderCard(problem)
        const text = wrapper.text()
        const count = (text.match(/—/g) ?? []).length
        expect(count).toBeGreaterThanOrEqual(5)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property: arbitrary combinations of present/absent string members — every
   * field row contains either its value or the empty indicator.
   *
   * Checks the four string fields: type, title, detail, instance.
   */
  it('every string field row contains either its value or the empty indicator (≥100 iterations)', async () => {
    const stringFields = ['type', 'title', 'detail', 'instance'] as const

    await fc.assert(
      fc.asyncProperty(problemDetailsArb, async (problem) => {
        const wrapper = await renderCard(problem)
        const text = wrapper.text()

        for (const field of stringFields) {
          const value = problem[field]
          if (typeof value === 'string' && value.length > 0) {
            // Present: the value must appear in the rendered text
            expect(text).toContain(value)
          } else {
            // Absent: the empty indicator must appear somewhere
            expect(text).toContain(EMPTY_INDICATOR)
          }
        }
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property: extension members (extra keys via .passthrough()) do not break
   * rendering — the card renders correctly and the testid is always present.
   */
  it('extension members (passthrough) do not break rendering (≥100 iterations)', async () => {
    const problemWithExtensionArb = fc
      .tuple(problemDetailsArb, extensionKeyArb, safeStringArb)
      .map(([problem, extKey, extValue]): ProblemDetails => ({
        ...problem,
        [extKey]: extValue,
      }))

    await fc.assert(
      fc.asyncProperty(problemWithExtensionArb, async (problem) => {
        const wrapper = await renderCard(problem)
        expect(wrapper.find('[data-testid="problem-details-card"]').exists()).toBe(true)
        expect(wrapper.text()).toContain('Problem Details')
      }),
      { numRuns: 100 },
    )
  })
})
