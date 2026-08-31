import { z } from 'zod'

export const problemDetailsSchema = z.object({
  type: z.string().optional(),
  title: z.string().optional(),
  status: z.number().optional(),
  detail: z.string().optional(),
  instance: z.string().optional(),
  // Nuxt BFF emits `{ error: true, statusCode, message }` for unauthenticated
  // requests, while backend ProblemDetails uses a string error code.
  error: z.union([z.string(), z.boolean()]).optional(),
}).passthrough()

export type ProblemDetails = z.infer<typeof problemDetailsSchema>

// oxlint-disable-next-line anti-slop/no-unknown-parameters -- Zod parses this external response value below.
export function isProblemDetails(body: unknown): body is ProblemDetails {
  const parsed = problemDetailsSchema.safeParse(body)
  if (!parsed.success) {
    return false
  }
  const { status, type } = parsed.data
  if (typeof status === 'number' && status >= 400) {
    return true
  }
  return typeof type === 'string' && type.startsWith('http')
}
