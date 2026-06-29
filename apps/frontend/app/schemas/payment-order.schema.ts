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
  metadata: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
  // Application-held version marker (derived from ETag or response field)
  versionMarker: z.string().nullable().optional(),
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
  actorDisplay: z.string().nullable().optional(), // safe display value only
  reason: z.string().nullable().optional(),
  amountMinor: z.number().int().nullable().optional(),
  pspReference: z.string().nullable().optional(),
  correlationId: z.string().nullable().optional(),
  createdAt: z.string(),
  // Internal fields (not for UI display)
  actorSubject: z.string().optional(),
})

export const paymentStatusHistoryResponseSchema = z.object({
  content: z.array(statusHistoryEntrySchema),
})

export const paymentEvidenceSchema = z.object({
  evidenceId: z.string().uuid(),
  paymentOrderId: z.string().uuid(),
  originalFilename: z.string(),
  contentType: z.enum(['application/pdf', 'image/png', 'image/jpeg', 'text/plain', 'text/csv']),
  sizeBytes: z.number().int().positive(),
  uploadedAt: z.string(),
})

export const paymentEvidenceListResponseSchema = z.object({
  content: z.array(paymentEvidenceSchema),
})

export const backendErrorSchema = z.object({
  error: z.string().optional(),
  message: z.string().optional(),
}).passthrough()

// Lifecycle-specific error categories for UI feedback mapping
export const lifecycleErrorCategorySchema = z.enum([
  'validation',
  'invalid_transition',
  'forbidden',
  'not_found',
  'stale_state',
  'idempotency_conflict',
  'backend_unavailable',
])

export type PaymentOrderResponse = z.infer<typeof paymentOrderResponseSchema>
export type PaymentOrderListResponse = z.infer<typeof paymentOrderListResponseSchema>
export type PaymentOrderSummaryResponse = z.infer<typeof paymentOrderSummaryResponseSchema>
export type StatusHistoryEntry = z.infer<typeof statusHistoryEntrySchema>
export type PaymentStatusHistoryResponse = z.infer<typeof paymentStatusHistoryResponseSchema>
export type PaymentEvidence = z.infer<typeof paymentEvidenceSchema>
export type PaymentEvidenceListResponse = z.infer<typeof paymentEvidenceListResponseSchema>
export type BackendErrorResponse = z.infer<typeof backendErrorSchema>
export type LifecycleErrorCategory = z.infer<typeof lifecycleErrorCategorySchema>

// ---------------------------------------------------------------------------
// Filter/query schema for GET .../payment-orders list endpoint
// Supports exactly the params the backend list endpoint accepts
// ---------------------------------------------------------------------------

export const paymentOrderListQuerySchema = z.object({
  status: paymentStatusSchema.optional(),
  currency: paymentCurrencySchema.optional(),
  fromDate: z.string().optional(),
  toDate: z.string().optional(),
  minAmount: z.number().int().nonnegative().optional(),
  maxAmount: z.number().int().nonnegative().optional(),
  clientOrderReference: z.string().optional(),
  page: z.number().int().nonnegative().default(0),
  size: z.number().int().min(1).max(100).default(20),
  sort: z.string().optional(),
})

export type PaymentOrderListQuery = z.infer<typeof paymentOrderListQuerySchema>
