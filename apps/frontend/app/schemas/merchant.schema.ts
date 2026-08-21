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
  tenantReference: z.string().trim().max(64).optional(),
})

export type CreateMerchantForm = z.infer<typeof createMerchantSchema>

export const merchantListStatusSchema = z.enum(['DRAFT', 'ACTIVE', 'SUSPENDED'])

export const merchantListSortSchema = z.string().regex(
  /^(createdAt|updatedAt|displayName|status),(asc|desc)$/,
)

export const merchantListQuerySchema = z.object({
  q: z.string().trim().min(1).optional(),
  status: merchantListStatusSchema.optional(),
  tenantId: z.string().trim().min(1).optional(),
  page: z.number().int().nonnegative().default(0),
  size: z.number().int().min(1).max(100).default(20),
  sort: merchantListSortSchema.default('createdAt,desc'),
})

export type MerchantListQuery = z.infer<typeof merchantListQuerySchema>
