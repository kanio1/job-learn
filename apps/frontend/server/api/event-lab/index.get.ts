import { createError, getQuery, defineEventHandler } from 'h3'
import { backendApi } from '~~/server/utils/backendApi'

export default defineEventHandler(async (event) => {
  // SAFETY: getQuery returns string|string[] values; the whitelist below rejects
  // any key outside {targetId, eventId} and those are used only as single strings.
  const query = getQuery(event) as Record<string, string>
  const allowed = new Set(['targetId', 'eventId'])
  for (const k of Object.keys(query)) {
    if (!allowed.has(k)) throw createError({ statusCode: 400, statusMessage: 'Unknown query param' })
  }
  const qs = new URLSearchParams()
  if (query.targetId) qs.set('targetId', query.targetId)
  if (query.eventId) qs.set('eventId', query.eventId)
  const path = qs.toString() ? `/api/event-lab?${qs.toString()}` : '/api/event-lab'
  return backendApi(event, path, { method: 'GET' })
})
