import type { ApiResponse } from '~/types/api'
import {
  paymentEvidenceListResponseSchema,
  paymentEvidenceSchema,
  type PaymentEvidence,
  type PaymentEvidenceListResponse,
} from '~/schemas/payment-order.schema'

export function usePaymentEvidenceApi() {
  const { request } = useApiClient()

  async function listEvidence(
    merchantId: string,
    paymentOrderId: string,
  ): Promise<ApiResponse<PaymentEvidenceListResponse>> {
    return request(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence`,
      paymentEvidenceListResponseSchema,
    )
  }

  async function uploadEvidence(
    merchantId: string,
    paymentOrderId: string,
    file: File,
  ): Promise<ApiResponse<PaymentEvidence>> {
    const body = new FormData()
    body.append('file', file)

    return request(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/evidence`,
      paymentEvidenceSchema,
      {
        method: 'POST',
        body,
      },
    )
  }

  return { listEvidence, uploadEvidence }
}
