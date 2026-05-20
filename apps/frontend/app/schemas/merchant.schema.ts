import { z } from 'zod'

export const createMerchantSchema = z.object({
  merchantReference: z.string()
    .trim()
    .min(3, 'Reference must be at least 3 characters')
    .max(64, 'Reference must be at most 64 characters')
    .regex(/^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$/, 'Reference must start and end with a letter or number, and contain only letters, numbers, and hyphens'),
  displayName: z.string()
    .trim()
    .min(2, 'Name must be at least 2 characters')
    .max(120, 'Name must be at most 120 characters'),
})

export type CreateMerchantForm = z.infer<typeof createMerchantSchema>
