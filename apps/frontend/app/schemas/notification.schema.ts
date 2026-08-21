import { z } from 'zod'

const backendUuidSchema = z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  'Expected UUID',
)

export const notificationSchema = z.object({
  notificationId: backendUuidSchema,
  eventId: backendUuidSchema,
  eventType: z.string(),
  title: z.string(),
  body: z.string(),
  payload: z.record(z.string(), z.unknown()).optional().default({}),
  readAt: z.string().nullable().optional(),
  createdAt: z.string(),
})

export const notificationListSchema = z.object({
  content: z.array(notificationSchema),
})

export type OpsNotification = z.infer<typeof notificationSchema>
