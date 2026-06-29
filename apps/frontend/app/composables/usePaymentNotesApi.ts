/**
 * Domain composable for internal notes on payment orders (F-C7).
 *
 * Delegates transport to `useApiClient`. Notes are internal/platform-only:
 * SUPPORT_AGENT and PLATFORM_ADMIN can read and create. MERCHANT_MANAGER
 * does not have access and the UI section is hidden.
 */

import { z } from 'zod'
import type { ApiResponse } from '~/types/api'

const paymentOrderNoteSchema = z.object({
  id: z.string().uuid(),
  body: z.string(),
  authorDisplay: z.string(),
  createdAt: z.string(),
})

const paymentOrderNotesListSchema = z.array(paymentOrderNoteSchema)

export type PaymentOrderNote = z.infer<typeof paymentOrderNoteSchema>

export function usePaymentNotesApi() {
  const { request } = useApiClient()

  async function listNotes(
    merchantId: string,
    paymentOrderId: string
  ): Promise<ApiResponse<PaymentOrderNote[]>> {
    return request(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/notes`,
      paymentOrderNotesListSchema,
    )
  }

  async function addNote(
    merchantId: string,
    paymentOrderId: string,
    body: string
  ): Promise<ApiResponse<PaymentOrderNote>> {
    return request(
      `/api/merchants/${merchantId}/payment-orders/${paymentOrderId}/notes`,
      paymentOrderNoteSchema,
      { method: 'POST', body: { body } },
    )
  }

  return { listNotes, addNote }
}
