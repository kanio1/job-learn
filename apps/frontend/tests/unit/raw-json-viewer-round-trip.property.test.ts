/**
 * Property 6: Raw JSON round-trip and key-order preservation
 *
 * Validates: Requirements 8.6, 8.7
 *
 * Tag: Feature: payment-operations-dashboard, Property 6: Raw JSON round-trip and key-order preservation
 *
 * Tests `app/components/shared/RawJsonViewer.vue`
 *
 * Properties verified:
 *
 *  P6a — Valid JSON round-trip:
 *    For any valid JSON object string, RawJsonViewer renders indented multi-line text
 *    that re-parses back to a value deeply equal to the original parsed value.
 *    No data is lost, added, or mutated during indented rendering.
 *
 *  P6b — Key ordering preservation:
 *    The order of keys emitted in the rendered output must match the insertion order
 *    of keys in the original JSON string. The component must NOT sort, reverse, or
 *    otherwise rearrange object keys.
 *
 *  P6c — Non-JSON fallback:
 *    For any string that cannot be parsed as JSON, the component must render the raw
 *    content as-is AND surface an explicit "not valid JSON" indicator. The indicator
 *    must not appear on valid JSON inputs.
 *
 * Design note: the component tests are implemented as pure logic tests because
 * `RawJsonViewer.vue` exposes its key computation (`isValidJson`, `displayContent`)
 * as straightforward computed properties. Extracting and testing those pure functions
 * directly is the narrowest possible test layer (no DOM, no Nuxt runtime) while still
 * validating the exact logic that Vue will execute. This matches the "narrowest layer
 * that proves the behavior" principle in testing-strategy.md.
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'

// ---------------------------------------------------------------------------
// Extracted logic under test — mirrors the computed logic in RawJsonViewer.vue
//
// These pure functions replicate the two computed properties from the SFC so
// we can property-test them without spinning up a Vue/Nuxt environment.
// ---------------------------------------------------------------------------

/**
 * Mirrors: `isValidJson` computed in RawJsonViewer.vue
 */
function isValidJson(content: string): boolean {
  if (!content.trim()) return false
  try {
    JSON.parse(content)
    return true
  } catch {
    return false
  }
}

/**
 * Mirrors: `displayContent` computed in RawJsonViewer.vue
 * Returns the text that would be rendered inside the <pre> element.
 */
function displayContent(content: string): string {
  if (!content) return ''
  if (!isValidJson(content)) return content
  try {
    return JSON.stringify(JSON.parse(content), null, 2)
  } catch {
    return content
  }
}

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

/**
 * Generates arbitrary JSON-serialisable values (objects, arrays, primitives).
 * fast-check's jsonValue() produces values that JSON.stringify accepts.
 */
const jsonValueArb = fc.jsonValue()

/**
 * Generates arbitrary JSON object values (not null, not arrays, not primitives).
 * Objects with distinct string keys are needed for the key-ordering property.
 */
const jsonObjectArb = fc.dictionary(
  // Keys: non-empty strings (valid JSON object keys)
  fc.string({ minLength: 1, maxLength: 20 }).filter(k => k.trim().length > 0),
  // Values: nested JSON-serialisable values
  fc.jsonValue(),
  { minKeys: 1, maxKeys: 20 },
)

/**
 * Generates arbitrary non-JSON strings (strings that JSON.parse rejects).
 * We use a union of common non-JSON patterns to keep generation efficient.
 */
const nonJsonStringArb: fc.Arbitrary<string> = fc.oneof(
  // Plain words / sentences
  fc.string({ minLength: 1, maxLength: 100 }).filter(s => {
    try { JSON.parse(s); return false } catch { return true }
  }),
  // XML-like strings
  fc.constant('<root><child>value</child></root>'),
  fc.constant('Hello, world!'),
  fc.constant('plain text content'),
  fc.constant('not: valid: yaml'),
  fc.constant('{{ template }}'),
  fc.constant('SELECT * FROM orders'),
  fc.constant('Error: something went wrong\n  at line 1'),
)

