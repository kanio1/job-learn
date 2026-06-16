import { z } from 'zod'

export const problemDetailsSchema = z.object({
  type: z.string().optional(),
  title: z.string().optional(),
  status: z.number().int().optional(),
  detail: z.string().optional(),
  instance: z.string().optional(),
}).passthrough() // preserve backend extension members for display (RFC 7807)

export type ProblemDetails = z.infer<typeof problemDetailsSchema>
