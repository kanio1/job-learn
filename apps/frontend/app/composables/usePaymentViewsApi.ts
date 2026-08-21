import { z } from 'zod'
import type { ApiResponse } from '~/types/api'
import {
  createPaymentViewRequestSchema,
  paymentViewListSchema,
  paymentViewSchema,
  type CreatePaymentViewRequest,
  type PaymentViewDto,
} from '~/schemas/payment-view.schema'

export function usePaymentViewsApi() {
  const { request } = useApiClient()

  function listPaymentViews(): Promise<ApiResponse<{ content: PaymentViewDto[] }>> {
    return request('/api/users/me/payment-views', paymentViewListSchema)
  }

  function createPaymentView(body: CreatePaymentViewRequest): Promise<ApiResponse<PaymentViewDto>> {
    return request('/api/users/me/payment-views', paymentViewSchema, {
      method: 'POST',
      body: createPaymentViewRequestSchema.parse(body),
    })
  }

  function updatePaymentView(id: string, body: CreatePaymentViewRequest): Promise<ApiResponse<PaymentViewDto>> {
    return request(`/api/users/me/payment-views/${id}`, paymentViewSchema, {
      method: 'PUT',
      body: createPaymentViewRequestSchema.parse(body),
    })
  }

  function deletePaymentView(id: string): Promise<ApiResponse<unknown>> {
    return request(`/api/users/me/payment-views/${id}`, z.unknown(), {
      method: 'DELETE',
    })
  }

  function setDefaultPaymentView(id: string): Promise<ApiResponse<PaymentViewDto>> {
    return request(`/api/users/me/payment-views/${id}/default`, paymentViewSchema, {
      method: 'POST',
    })
  }

  return {
    listPaymentViews,
    createPaymentView,
    updatePaymentView,
    deletePaymentView,
    setDefaultPaymentView,
  }
}
