/**
 * Contract tests for GET /api/merchants page DTO (PW-M360-T04).
 *
 * Backend and BFF return { content, page, size, totalElements, totalPages }.
 */

import { describe, it, expect } from 'vitest'
import { merchantListBackendSchema, merchantStatusSchema } from '~/composables/useMerchantsApi'

const validMerchant = {
  merchantId: '550e8400-e29b-41d4-a716-446655440001',
  merchantReference: 'ACME-001',
  displayName: 'Acme Corp',
  status: 'DRAFT' as const,
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
}

const emptyPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

describe('merchantListBackendSchema', () => {
  it('accepts an empty page DTO', () => {
    const result = merchantListBackendSchema.safeParse(emptyPage)
    expect(result.success).toBe(true)
  })

  it('accepts deterministic seed merchant ids that are not RFC-4122 version 4', () => {
    const result = merchantListBackendSchema.safeParse({
      ...emptyPage,
      content: [{
        ...validMerchant,
        merchantId: '00000000-0000-0000-0000-0000000000b1',
        merchantReference: 'MERCHANT_ALPHA_001',
      }],
      totalElements: 1,
      totalPages: 1,
    })
    expect(result.success).toBe(true)
  })

  it('accepts a non-empty content array', () => {
    const result = merchantListBackendSchema.safeParse({
      content: [validMerchant],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    expect(result.success).toBe(true)
    if (result.success) {
      expect(result.data.content).toHaveLength(1)
      expect(result.data.content[0]!.merchantReference).toBe('ACME-001')
      expect(result.data.totalElements).toBe(1)
    }
  })

  it('rejects the legacy { merchants: [] } envelope', () => {
    const result = merchantListBackendSchema.safeParse({ merchants: [] })
    expect(result.success).toBe(false)
  })

  it('rejects a merchant item missing required fields', () => {
    const incomplete = {
      content: [{ merchantId: '00000000-0000-0000-0000-000000000001' }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }
    const result = merchantListBackendSchema.safeParse(incomplete)
    expect(result.success).toBe(false)
  })
})

describe('merchantStatusSchema — canonical values', () => {
  it.each(['DRAFT', 'ACTIVE', 'SUSPENDED'] as const)('accepts the canonical status %s', (status) => {
    const result = merchantStatusSchema.safeParse(status)
    expect(result.success).toBe(true)
  })

  it('rejects the legacy PENDING value (no known backend caller still sends it)', () => {
    const result = merchantStatusSchema.safeParse('PENDING')
    expect(result.success).toBe(false)
  })

  it('rejects an arbitrary unsupported status value', () => {
    const result = merchantStatusSchema.safeParse('ARCHIVED')
    expect(result.success).toBe(false)
  })

  it('a page DTO containing a real DRAFT merchant parses successfully', () => {
    const result = merchantListBackendSchema.safeParse({
      content: [validMerchant],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    expect(result.success).toBe(true)
    if (result.success) {
      expect(result.data.content[0]!.status).toBe('DRAFT')
    }
  })

  it('a merchant list response containing a legacy PENDING status is rejected', () => {
    const result = merchantListBackendSchema.safeParse({
      content: [{ ...validMerchant, status: 'PENDING' }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    expect(result.success).toBe(false)
  })
})
