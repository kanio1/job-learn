import { z } from 'zod'

export const eventLabStatusSchema = z.enum(['PROCESSED', 'RETRYING', 'DEAD'])

export const eventLabRecordSchema = z.object({
  id: z.string().uuid(),
  eventId: z.string().uuid(),
  consumerGroup: z.string(),
  action: z.string(),
  targetType: z.string(),
  targetId: z.string(),
  tenantRef: z.string(),
  status: eventLabStatusSchema,
  attempts: z.number().int(),
  consumedAt: z.string(),
  lastError: z.string().nullable().optional(),
  topic: z.string(),
  partitionNo: z.number().int(),
  recordOffset: z.number().int(),
  recordKey: z.string().nullable().optional(),
})

export type EventLabRecord = z.infer<typeof eventLabRecordSchema>
export type EventLabStatus = z.infer<typeof eventLabStatusSchema>
