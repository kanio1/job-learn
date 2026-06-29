import { z } from 'zod'

export const problemDetailsSchema = z.object({
  type: z.string().optional(),
  title: z.string().optional(),
  status: z.number().int().optional(),
  detail: z.string().optional(),
  instance: z.string().optional(),
  // Known backend extensions
  correlationId: z.string().optional(),
  error: z.string().optional(),
  requiredHeader: z.string().optional(),
  details: z.array(z.object({
    field: z.string(),
    message: z.string(),
  })).optional(),
  retryable: z.boolean().optional(),
  retryAfterSeconds: z.number().int().optional(),
}).passthrough() // preserve unknown extension members for RawJsonViewer

export type ProblemDetails = z.infer<typeof problemDetailsSchema>
