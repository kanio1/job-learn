/**
 * Domain composable for merchant operations.
 *
 * All HTTP transport delegates to `useApiClient`. Inline Zod schemas define
 * the expected response shapes for merchant endpoints (no separate schema
 * file needed — these are response-only, not form schemas).
 *
 * Requirements: 4.4
 */

import { z } from 'zod'
import type { ApiResponse } from '~/types/api'
import { createMerchantSchema, type CreateMerchantForm } from '~/schemas/merchant.schema'

// ---------------------------------------------------------------------------
// Inline response schemas (merchant API responses only)
// ---------------------------------------------------------------------------

const merchantStatusSchema = z.enum(['PENDING', 'ACTIVE', 'SUSPENDED'])

const merchantResponseSchema = z.object({
  merchantId: z.string().uuid(),
  merchantReference: z.string(),
  displayName: z.string(),
  status: merchantStatusSchema,
  createdAt: z.string(),
  updatedAt: z.string(),
})

// Backend returns { merchants: [...] } — no pagination metadata.
const merchantListBackendSchema = z.object({
  merchants: z.array(merchantResponseSchema),
})

// View-model exposed to callers: paginated shape derived from the flat list.
// Keeps callers (index.vue, merchants page, PaymentOrderSummaryCards) stable
// without requiring backend pagination support.
const merchantListResponseSchema = z.object({
  content: z.array(merchantResponseSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().nonnegative(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
})

// ---------------------------------------------------------------------------
// Exported types inferred from schemas
// ---------------------------------------------------------------------------

export type MerchantResponse = z.infer<typeof merchantResponseSchema>
export type MerchantListResponse = z.infer<typeof merchantListResponseSchema>

// Re-export so callers don't need to import from the schema file
export type { CreateMerchantForm }

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

export function useMerchantsApi() {
  const { request } = useApiClient()

  async function listMerchants(): Promise<ApiResponse<MerchantListResponse>> {
    const raw = await request('/api/merchants', merchantListBackendSchema)
    if (!raw.data) return { ...raw, data: null }
    const { merchants } = raw.data
    return {
      ...raw,
      data: {
        content: merchants,
        page: 0,
        size: merchants.length,
        totalElements: merchants.length,
        totalPages: merchants.length > 0 ? 1 : 0,
      },
    }
  }

  async function getMerchant(merchantId: string): Promise<ApiResponse<MerchantResponse>> {
    return request(`/api/merchants/${merchantId}`, merchantResponseSchema)
  }

  async function createMerchant(
    payload: CreateMerchantForm
  ): Promise<ApiResponse<MerchantResponse>> {
    return request('/api/merchants', merchantResponseSchema, {
      method: 'POST',
      body: payload,
    })
  }

  async function activateMerchant(merchantId: string): Promise<ApiResponse<MerchantResponse>> {
    return request(`/api/merchants/${merchantId}/activate`, merchantResponseSchema, {
      method: 'POST',
    })
  }

  async function suspendMerchant(merchantId: string): Promise<ApiResponse<MerchantResponse>> {
    return request(`/api/merchants/${merchantId}/suspend`, merchantResponseSchema, {
      method: 'POST',
    })
  }

  return { listMerchants, getMerchant, createMerchant, activateMerchant, suspendMerchant }
}
