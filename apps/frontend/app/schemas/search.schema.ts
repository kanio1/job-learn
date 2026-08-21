import { z } from 'zod'

const backendUuidSchema = z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  'Expected UUID',
)

export const searchMerchantHitSchema = z.object({
  merchantId: backendUuidSchema,
  merchantReference: z.string(),
  displayName: z.string(),
})

export const searchPaymentHitSchema = z.object({
  paymentOrderId: backendUuidSchema,
  merchantId: backendUuidSchema,
  clientOrderReference: z.string(),
})

export const searchResponseSchema = z.object({
  merchants: z.array(searchMerchantHitSchema),
  payments: z.array(searchPaymentHitSchema),
})

export type SearchMerchantHit = z.infer<typeof searchMerchantHitSchema>
export type SearchPaymentHit = z.infer<typeof searchPaymentHitSchema>
export type SearchResponse = z.infer<typeof searchResponseSchema>
