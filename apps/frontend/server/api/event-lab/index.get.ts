import { createError, getQuery, defineEventHandler, getRequestHeader } from 'h3'
import { backendApi } from '~~/server/utils/backendApi'

export default defineEventHandler(async (event) => {
  const query = getQuery(event) as Record<string, string>
  const allowed = new Set(['targetId', 'eventId'])
  for (const k of Object.keys(query)) {
    if (!allowed.has(k)) throw createError({ statusCode: 400, statusMessage: 'Unknown query param' })
  }
  const qs = new URLSearchParams()
  if (query.targetId) qs.set('targetId', query.targetId)
  if (query.eventId) qs.set('eventId', query.eventId)
  const path = qs.toString() ? `/api/event-lab?${qs.toString()}` : '/api/event-lab'
  const res = await backendApi(event, path, { method: 'GET' })
  // Pass through X-Correlation-ID
  const cid = getRequestHeader(event, 'x-correlation-id') || res.headers['x-correlation-id']
  if (cid) event.node.res.setHeader('X-Correlation-ID', cid)
  return res.data
})
