/**
 * Unit tests for problem-details.schema.ts extension fields.
 *
 * Feature: error-lab-extensions, Property 1: Problem Details schema parses backend extensions
 */
import { describe, it, expect } from 'vitest'
import { problemDetailsSchema } from '../problem-details.schema'

describe('problemDetailsSchema — extension fields', () => {
  it('accepts correlationId as an optional string', () => {
    const result = problemDetailsSchema.safeParse({ correlationId: 'abc-123' })
    expect(result.success).toBe(true)
    expect(result.data?.correlationId).toBe('abc-123')
  })

  it('accepts requiredHeader as an optional string', () => {
    const result = problemDetailsSchema.safeParse({ requiredHeader: 'If-Match' })
    expect(result.success).toBe(true)
    expect(result.data?.requiredHeader).toBe('If-Match')
  })

  it('accepts retryable as an optional boolean', () => {
    const result = problemDetailsSchema.safeParse({ retryable: true })
    expect(result.success).toBe(true)
    expect(result.data?.retryable).toBe(true)
  })

  it('accepts retryAfterSeconds as an optional integer', () => {
    const result = problemDetailsSchema.safeParse({ retryAfterSeconds: 30 })
    expect(result.success).toBe(true)
    expect(result.data?.retryAfterSeconds).toBe(30)
  })

  it('accepts details array with field+message items', () => {
    const result = problemDetailsSchema.safeParse({
      details: [{ field: 'amount', message: 'must be positive' }],
    })
    expect(result.success).toBe(true)
    expect(result.data?.details).toHaveLength(1)
  })

  it('passes unknown extension keys through (.passthrough)', () => {
    const result = problemDetailsSchema.safeParse({ unknownExt: 'value' })
    expect(result.success).toBe(true)
    expect((result.data as Record<string, unknown>).unknownExt).toBe('value')
  })

  it('rejects retryAfterSeconds when it is not an integer', () => {
    const result = problemDetailsSchema.safeParse({ retryAfterSeconds: 1.5 })
    expect(result.success).toBe(false)
  })

  it('parses a full 429 body with all extension fields', () => {
    const body = {
      type: 'https://api.payment-quality.local/problems/rate-limit-exceeded',
      title: 'Too Many Requests',
      status: 429,
      detail: 'Rate limit exceeded.',
      correlationId: 'corr-xyz',
      error: 'rate_limit_exceeded',
      retryable: true,
      retryAfterSeconds: 30,
    }
    const result = problemDetailsSchema.safeParse(body)
    expect(result.success).toBe(true)
    expect(result.data?.retryable).toBe(true)
    expect(result.data?.retryAfterSeconds).toBe(30)
    expect(result.data?.correlationId).toBe('corr-xyz')
  })
})
