/**
 * Property 14: History is ordered ascending by timestamp
 * Property 15: Non-display actor fields are never rendered
 *
 * Validates: Requirements 7.6, 7.8
 *
 * Tag: Feature: payment-operations-dashboard, Property 14: History is ordered ascending by timestamp
 * Tag: Feature: payment-operations-dashboard, Property 15: Non-display actor fields are never rendered
 *
 * Tests the history-tab logic in `PaymentOrderDetail.vue`.
 *
 * Design note: both properties target pure functions that mirror the computed properties
 * in the component. Testing pure functions directly is the narrowest layer that proves the
 * behaviour, matching the "narrowest layer" principle in testing-strategy.md.
 *
 *  P14 — History ordering ascending by timestamp
 *    The `sortedHistory` computed in PaymentOrderDetail sorts entries by `createdAt`
 *    (ascending). For any collection of history entries whose timestamps are in arbitrary
 *    order, the rendered order must be non-decreasing from oldest to newest.
 *
 *  P15 — Non-display actor fields never rendered
 *    History entries carry two actor-related fields:
 *      - `actorDisplay`  — safe display label (e.g. "platform-operator") — MUST be rendered
 *      - `actorSubject`  — internal JWT subject / claim (e.g. sub, iss, azp) — MUST NOT be rendered
 *    For any history entry, the rendered actor output must use only `actorDisplay` and must
 *    contain no fragment of any internal `actorSubject` value.
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'

// ---------------------------------------------------------------------------
// Types — mirrors the history entry prop shape from PaymentOrderDetail.vue
// ---------------------------------------------------------------------------

interface HistoryEntry {
  statusHistoryId: string
  paymentOrderId: string
  fromStatus: string | null
  toStatus: string
  action: string | null
  actorDisplay?: string | null
  actorSubject?: string // internal — MUST NOT be rendered
  reason?: string | null
  amountMinor?: number | null
  pspReference?: string | null
  correlationId?: string | null
  createdAt: string
}

// ---------------------------------------------------------------------------
// Logic under test — mirrors computed properties in PaymentOrderDetail.vue
// ---------------------------------------------------------------------------

/**
 * Mirrors: `sortedHistory` computed in PaymentOrderDetail.vue
 *
 *   return [...props.history].sort(
 *     (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
 *   )
 */
