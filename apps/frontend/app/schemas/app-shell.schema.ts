import { z } from 'zod'

export const appShellConfigSchema = z.object({
  apiBaseUrl: z.string().url(),
  keycloakUrl: z.string().url(),
  phase: z.literal('foundation')
})

export type AppShellConfig = z.infer<typeof appShellConfigSchema>
