import { z } from 'zod'
import { paymentCurrencySchema, paymentStatusSchema } from './payment-order.schema'

export const paymentViewFiltersSchema = z.object({
  status: paymentStatusSchema.optional(),
  currency: paymentCurrencySchema.optional(),
  minAmount: z.number().int().nonnegative().optional(),
  maxAmount: z.number().int().nonnegative().optional(),
  fromDate: z.string().optional(),
  toDate: z.string().optional(),
  clientOrderReference: z.string().optional(),
  sort: z.string().optional(),
}).strict()

export const paymentViewSchema = z.object({
  id: z.string().min(1),
  name: z.string().trim().min(1).max(80),
  resource: z.literal('PAYMENT_ORDERS'),
  filters: paymentViewFiltersSchema,
  columns: z.array(z.string().min(1)).default([]),
  isDefault: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export const paymentViewListSchema = z.object({
  content: z.array(paymentViewSchema),
})

export const createPaymentViewRequestSchema = z.object({
  name: z.string().trim().min(1).max(80),
  filters: paymentViewFiltersSchema,
  columns: z.array(z.string().min(1)).default([]),
  isDefault: z.boolean().optional(),
})

export type PaymentViewDto = z.infer<typeof paymentViewSchema>
export type CreatePaymentViewRequest = z.infer<typeof createPaymentViewRequestSchema>
