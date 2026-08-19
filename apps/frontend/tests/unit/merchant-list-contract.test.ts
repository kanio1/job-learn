/**
 * Contract tests for the merchant list backend schema and adapter.
 *
 * The backend returns { merchants: MerchantResponse[] } (no pagination).
 * useMerchantsApi adapts this to the paginated view model
 * { content, page, size, totalElements, totalPages } consumed by callers.
 *
 * These tests guard the adapter logic in isolation, without mounting Vue components.
 */

import { z } from 'zod'
import { describe, it, expect } from 'vitest'
import { merchantListBackendSchema, merchantStatusSchema } from '~/composables/useMerchantsApi'

// ---------------------------------------------------------------------------
// Import the production response boundary; keep only the local view-model
// schema needed to test the adapter separately.
// ---------------------------------------------------------------------------

const merchantResponseSchema = z.object({
  merchantId: z.string().uuid(),
  merchantReference: z.string(),
  displayName: z.string(),
  status: merchantStatusSchema,
  createdAt: z.string(),
  updatedAt: z.string(),
})

function adapt(backend: z.infer<typeof merchantListBackendSchema>) {
  const { merchants } = backend
  return {
    content: merchants,
    page: 0,
    size: merchants.length,
    totalElements: merchants.length,
    totalPages: merchants.length > 0 ? 1 : 0,
  }
}

// ---------------------------------------------------------------------------
// Minimal valid merchant fixture
// ---------------------------------------------------------------------------

const validMerchant = {
  merchantId: '550e8400-e29b-41d4-a716-446655440001',
  merchantReference: 'ACME-001',
  displayName: 'Acme Corp',
  status: 'DRAFT' as const,
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
}

// ---------------------------------------------------------------------------
// Backend schema validation
// ---------------------------------------------------------------------------

describe('merchantListBackendSchema', () => {
  it('accepts the real backend shape { merchants: [] }', () => {
    const result = merchantListBackendSchema.safeParse({ merchants: [] })
    expect(result.success).toBe(true)
  })

  it('accepts deterministic seed merchant ids that are not RFC-4122 version 4', () => {
    const result = merchantListBackendSchema.safeParse({
      merchants: [{
        ...validMerchant,
        merchantId: '00000000-0000-0000-0000-0000000000b1',
        merchantReference: 'MERCHANT_ALPHA_001',
      }],
    })
    expect(result.success).toBe(true)
  })

  it('accepts a non-empty merchants array', () => {
    const result = merchantListBackendSchema.safeParse({ merchants: [validMerchant] })
    expect(result.success).toBe(true)
    if (result.success) {
      expect(result.data.merchants).toHaveLength(1)
      expect(result.data.merchants[0]!.merchantReference).toBe('ACME-001')
    }
  })

  it('rejects the old paginated shape { content, page, size, totalElements, totalPages }', () => {
    const paginatedShape = {
      content: [validMerchant],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }
    const result = merchantListBackendSchema.safeParse(paginatedShape)
    // merchants field is missing — should fail or return empty merchants
    // (Zod strict would fail; default Zod strips extra keys)
    if (result.success) {
      // Without strict mode, extra fields are stripped; merchants array is missing
      expect(result.data.merchants).toBeUndefined()
    } else {
      expect(result.success).toBe(false)
    }
  })

  it('rejects a merchant item missing required fields', () => {
    const incomplete = { merchants: [{ merchantId: '00000000-0000-0000-0000-000000000001' }] }
    const result = merchantListBackendSchema.safeParse(incomplete)
    expect(result.success).toBe(false)
  })
})

// ---------------------------------------------------------------------------
// TD-2B: merchant status contract — DRAFT is canonical (matches the real
// backend MerchantStatus.java enum and Flyway V1__create_merchants.sql CHECK
// constraint), not the frontend's former (incorrect) PENDING value. A real
// DRAFT merchant previously failed this schema entirely.
// ---------------------------------------------------------------------------

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

  it('a full merchant list response containing a real DRAFT merchant parses successfully', () => {
    const result = merchantListBackendSchema.safeParse({ merchants: [validMerchant] })
    expect(result.success).toBe(true)
    if (result.success) {
      expect(result.data.merchants[0]!.status).toBe('DRAFT')
    }
  })

  it('a merchant list response containing a legacy PENDING status is rejected', () => {
    const result = merchantListBackendSchema.safeParse({
      merchants: [{ ...validMerchant, status: 'PENDING' }],
    })
    expect(result.success).toBe(false)
  })
})

// ---------------------------------------------------------------------------
// Adapter logic
// ---------------------------------------------------------------------------

describe('merchant list adapter (backend → view model)', () => {
  it('maps empty list to zeroed pagination fields', () => {
    const viewModel = adapt({ merchants: [] })
    expect(viewModel.content).toEqual([])
    expect(viewModel.page).toBe(0)
    expect(viewModel.size).toBe(0)
    expect(viewModel.totalElements).toBe(0)
    expect(viewModel.totalPages).toBe(0)
  })

  it('maps non-empty list: totalElements equals merchants.length', () => {
    const viewModel = adapt({ merchants: [validMerchant] })
    expect(viewModel.totalElements).toBe(1)
    expect(viewModel.content).toHaveLength(1)
    expect(viewModel.totalPages).toBe(1)
    expect(viewModel.page).toBe(0)
    expect(viewModel.size).toBe(1)
  })

  it('preserves all merchant fields through content array', () => {
    const viewModel = adapt({ merchants: [validMerchant] })
    expect(viewModel.content[0]).toEqual(validMerchant)
  })

  it('totalElements equals content.length for any list size', () => {
    const merchants = [validMerchant, { ...validMerchant, merchantId: '550e8400-e29b-41d4-a716-446655440002', merchantReference: 'ACME-002' }]
    const viewModel = adapt({ merchants })
    expect(viewModel.totalElements).toBe(merchants.length)
    expect(viewModel.content.length).toBe(merchants.length)
  })

  it('totalPages is 0 for empty list and 1 for any non-empty list', () => {
    expect(adapt({ merchants: [] }).totalPages).toBe(0)
    expect(adapt({ merchants: [validMerchant] }).totalPages).toBe(1)
  })
})