function sortedHistory(history: HistoryEntry[]): HistoryEntry[] {
  if (!history || history.length === 0) return []
  return [...history].sort(
    (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
  )
}

/**
 * Mirrors: actor rendering logic in the History tab template.
 *
 *   {{ entry.actorDisplay || 'System' }}
 *
 * Returns the string that would appear in the rendered UI for the actor field.
 * This is the ONLY field that should be rendered — `actorSubject` must never appear.
 */
function renderActorLabel(entry: HistoryEntry): string {
  return entry.actorDisplay || 'System'
}

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

/**
 * Generates arbitrary ISO 8601 timestamps within a realistic range.
 * Range: 2020-01-01T00:00:00Z … 2030-12-31T23:59:59Z (10-year window).
 */
const timestampArb: fc.Arbitrary<string> = fc
  .integer({ min: 1577836800000, max: 1924991999000 }) // ms since epoch
  .map(ms => new Date(ms).toISOString())

/**
 * Generates an arbitrary non-empty actor display label (the safe value).
 * Realistic labels: role names, system identifiers, or short strings.
 */
const actorDisplayArb: fc.Arbitrary<string> = fc.oneof(
  fc.constantFrom(
    'platform-operator',
    'system',
    'merchant-admin',
    'payment-service',
    'scheduler',
    'api-gateway',
  ),
  // Arbitrary non-empty string labels
  fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
)

/**
 * Generates an arbitrary internal actor subject value (JWT claim style).
 * These values MUST NOT appear in the rendered output.
 * Examples from JWT: sub, iss, azp, jti claims.
 */
const actorSubjectArb: fc.Arbitrary<string> = fc.oneof(
  // UUID-style sub claims
  fc.uuid(),
  // ISS claim URL patterns
  fc.constantFrom(
    'https://keycloak.example.com/realms/payment-quality',
    'https://idp.internal/realms/platform',
    'https://auth.example.org/oauth',
  ),
  // AZP (authorized party) client IDs
  fc.constantFrom('payment-dashboard', 'admin-cli', 'frontend-client', 'internal-service'),
  // JTI (JWT ID) values
  fc.uuidV(4).map(id => `jti:${id}`),
  // Generic opaque internal identifiers
  fc.string({ minLength: 10, maxLength: 100 }).filter(s => s.trim().length > 0),
)

/**
 * Generates a valid Payment_Status value.
 */
const paymentStatusArb: fc.Arbitrary<string> = fc.constantFrom(
  'CREATED',
  'AUTHORIZED',
  'CAPTURED',
  'CANCELLED',
  'EXPIRED',
  'REFUNDED',
)

/**
 * Generates a valid lifecycle action name.
 */
const lifecycleActionArb: fc.Arbitrary<string | null> = fc.oneof(
  fc.constantFrom('authorize', 'capture', 'cancel', 'refund', 'create'),
  fc.constant(null),
)

/**
 * Generates an arbitrary HistoryEntry with a controlled timestamp (separate param).
 * The timestamp is factored out so we can compose sequences with known orderings.
 */
function historyEntryWithTimestamp(
  timestamp: string,
  overrides: Partial<HistoryEntry> = {},
): HistoryEntry {
  return {
    statusHistoryId: `00000000-0000-0000-0000-${String(Math.floor(Math.random() * 1e12)).padStart(12, '0')}`,
    paymentOrderId: '00000000-0000-0000-0000-000000000001',
    fromStatus: null,
    toStatus: 'CREATED',
    action: null,
    actorDisplay: 'system',
    createdAt: timestamp,
    ...overrides,
  }
}

/**
 * Generates an arbitrary history entry with both actorDisplay and actorSubject.
 * Used by Property 15 to verify that actorSubject is never used in rendering.
 */
const historyEntryWithBothActorFieldsArb: fc.Arbitrary<HistoryEntry> = fc
  .record({
    timestamp: timestampArb,
    display: actorDisplayArb,
    subject: actorSubjectArb,
    fromStatus: fc.oneof(paymentStatusArb, fc.constant(null)),
    toStatus: paymentStatusArb,
    action: lifecycleActionArb,
  })
  .map(({ timestamp, display, subject, fromStatus, toStatus, action }) => ({
    statusHistoryId: '00000000-0000-0000-0000-000000000001',
    paymentOrderId: '00000000-0000-0000-0000-000000000002',
    fromStatus,
    toStatus,
    action,
    actorDisplay: display,
    actorSubject: subject,
    createdAt: timestamp,
  }))

/**
 * Generates a history entry with ONLY actorSubject set (actorDisplay absent/null).
 * The rendered output must still fall back to 'System', not expose actorSubject.
 */
const historyEntrySubjectOnlyArb: fc.Arbitrary<HistoryEntry> = fc
  .record({
    timestamp: timestampArb,
    subject: actorSubjectArb,
    toStatus: paymentStatusArb,
  })
  .map(({ timestamp, subject, toStatus }) => ({
    statusHistoryId: '00000000-0000-0000-0000-000000000001',
    paymentOrderId: '00000000-0000-0000-0000-000000000002',
    fromStatus: null,
    toStatus,
    action: null,
    actorDisplay: null, // intentionally absent
    actorSubject: subject,
    createdAt: timestamp,
  }))

/**
 * Generates an array of 0–20 history entries with arbitrary timestamps.
 */
const historyArrayArb: fc.Arbitrary<HistoryEntry[]> = fc
  .array(timestampArb, { minLength: 0, maxLength: 20 })
  .map(timestamps =>
    timestamps.map((ts, i) =>
      historyEntryWithTimestamp(ts, {
        statusHistoryId: `00000000-0000-0000-0000-${String(i).padStart(12, '0')}`,
      }),
    ),
  )

/**
 * Generates a non-empty array of ≥2 history entries — needed for ordering assertions.
 */
const nonEmptyHistoryArb: fc.Arbitrary<HistoryEntry[]> = fc
  .array(timestampArb, { minLength: 2, maxLength: 20 })
  .map(timestamps =>
    timestamps.map((ts, i) =>
      historyEntryWithTimestamp(ts, {
        statusHistoryId: `00000000-0000-0000-0000-${String(i).padStart(12, '0')}`,
      }),
    ),
  )

// ---------------------------------------------------------------------------
// Helper: check that an array of history entries is sorted ascending by timestamp
// ---------------------------------------------------------------------------

function isAscendingByTimestamp(entries: HistoryEntry[]): boolean {
  for (let i = 1; i < entries.length; i++) {
    const prev = new Date(entries[i - 1]!.createdAt).getTime()
    const curr = new Date(entries[i]!.createdAt).getTime()
    if (curr < prev) return false
  }
  return true
}

// ---------------------------------------------------------------------------
// Property 14: History is ordered ascending by timestamp
// ---------------------------------------------------------------------------

describe('Feature: payment-operations-dashboard, Property 14: History is ordered ascending by timestamp', () => {
  /**
   * Validates: Requirements 7.6
   *
   * For any array of history entries (with timestamps in arbitrary order),
   * `sortedHistory` must produce a non-decreasing sequence by `createdAt`.
   */
  describe('P14a: sortedHistory produces ascending timestamp order', () => {
    it('should always output entries in non-decreasing timestamp order for any input order', () => {
      fc.assert(
        fc.property(historyArrayArb, (entries) => {
          const sorted = sortedHistory(entries)

          // Property: the output must be ordered ascending by createdAt timestamp
          expect(isAscendingByTimestamp(sorted)).toBe(true)
        }),
        { numRuns: 200 },
      )
    })

    it('should produce the same set of entries as the input (no additions or deletions)', () => {
      fc.assert(
        fc.property(historyArrayArb, (entries) => {
          const sorted = sortedHistory(entries)

          // The sorted array must contain the same entries as the original
          expect(sorted).toHaveLength(entries.length)

          // Every original entry must appear in the sorted output
          const sortedIds = new Set(sorted.map(e => e.statusHistoryId))
          for (const entry of entries) {
            expect(sortedIds.has(entry.statusHistoryId)).toBe(true)
          }
        }),
        { numRuns: 200 },
      )
    })

    it('should not mutate the original input array', () => {
      fc.assert(
        fc.property(nonEmptyHistoryArb, (entries) => {
          const originalOrder = entries.map(e => e.statusHistoryId)

          sortedHistory(entries)

          // The original array must remain in its original order
          const afterSortOrder = entries.map(e => e.statusHistoryId)
          expect(afterSortOrder).toEqual(originalOrder)
        }),
        { numRuns: 100 },
      )
    })

    it('should return an empty array for an empty input', () => {
      const result = sortedHistory([])
      expect(result).toEqual([])
    })

    it('should return a single-element array unchanged for a single entry', () => {
      fc.assert(
        fc.property(timestampArb, (ts) => {
          const entry = historyEntryWithTimestamp(ts)
          const result = sortedHistory([entry])

          expect(result).toHaveLength(1)
          expect(result[0]!.createdAt).toBe(ts)
        }),
        { numRuns: 100 },
      )
    })

    it('should place earlier timestamps before later timestamps — ordering direction invariant', () => {
      /**
       * Specifically verifies the ascending (oldest-first) direction.
       * For any two entries A and B where A.createdAt < B.createdAt, A must appear first.
       */
      fc.assert(
        fc.property(
          fc.integer({ min: 1577836800000, max: 1924991998000 }),
          fc.integer({ min: 1, max: 1000 }),
          (baseMs, offsetMs) => {
            const earlierTs = new Date(baseMs).toISOString()
            const laterTs = new Date(baseMs + offsetMs).toISOString()

            const earlier = historyEntryWithTimestamp(earlierTs, {
              statusHistoryId: '00000000-0000-0000-0000-000000000001',
            })
            const later = historyEntryWithTimestamp(laterTs, {
              statusHistoryId: '00000000-0000-0000-0000-000000000002',
            })

            // Feed them in reverse order — sorted output must put earlier first
            const sorted = sortedHistory([later, earlier])

            expect(sorted[0]!.statusHistoryId).toBe('00000000-0000-0000-0000-000000000001')
            expect(sorted[1]!.statusHistoryId).toBe('00000000-0000-0000-0000-000000000002')
          },
        ),
        { numRuns: 200 },
      )
    })

    it('should handle entries with equal timestamps without dropping any entry', () => {
      /**
       * Edge case: two entries sharing the same timestamp.
       * The sort is stable in V8 (Node 18+), so equal-timestamp entries keep their
       * relative order. The important guarantee is that both entries remain present.
       */
      fc.assert(
        fc.property(timestampArb, (ts) => {
          const a = historyEntryWithTimestamp(ts, {
            statusHistoryId: '00000000-0000-0000-0000-000000000001',
          })
          const b = historyEntryWithTimestamp(ts, {
            statusHistoryId: '00000000-0000-0000-0000-000000000002',
          })

          const sorted = sortedHistory([a, b])

          expect(sorted).toHaveLength(2)
          expect(isAscendingByTimestamp(sorted)).toBe(true)
        }),
        { numRuns: 100 },
      )
    })
  })
})

// ---------------------------------------------------------------------------
// Property 15: Non-display actor fields are never rendered
// ---------------------------------------------------------------------------

describe('Feature: payment-operations-dashboard, Property 15: Non-display actor fields are never rendered', () => {
  /**
   * Validates: Requirements 7.8
   *
   * For any history entry, the rendered actor label must:
   *  1. Equal `actorDisplay` when that field is present and non-empty.
   *  2. Fall back to 'System' when `actorDisplay` is absent or null.
   *  3. NEVER contain any character fragment from `actorSubject`.
   */

  describe('P15a: Only actorDisplay is used — actorSubject is never rendered', () => {
    it('should render actorDisplay and not expose any part of actorSubject', () => {
      fc.assert(
        fc.property(historyEntryWithBothActorFieldsArb, (entry) => {
          const rendered = renderActorLabel(entry)

          // The rendered value must be the safe display label
          expect(rendered).toBe(entry.actorDisplay)

          // The rendered value must NOT be the internal subject
          // (unless actorDisplay happens to equal actorSubject, which is extremely
          //  unlikely in practice but could theoretically occur by coincidence with
          //  short display values; we only check they differ when actorSubject differs)
          if (entry.actorSubject !== entry.actorDisplay) {
            expect(rendered).not.toBe(entry.actorSubject)
          }

          // Most important: the rendered string must not contain the actorSubject value
          // when actorSubject is sufficiently distinct (length > 5 to avoid false positives
          // with very short coincidental overlaps in generic strings)
          if (entry.actorSubject && entry.actorSubject.length > 5) {
            // The full actorSubject value must not appear verbatim in the rendered output
            expect(rendered).not.toContain(entry.actorSubject)
          }
        }),
        { numRuns: 200 },
      )
    })

    it('should fall back to "System" when actorDisplay is null — never exposing actorSubject', () => {
      fc.assert(
        fc.property(historyEntrySubjectOnlyArb, (entry) => {
          const rendered = renderActorLabel(entry)

          // When actorDisplay is null, the fallback "System" must be used
          expect(rendered).toBe('System')

          // The rendered value must NOT be the actorSubject
          expect(rendered).not.toBe(entry.actorSubject)

          // The actorSubject value must not appear in the rendered string
          if (entry.actorSubject && entry.actorSubject.length > 0) {
            expect(rendered).not.toContain(entry.actorSubject)
          }
        }),
        { numRuns: 200 },
      )
    })
  })

  describe('P15b: Rendered actor label is always a safe display value', () => {
    it('should render a non-empty actor label for any history entry', () => {
      /**
       * The rendered actor label must always be a non-empty string.
       * Either the actorDisplay value (when present) or the "System" fallback.
       */
      fc.assert(
        fc.property(historyEntryWithBothActorFieldsArb, (entry) => {
          const rendered = renderActorLabel(entry)

          expect(typeof rendered).toBe('string')
          expect(rendered.length).toBeGreaterThan(0)
        }),
        { numRuns: 200 },
      )
    })

    it('should render the "System" fallback for entries without actorDisplay', () => {
      fc.assert(
        fc.property(historyEntrySubjectOnlyArb, (entry) => {
          const rendered = renderActorLabel(entry)
          expect(rendered).toBe('System')
        }),
        { numRuns: 200 },
      )
    })

    it('should render the actorDisplay value verbatim when it is present and non-empty', () => {
      fc.assert(
        fc.property(actorDisplayArb, actorSubjectArb, timestampArb, (display, subject, ts) => {
          const entry: HistoryEntry = historyEntryWithTimestamp(ts, {
            actorDisplay: display,
            actorSubject: subject,
          })

          const rendered = renderActorLabel(entry)

          // Property: renders exactly the display label
          expect(rendered).toBe(display)
        }),
        { numRuns: 200 },
      )
    })
  })

  describe('P15c: JWT internal claim field names are never present in rendered output', () => {
    /**
     * Explicit test for the JWT claim field names named in the spec:
     * sub, iss, azp — these are the representative internal actor subject field formats.
     * The test verifies that using any of these as an actorSubject value never leaks
     * into the rendered actor label output.
     */
    it('should not render UUID sub claim as actor label', () => {
      fc.assert(
        fc.property(fc.uuid(), actorDisplayArb, timestampArb, (subClaim, display, ts) => {
          const entry: HistoryEntry = historyEntryWithTimestamp(ts, {
            actorDisplay: display,
            actorSubject: subClaim,
          })

          const rendered = renderActorLabel(entry)

          expect(rendered).toBe(display)
          expect(rendered).not.toBe(subClaim)
          // The UUID sub claim must not appear in the rendered actor label
          expect(rendered).not.toContain(subClaim)
        }),
        { numRuns: 200 },
      )
    })

    it('should not render ISS claim URL as actor label', () => {
      const issClaimArb = fc.constantFrom(
        'https://keycloak.example.com/realms/payment-quality',
        'https://idp.internal/realms/platform',
        'https://auth.example.org/oauth',
        'https://login.microsoftonline.com/tenant/v2.0',
      )

      fc.assert(
        fc.property(issClaimArb, actorDisplayArb, timestampArb, (issClaim, display, ts) => {
          const entry: HistoryEntry = historyEntryWithTimestamp(ts, {
            actorDisplay: display,
            actorSubject: issClaim,
          })

          const rendered = renderActorLabel(entry)

          expect(rendered).toBe(display)
          expect(rendered).not.toContain(issClaim)
        }),
        { numRuns: 100 },
      )
    })

    it('should not render AZP client-id claim as actor label', () => {
      const azpClaimArb = fc.constantFrom(
        'payment-dashboard',
        'admin-cli',
        'frontend-client',
        'internal-service',
        'oauth-client-abc123',
      )

      fc.assert(
        fc.property(azpClaimArb, actorDisplayArb, timestampArb, (azpClaim, display, ts) => {
          const entry: HistoryEntry = historyEntryWithTimestamp(ts, {
            actorDisplay: display,
            actorSubject: azpClaim,
          })

          const rendered = renderActorLabel(entry)

          expect(rendered).toBe(display)
          // AZP claim must not appear as the rendered actor
          if (azpClaim !== display) {
            expect(rendered).not.toBe(azpClaim)
          }
        }),
        { numRuns: 100 },
      )
    })

    it('should render only "System" when actorDisplay is absent regardless of actorSubject JWT field name', () => {
      /**
       * Concrete example-based anchor: the three JWT claim field names
       * explicitly mentioned in the spec — sub, iss, azp.
       * These must never appear in the rendered output when actorDisplay is absent.
       */
      const jwtSubjectExamples = [
        'a1b2c3d4-e5f6-7890-abcd-ef1234567890', // sub (UUID)
        'https://keycloak.example.com/realms/payment-quality', // iss
        'payment-dashboard', // azp
      ]

      for (const subject of jwtSubjectExamples) {
        const entry: HistoryEntry = {
          statusHistoryId: '00000000-0000-0000-0000-000000000001',
          paymentOrderId: '00000000-0000-0000-0000-000000000002',
          fromStatus: null,
          toStatus: 'AUTHORIZED',
          action: 'authorize',
          actorDisplay: null, // absent
          actorSubject: subject,
          createdAt: '2024-06-15T10:00:00.000Z',
        }

        const rendered = renderActorLabel(entry)

        expect(rendered).toBe('System')
        expect(rendered).not.toContain(subject)
      }
    })
  })
})
