/**
 * Domain composable for payment order read/list/summary/create operations.
 *
 * All HTTP transport delegates to `useApiClient`. The caller can read
 * `response.headers.etag` and `response.headers.location` directly from
 * the returned `ApiResponse`.
 *
 * Requirements: 4.5
 */

import type { ApiResponse } from '~/types/api'
import {
  paymentOrderResponseSchema,
  paymentOrderListResponseSchema,
  paymentOrderSummaryResponseSchema,
  type PaymentOrderResponse,
  type PaymentOrderListResponse,
  type PaymentOrderSummaryResponse,
  type CreatePaymentOrderForm,
  type PaymentOrderListQuery,
} from '~/schemas/payment-order.schema'

export function usePaymentOrdersApi() {
  const { request } = useApiClient()

  async function listOrders(
    merchantId: string,
    query?: PaymentOrderListQuery
  ): Promise<ApiResponse<PaymentOrderListResponse>> {
    return request(
      `/api/merchants/${merchantId}/payment-orders`,
      paymentOrderListResponseSchema,
      { query }
    )
  }

  async function getOrderSummary(
    merchantId: string
  ): Promise<ApiResponse<PaymentOrderSummaryResponse>> {
    return request(
      `/api/merchants/${merchantId}/payment-orders/summary`,
      paymentOrderSummaryResponseSchema
    )
  }

  async function getOrder(
    merchantId: string,
    paymentOrderId: string
  ): Promise<ApiResponse<PaymentOrderResponse>> {
    return request(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`,
      paymentOrderResponseSchema
    )
  }

  async function createOrder(
    merchantId: string,
    payload: CreatePaymentOrderForm,
    idempotencyKey: string
  ): Promise<ApiResponse<PaymentOrderResponse>> {
    return request(
      `/api/merchants/${merchantId}/payment-orders`,
      paymentOrderResponseSchema,
      {
        method: 'POST',
        body: payload,
        headers: { 'Idempotency-Key': idempotencyKey },
      }
    )
  }

  return { listOrders, getOrderSummary, getOrder, createOrder }
}

// Re-export types for convenience
export type {
  PaymentOrderResponse,
  PaymentOrderListResponse,
  PaymentOrderSummaryResponse,
  CreatePaymentOrderForm,
  PaymentOrderListQuery,
}
