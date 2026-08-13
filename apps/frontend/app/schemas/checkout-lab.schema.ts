import { z } from 'zod'

const checkoutSessionStatusSchema = z.enum(['CREATED', 'PENDING', 'COMPLETED', 'CANCELED', 'EXPIRED'])
const fulfillmentStatusSchema = z.enum(['AWAITING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED'])
const bookingModeSchema = z.enum(['ONLINE', 'CASH'])
const checkoutEventProcessStatusSchema = z.enum(['RECEIVED', 'PROCESSING', 'DONE', 'FAILED', 'DUPLICATE'])

export const checkoutSessionSchema = z.object({
  sessionId: z.string().uuid(),
  extOrderId: z.string(),
  status: checkoutSessionStatusSchema,
  amountMinor: z.number(),
  currency: z.enum(['PLN', 'EUR', 'USD']),
  validityUntil: z.string().nullable().optional(),
  continueUrl: z.string().optional(),
  notifyUrl: z.string().optional(),
  redirectUri: z.string().optional(),
  correlationId: z.string().optional(),
})

export const hostedCheckoutSessionSchema = checkoutSessionSchema
  .omit({ notifyUrl: true, redirectUri: true, correlationId: true })
  .extend({
    simulateToken: z.string().nullable().optional(),
    simulateTokenExpiresAt: z.string().nullable().optional(),
  })

export const createSessionResponseSchema = z.object({
  sessionId: z.string().uuid(),
  redirectUri: z.string(),
  status: checkoutSessionStatusSchema,
})

export const fulfillmentSchema = z.object({
  fulfillmentId: z.string().uuid(),
  sessionId: z.string().uuid().nullable().optional(),
  status: fulfillmentStatusSchema,
  sourceEventId: z.string().nullable().optional(),
  confirmedAt: z.string().nullable().optional(),
})

export const bookingResultSchema = z.object({
  bookingId: z.string().uuid(),
  mode: bookingModeSchema,
  fulfillmentStatus: fulfillmentStatusSchema,
  sessionId: z.string().uuid().nullable().optional(),
  redirectUri: z.string().nullable().optional(),
  validityUntil: z.string().nullable().optional(),
})

export const checkoutEventSchema = z.object({
  eventId: z.string(),
  sessionId: z.string().uuid(),
  eventType: z.string(),
  signatureHeader: z.string().nullable().optional(),
  processStatus: checkoutEventProcessStatusSchema,
  attempts: z.number(),
  ackStatus: z.number().nullable().optional(),
  payload: z.record(z.string(), z.unknown()),
  receivedAt: z.string(),
  lastError: z.string().nullable().optional(),
})

export const deliverySchema = z.object({
  eventId: z.string(),
  attempt: z.number(),
  responseStatus: z.number(),
  at: z.string(),
})

export const anomalySchema = z.object({
  anomalyId: z.string().uuid(),
  sessionId: z.string().uuid().nullable().optional(),
  kind: z.string(),
  detail: z.string(),
  detectedAt: z.string(),
})

export type CheckoutSession = z.infer<typeof checkoutSessionSchema>
export type HostedCheckoutSession = z.infer<typeof hostedCheckoutSessionSchema>
export type Fulfillment = z.infer<typeof fulfillmentSchema>
export type BookingResult = z.infer<typeof bookingResultSchema>
export type CheckoutEvent = z.infer<typeof checkoutEventSchema>
