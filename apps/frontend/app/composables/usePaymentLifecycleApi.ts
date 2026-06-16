/**
 * Domain composable for payment order lifecycle mutations and history.
 *
 * Handles: authorize, capture, cancel, refund, metadata PATCH, and history fetch.
 * All lifecycle POST methods require `If-Match` (optimistic locking) and
 * `Idempotency-Key` (de-duplication).
 *
 * Also exports `mapStatusToCategory` — a pure helper that maps HTTP status
 * codes to the `LifecycleErrorCategory` enum used by the Pinia store and UI.
 *
 * Requirements: 4.6
 */

import type { ApiResponse } from '~/types/api'
import {
  paymentOrderResponseSchema,
  paymentStatusHistoryResponseSchema,
  type PaymentOrderResponse,
  type PaymentStatusHistoryResponse,
  type LifecycleErrorCategory,
} from '~/schemas/payment-order.schema'

/**
 * Maps an HTTP status code returned by a lifecycle operation to the
 * `LifecycleErrorCategory` used for UI feedback.
 */
export function mapStatusToCategory(statusCode: number): LifecycleErrorCategory {
  if (statusCode === 412) return 'stale_state'
  if (statusCode === 422) return 'invalid_transition'
  if (statusCode === 409) return 'idempotency_conflict'
  if (statusCode === 403) return 'forbidden'
  if (statusCode === 404) return 'not_found'
  if (statusCode === 428) return 'validation'
  return 'validation'
}

export function usePaymentLifecycleApi() {
  const { request } = useApiClient()

  // ---------------------------------------------------------------------------
  // Helper: build lifecycle endpoint path
  // ---------------------------------------------------------------------------
  function lifecycleUrl(
    merchantId: string,
    paymentOrderId: string,
    action: string
  ): string {
    return `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/${action}`
  }

  // ---------------------------------------------------------------------------
  // Lifecycle POST operations
  // ---------------------------------------------------------------------------

  async function authorizeOrder(
    merchantId: string,
    paymentOrderId: string,
    ifMatch: string,
    idempotencyKey: string
  ): Promise<ApiResponse<PaymentOrderResponse>> {
    return request(lifecycleUrl(merchantId, paymentOrderId, 'authorize'), paymentOrderResponseSchema, {
      method: 'POST',
      headers: { 'If-Match': ifMatch, 'Idempotency-Key': idempotencyKey },
      body: {},
    })
  }

  async function captureOrder(
    merchantId: string,
    paymentOrderId: string,
    ifMatch: string,
    idempotencyKey: string,
    payload?: { amountMinor?: number; reason?: string }
  ): Promise<ApiResponse<PaymentOrderResponse>> {
    return request(lifecycleUrl(merchantId, paymentOrderId, 'capture'), paymentOrderResponseSchema, {
      method: 'POST',
      headers: { 'If-Match': ifMatch, 'Idempotency-Key': idempotencyKey },
      body: payload ?? {},
    })
  }

  async function cancelOrder(
    merchantId: string,
    paymentOrderId: string,
    ifMatch: string,
    idempotencyKey: string,
    payload?: { reason?: string }
  ): Promise<ApiResponse<PaymentOrderResponse>> {
    return request(lifecycleUrl(merchantId, paymentOrderId, 'cancel'), paymentOrderResponseSchema, {
      method: 'POST',
      headers: { 'If-Match': ifMatch, 'Idempotency-Key': idempotencyKey },
      body: payload ?? {},
    })
  }

  async function refundOrder(
    merchantId: string,
    paymentOrderId: string,
    ifMatch: string,
    idempotencyKey: string,
    payload?: { amountMinor?: number; reason?: string }
  ): Promise<ApiResponse<PaymentOrderResponse>> {
    return request(lifecycleUrl(merchantId, paymentOrderId, 'refund'), paymentOrderResponseSchema, {
      method: 'POST',
      headers: { 'If-Match': ifMatch, 'Idempotency-Key': idempotencyKey },
      body: payload ?? {},
    })
  }

  // ---------------------------------------------------------------------------
  // Metadata PATCH
  // ---------------------------------------------------------------------------

  async function patchMetadata(
    merchantId: string,
    paymentOrderId: string,
    ifMatch: string,
    metadata: Record<string, unknown>
  ): Promise<ApiResponse<PaymentOrderResponse>> {
    return request(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`,
      paymentOrderResponseSchema,
      {
        method: 'PATCH',
        headers: { 'If-Match': ifMatch },
        body: { metadata },
      }
    )
  }

  // ---------------------------------------------------------------------------
  // History
  // ---------------------------------------------------------------------------

  async function getHistory(
    merchantId: string,
    paymentOrderId: string
  ): Promise<ApiResponse<PaymentStatusHistoryResponse>> {
    return request(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/history`,
      paymentStatusHistoryResponseSchema
    )
  }

  return {
    authorizeOrder,
    captureOrder,
    cancelOrder,
    refundOrder,
    patchMetadata,
    getHistory,
  }
}

// Re-export history + lifecycle-specific types for convenience
// (PaymentOrderResponse is already exported by usePaymentOrdersApi)
export type { PaymentStatusHistoryResponse, LifecycleErrorCategory }
