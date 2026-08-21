import { z } from 'zod'

export const merchantImportRowSchema = z.object({
  line: z.number().int(),
  status: z.enum(['VALID', 'WARNING', 'REJECTED']),
  merchantReference: z.string(),
  displayName: z.string(),
  tenantReference: z.string(),
  reason: z.string().nullable().optional(),
})

export const merchantImportPreviewSchema = z.object({
  previewId: z.string(),
  validCount: z.number().int().nonnegative(),
  warningCount: z.number().int().nonnegative(),
  rejectedCount: z.number().int().nonnegative(),
  rows: z.array(merchantImportRowSchema),
})

export const merchantImportCommitSchema = z.object({
  createdCount: z.number().int().nonnegative(),
})

export type MerchantImportPreview = z.infer<typeof merchantImportPreviewSchema>
export type MerchantImportCommit = z.infer<typeof merchantImportCommitSchema>
