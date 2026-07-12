import { z } from 'zod'

const nativeDateSchema = z.string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, 'Date must use yyyy-MM-dd')
  .refine((value) => {
    const [year, month, day] = value.split('-').map(Number)
    const date = new Date(Date.UTC(year!, month! - 1, day!))
    return date.getUTCFullYear() === year
      && date.getUTCMonth() === month! - 1
      && date.getUTCDate() === day
  }, 'Date must be a valid calendar date')

const optionalFilterSchema = z.string()
  .trim()
  .transform(value => value || undefined)
  .optional()

export const outcomeSchema = z.enum(['SUCCESS', 'DENIED', 'FAILED'])

// Field-level before/after state for the audit diff drawer (F-D7). Only
// present on the detail response (GET /api/audit/{id}), not the list view.
const auditStateSchema = z.record(z.string(), z.unknown()).nullable().optional()

export const auditEventSchema = z.object({
  id: z.string().uuid(),
  occurredAt: z.iso.datetime({ offset: true }),
  actorDisplay: z.string(),
  action: z.string(),
  targetType: z.string(),
  targetId: z.string(),
  tenantId: z.string(),
  correlationId: z.string().nullable(),
  outcome: outcomeSchema,
  beforeState: auditStateSchema,
  afterState: auditStateSchema,
})

export const auditListResponseSchema = z.object({
  content: z.array(auditEventSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive().max(100),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
})

export const auditQuerySchema = z.object({
  actor: optionalFilterSchema,
  action: optionalFilterSchema,
  targetType: optionalFilterSchema,
  from: nativeDateSchema.optional(),
  to: nativeDateSchema.optional(),
  page: z.number().int().nonnegative().default(0),
  size: z.number().int().min(1).max(100).default(20),
}).refine(
  query => !query.from || !query.to || query.from <= query.to,
  { message: 'From date must not be after to date', path: ['to'] }
)

export type AuditOutcome = z.infer<typeof outcomeSchema>
export type AuditEvent = z.infer<typeof auditEventSchema>
export type AuditListResponse = z.infer<typeof auditListResponseSchema>
export type AuditQuery = z.input<typeof auditQuerySchema>

