import { z } from 'zod'

export const paymentCurrencySchema = z.enum(['PLN', 'EUR', 'USD'])
export const paymentStatusSchema = z.enum(['CREATED', 'AUTHORIZED', 'CAPTURED', 'CANCELLED', 'EXPIRED', 'REFUNDED'])

export const createPaymentOrderSchema = z.object({
  amountMinor: z.coerce.number()
    .int('Amount must be a whole number')
    .min(1, 'Amount must be at least 1')
    .max(100000000, 'Amount must be at most 100,000,000'),
  currency: paymentCurrencySchema,
  clientOrderReference: z.string()
    .trim()
    .min(1, 'Client order reference is required')
    .max(120, 'Client order reference must not exceed 120 characters'),
})

export type CreatePaymentOrderForm = z.infer<typeof createPaymentOrderSchema>

export const paymentOrderResponseSchema = z.object({
  paymentOrderId: z.string().uuid(),
  merchantId: z.string().uuid(),
  clientOrderReference: z.string(),
  amountMinor: z.number().int(),
  currency: paymentCurrencySchema,
  status: paymentStatusSchema,
  capturedAmountMinor: z.number().int().nullable().optional(),
  refundedAmountMinor: z.number().int().nullable().optional(),
  authorizedAt: z.string().nullable().optional(),
  expiresAt: z.string().nullable().optional(),
  capturedAt: z.string().nullable().optional(),
  cancelledAt: z.string().nullable().optional(),
  refundedAt: z.string().nullable().optional(),
  cancellationReason: z.string().nullable().optional(),
  refundReason: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export const paymentOrderListResponseSchema = z.object({
  content: z.array(paymentOrderResponseSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
})

export const paymentOrderSummaryResponseSchema = z.object({
  totalOrders: z.number().int().nonnegative(),
  totalAmountMinor: z.number().int().nonnegative(),
  byCurrency: z.array(z.object({
    currency: paymentCurrencySchema,
    orderCount: z.number().int().nonnegative(),
    totalAmountMinor: z.number().int().nonnegative(),
  })),
  byStatus: z.array(z.object({
    status: paymentStatusSchema,
    orderCount: z.number().int().nonnegative(),
    totalAmountMinor: z.number().int().nonnegative(),
  })),
})

export const statusHistoryEntrySchema = z.object({
  statusHistoryId: z.string().uuid(),
  paymentOrderId: z.string().uuid(),
  fromStatus: z.string().nullable(),
  toStatus: z.string(),
  action: z.string().nullable(),
  actorSubject: z.string(),
  idempotencyKeyHash: z.string().nullable(),
  correlationId: z.string(),
  createdAt: z.string(),
})

export const paymentStatusHistoryResponseSchema = z.object({
  content: z.array(statusHistoryEntrySchema),
})

export const backendErrorSchema = z.object({
  error: z.string().optional(),
  message: z.string().optional(),
}).passthrough()

export type PaymentOrderResponse = z.infer<typeof paymentOrderResponseSchema>
export type PaymentOrderListResponse = z.infer<typeof paymentOrderListResponseSchema>
export type PaymentOrderSummaryResponse = z.infer<typeof paymentOrderSummaryResponseSchema>
export type StatusHistoryEntry = z.infer<typeof statusHistoryEntrySchema>
export type PaymentStatusHistoryResponse = z.infer<typeof paymentStatusHistoryResponseSchema>
export type BackendErrorResponse = z.infer<typeof backendErrorSchema>
