import { z } from 'zod'

export const createPaymentOrderSchema = z.object({
  amountMinor: z.coerce.number()
    .int('Amount must be a whole number')
    .min(1, 'Amount must be at least 1')
    .max(100000000, 'Amount must be at most 100,000,000'),
  currency: z.enum(['PLN', 'EUR', 'USD'], {
    message: 'Currency must be PLN, EUR, or USD'
  }),
  clientOrderReference: z.string()
    .trim()
    .min(1, 'Client order reference is required')
    .max(120, 'Client order reference must not exceed 120 characters'),
})

export type CreatePaymentOrderForm = z.infer<typeof createPaymentOrderSchema>