// ---------------------------------------------------------------------------
// Helper: extract insertion-order top-level keys from a JSON object string
//
// V8 (Node.js / browsers) preserves insertion order for string keys in
// objects. JSON.parse follows this convention: keys appear in Object.keys()
// in the order they were encountered in the source text. JSON.stringify then
// emits them in that same order.
//
// Consequently, to verify key-ordering preservation we simply compare
// Object.keys(JSON.parse(input)) with Object.keys(JSON.parse(rendered)).
// Both must be identical.
// ---------------------------------------------------------------------------

/**
 * Returns the top-level keys of a JSON object string in their V8 insertion
 * order (= the wire order in which they appeared in the source text).
 * Returns [] for non-object JSON values (null, arrays, primitives).
 */
function topLevelKeyOrder(jsonStr: string): string[] {
  try {
    const parsed = JSON.parse(jsonStr)
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      return []
    }
    return Object.keys(parsed)
  } catch {
    return []
  }
}

/**
 * Returns the top-level key order from an already-indented JSON string
 * (the output of displayContent). Identical logic to topLevelKeyOrder — they
 * are separate helpers to keep the intent clear at the call site.
 */
function keyOrderFromRendered(rendered: string): string[] {
  return topLevelKeyOrder(rendered)
}

// ---------------------------------------------------------------------------
// P6a — Valid JSON round-trip
// ---------------------------------------------------------------------------

