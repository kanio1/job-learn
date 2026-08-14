import type { H3Event } from 'h3'

export function requireRlsLab(event: H3Event) {
  const config = useRuntimeConfig(event)
  if (config.public.rlsLabEnabled !== true) {
    throw createError({ statusCode: 404, statusMessage: 'Not Found' })
  }
}

export async function requireRlsLabSession(event: H3Event) {
  requireRlsLab(event)
  await requireUserSession(event)
}
