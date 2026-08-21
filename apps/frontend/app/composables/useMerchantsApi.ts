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
import { type CreateMerchantForm } from '~/schemas/merchant.schema'
import {
  merchantImportCommitSchema,
  merchantImportPreviewSchema,
  type MerchantImportCommit,
  type MerchantImportPreview,
} from '~/schemas/merchant-import.schema'

// ---------------------------------------------------------------------------
// Inline response schemas (merchant API responses only)
// ---------------------------------------------------------------------------

export const merchantStatusSchema = z.enum(['DRAFT', 'ACTIVE', 'SUSPENDED'])

// Seed merchants use Java UUID strings that fail Zod 4 RFC-4122 uuid() (nil version).
const backendMerchantIdSchema = z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  'Expected UUID',
)

const merchantResponseSchema = z.object({
  merchantId: backendMerchantIdSchema,
  merchantReference: z.string(),
  displayName: z.string(),
  status: merchantStatusSchema,
  createdAt: z.string(),
  updatedAt: z.string(),
  riskFlagged: z.boolean().default(false),
  version: z.number().int().nonnegative().optional().default(0),
  contactPhone: z.string().nullable().optional(),
  contactAddress: z.string().nullable().optional(),
})

// Backend page DTO: { content, page, size, totalElements, totalPages }.
export const merchantListResponseSchema = z.object({
  content: z.array(merchantResponseSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().nonnegative(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
})

export const merchantListBackendSchema = merchantListResponseSchema

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

  async function listMerchants(
    query?: Record<string, string | number | boolean | null | undefined>,
  ): Promise<ApiResponse<MerchantListResponse>> {
    const params: Record<string, string | number | boolean> = {}
    if (query) {
      for (const [key, value] of Object.entries(query)) {
        if (value !== undefined && value !== null && value !== '') {
          params[key] = value
        }
      }
    }
    return request('/api/merchants', merchantListResponseSchema, {
      query: Object.keys(params).length > 0 ? params : undefined,
    })
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

  async function activateMerchant(
    merchantId: string,
    ifMatch: string,
  ): Promise<ApiResponse<MerchantResponse>> {
    return request(`/api/merchants/${merchantId}/activate`, merchantResponseSchema, {
      method: 'POST',
      headers: { 'If-Match': ifMatch },
    })
  }

  async function suspendMerchant(
    merchantId: string,
    ifMatch: string,
  ): Promise<ApiResponse<MerchantResponse>> {
    return request(`/api/merchants/${merchantId}/suspend`, merchantResponseSchema, {
      method: 'POST',
      headers: { 'If-Match': ifMatch },
    })
  }

  async function updateMerchantRiskFlag(
    merchantId: string,
    riskFlagged: boolean,
    ifMatch: string,
  ): Promise<ApiResponse<MerchantResponse>> {
    return request(`/api/merchants/${merchantId}/risk-flag`, merchantResponseSchema, {
      method: 'PATCH',
      body: { riskFlagged },
      headers: { 'If-Match': ifMatch },
    })
  }

  async function patchMerchantDisplayName(
    merchantId: string,
    displayName: string,
    ifMatch: string,
  ): Promise<ApiResponse<MerchantResponse>> {
    return patchMerchant(merchantId, { displayName }, ifMatch)
  }

  async function patchMerchant(
    merchantId: string,
    body: {
      displayName?: string
      contactPhone?: string | null
      contactAddress?: string | null
    },
    ifMatch: string,
  ): Promise<ApiResponse<MerchantResponse>> {
    return request(`/api/merchants/${merchantId}`, merchantResponseSchema, {
      method: 'PATCH',
      body,
      headers: { 'If-Match': ifMatch },
    })
  }

  async function previewMerchantImport(file: File): Promise<ApiResponse<MerchantImportPreview>> {
    const body = new FormData()
    body.append('file', file)
    return request('/api/merchants/import/preview', merchantImportPreviewSchema, {
      method: 'POST',
      body,
    })
  }

  async function commitMerchantImport(previewId: string): Promise<ApiResponse<MerchantImportCommit>> {
    return request('/api/merchants/import/commit', merchantImportCommitSchema, {
      method: 'POST',
      body: { previewId },
    })
  }

  return {
    listMerchants,
    getMerchant,
    createMerchant,
    activateMerchant,
    suspendMerchant,
    updateMerchantRiskFlag,
    patchMerchantDisplayName,
    patchMerchant,
    previewMerchantImport,
    commitMerchantImport,
  }
}