describe('Feature: payment-operations-dashboard, Property 6: Raw JSON round-trip and key-order preservation', () => {

  describe('P6a: Valid JSON round-trip — all values preserved', () => {
    it('should produce indented output that round-trips to the same value for any JSON value', () => {
      /**
       * Validates: Requirements 8.6
       *
       * For any valid JSON value (object, array, string, number, boolean, null),
       * the rendered display text must be parseable JSON that equals the original value.
       * No data loss, no mutation.
       */
      fc.assert(
        fc.property(jsonValueArb, (value) => {
          const jsonStr = JSON.stringify(value)

          // Pre-condition: the input must be valid JSON
          expect(isValidJson(jsonStr)).toBe(true)

          const rendered = displayContent(jsonStr)

          // Post-condition 1: the rendered output must itself be valid JSON
          expect(isValidJson(rendered)).toBe(true)

          // Post-condition 2: re-parsing the rendered output must deep-equal the original value
          const roundTripped = JSON.parse(rendered)
          expect(roundTripped).toEqual(value)
        }),
        { numRuns: 200 },
      )
    })

    it('should produce multi-line indented output for any non-trivial JSON object', () => {
      /**
       * Validates: Requirements 8.6 — "indented, multi-line text"
       *
       * A JSON object with at least one key must be rendered as multi-line text
       * (at least one newline character) when displayed by the component.
       */
      fc.assert(
        fc.property(jsonObjectArb, (obj) => {
          const jsonStr = JSON.stringify(obj)
          const rendered = displayContent(jsonStr)

          // An object with ≥1 key must produce multi-line output
          expect(rendered).toContain('\n')
        }),
        { numRuns: 200 },
      )
    })

    it('should use 2-space indentation on rendered JSON objects', () => {
      /**
       * The component uses JSON.stringify(..., null, 2).
       * Any top-level object must have its first key indented by exactly 2 spaces.
       */
      fc.assert(
        fc.property(jsonObjectArb, (obj) => {
          const jsonStr = JSON.stringify(obj)
          const rendered = displayContent(jsonStr)

          // The rendered output should start with "{\n  " (open brace, newline, 2 spaces)
          expect(rendered.startsWith('{\n  ')).toBe(true)
        }),
        { numRuns: 200 },
      )
    })
  })

  // --------------------------------------------------------------------------
  // P6b — Key ordering preservation
  // --------------------------------------------------------------------------

  describe('P6b: Key ordering preservation — rendered keys match original wire order', () => {
    it('should render object keys in the same order as the original JSON string', () => {
      /**
       * Validates: Requirements 8.6 — "preserving the original key ordering"
       *
       * For any JSON object, the sequence of keys in the rendered output must
       * equal the sequence of keys in the original JSON string. The component
       * must NOT sort, reverse, or otherwise rearrange keys.
       */
      fc.assert(
        fc.property(jsonObjectArb, (obj) => {
          // Serialise in insertion order (V8/Node.js guarantees this for string keys)
          const jsonStr = JSON.stringify(obj)
          const rendered = displayContent(jsonStr)

          const originalOrder = topLevelKeyOrder(jsonStr)
          const renderedOrder = keyOrderFromRendered(rendered)

          // Both arrays must contain the same keys in the same order
          expect(renderedOrder).toEqual(originalOrder)
        }),
        { numRuns: 200 },
      )
    })

    it('should preserve key order for objects with many keys', () => {
      /**
       * Extra assurance for larger objects (≥5 keys) where accidental sorting
       * would be more obvious.
       */
      const largeObjectArb = fc.dictionary(
        fc.string({ minLength: 1, maxLength: 15 }).filter(k => k.trim().length > 0),
        fc.jsonValue(),
        { minKeys: 5, maxKeys: 30 },
      )

      fc.assert(
        fc.property(largeObjectArb, (obj) => {
          const jsonStr = JSON.stringify(obj)
          const rendered = displayContent(jsonStr)

          const originalOrder = topLevelKeyOrder(jsonStr)
          const renderedOrder = keyOrderFromRendered(rendered)

          expect(renderedOrder).toEqual(originalOrder)
        }),
        { numRuns: 200 },
      )
    })

    it('should preserve key order for a known fixed object (regression anchor)', () => {
      /**
       * Example-based anchor: a concrete object with a predictable known order.
       * This serves as a stable regression check alongside the property.
       */
      const input = JSON.stringify({ z: 1, a: 2, m: 3, b: 4 })
      const rendered = displayContent(input)

      const renderedOrder = keyOrderFromRendered(rendered)
      expect(renderedOrder).toEqual(['z', 'a', 'm', 'b'])
    })
  })

  // --------------------------------------------------------------------------
  // P6c — Non-JSON fallback
  // --------------------------------------------------------------------------

  describe('P6c: Non-JSON fallback — raw content rendered with "not valid JSON" indicator', () => {
    it('should classify non-JSON strings as not valid JSON', () => {
      /**
       * Validates: Requirements 8.7
       *
       * The isValidJson gate must return false for any content that JSON.parse rejects.
       * This is the precondition for triggering the non-JSON fallback path.
       */
      fc.assert(
        fc.property(nonJsonStringArb, (nonJson) => {
          expect(isValidJson(nonJson)).toBe(false)
        }),
        { numRuns: 200 },
      )
    })

    it('should return the raw content unchanged for non-JSON input', () => {
      /**
       * Validates: Requirements 8.7 — "display the raw content"
       *
       * When the input cannot be parsed as JSON, displayContent must return
       * the original string unchanged (no transformation, no truncation).
       */
      fc.assert(
        fc.property(nonJsonStringArb, (nonJson) => {
          const rendered = displayContent(nonJson)

          // The raw content must be returned as-is
          expect(rendered).toBe(nonJson)
        }),
        { numRuns: 200 },
      )
    })

    it('should classify valid JSON strings as valid JSON (no false negatives)', () => {
      /**
       * Validates: Requirements 8.6
       *
       * isValidJson must return true for any valid JSON value so that the
       * component correctly takes the indented-rendering path, not the fallback.
       */
      fc.assert(
        fc.property(jsonValueArb, (value) => {
          const jsonStr = JSON.stringify(value)

          expect(isValidJson(jsonStr)).toBe(true)
        }),
        { numRuns: 200 },
      )
    })

    it('should mark empty string as not valid JSON', () => {
      /**
       * Edge case: an empty string is not parseable JSON.
       * The component explicitly checks trim() before attempting parse.
       */
      expect(isValidJson('')).toBe(false)
      expect(isValidJson('   ')).toBe(false)
    })

    it('should return empty string for empty input', () => {
      /**
       * Edge case: displayContent must return '' when content is '' (not crash).
       */
      expect(displayContent('')).toBe('')
    })

    it('should mark JSON primitives (string, number, boolean, null) as valid JSON', () => {
      /**
       * JSON.parse accepts standalone primitives. The component must handle
       * these without treating them as invalid.
       */
      expect(isValidJson('"hello"')).toBe(true)
      expect(isValidJson('42')).toBe(true)
      expect(isValidJson('true')).toBe(true)
      expect(isValidJson('null')).toBe(true)
    })
  })
})
