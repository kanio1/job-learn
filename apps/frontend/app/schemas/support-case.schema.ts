import { z } from 'zod'

const backendUuidSchema = z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  'Expected UUID',
)

export const supportCaseStatusSchema = z.enum(['NEW', 'IN_PROGRESS', 'WAITING', 'RESOLVED'])
export const supportCasePrioritySchema = z.enum(['LOW', 'NORMAL', 'HIGH'])

export const supportCaseSchema = z.object({
  caseId: backendUuidSchema,
  caseReference: z.string(),
  tenantId: backendUuidSchema,
  merchantId: backendUuidSchema,
  paymentOrderId: backendUuidSchema.nullable().optional(),
  status: supportCaseStatusSchema,
  priority: supportCasePrioritySchema,
  assigneeSubject: z.string().nullable().optional(),
  title: z.string(),
  version: z.number().int().nonnegative(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export const supportCaseListSchema = z.object({
  content: z.array(supportCaseSchema),
})

export const bulkAssignFailureSchema = z.object({
  caseId: z.string(),
  caseReference: z.string().nullable().optional(),
  error: z.string(),
})

export const bulkAssignResultSchema = z.object({
  succeeded: z.number().int().nonnegative(),
  failed: z.array(bulkAssignFailureSchema),
})

export type SupportCase = z.infer<typeof supportCaseSchema>
export type SupportCaseStatus = z.infer<typeof supportCaseStatusSchema>
export type BulkAssignResult = z.infer<typeof bulkAssignResultSchema>
