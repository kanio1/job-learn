import type { H3Event } from 'h3'
import { createError } from 'h3'

export async function backendApi(
  event: H3Event,
  path: string,
  opts: { method?: string; body?: any } = {}
) {
  const config = useRuntimeConfig()
  const backendUrl = config.public.apiBaseUrl || 'http://localhost:8080'
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }

  const session = await requireUserSession(event)
  const accessToken = session.secure?.accessToken
  if (!accessToken) {
    throw createError({
      statusCode: 401,
      statusMessage: 'Authenticated session is missing a backend access token',
      data: { error: 'missing_access_token' }
    })
  }

  headers.Authorization = `Bearer ${accessToken}`

  try {
    return await $fetch(`${backendUrl}${path}`, {
      method: (opts.method || 'GET') as any,
      body: opts.body,
      headers
    })
  } catch (error: any) {
    const statusCode = error?.statusCode || error?.response?.status

    throw createError({
      statusCode: statusCode || 503,
      statusMessage: error?.data?.message || error?.message || 'Backend request failed',
      data: error?.data || { error: 'backend_unavailable' }
    })
  }
}
