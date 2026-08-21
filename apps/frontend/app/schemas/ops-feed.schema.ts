import { z } from 'zod'

const backendUuidSchema = z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  'Expected UUID',
)

export const opsFeedFrameSchema = z.object({
  eventId: backendUuidSchema,
  occurredAt: z.string(),
  merchantId: backendUuidSchema.nullable().optional(),
  paymentOrderId: backendUuidSchema.nullable().optional(),
  type: z.string(),
  label: z.string(),
})

export const opsFeedRecentSchema = z.object({
  events: z.array(opsFeedFrameSchema),
})

export const opsFeedInjectSchema = opsFeedFrameSchema.extend({
  malformed: z.boolean().optional(),
}).partial()

export type OpsFeedFrame = z.infer<typeof opsFeedFrameSchema>
