import { eventLabRecordSchema } from '~/schemas/event-lab.schema'
import { z } from 'zod'

const listSchema = z.array(eventLabRecordSchema)

export function useEventLabApi() {
  const { request } = useApiClient()

  async function list(params: { targetId?: string; eventId?: string } = {}) {
    const query: Record<string, string> = {}
    if (params.targetId) query.targetId = params.targetId
    if (params.eventId) query.eventId = params.eventId
    return request('/api/event-lab', listSchema, { query })
  }

  async function detail(id: string) {
    const res = await request(`/api/event-lab/${encodeURIComponent(id)}`, eventLabRecordSchema)
    if (res.status === 404) return { ...res, data: null as unknown as import('~/schemas/event-lab.schema').EventLabRecord | null }
    return res
  }

  async function injectDuplicate(eventId: string) {
    return request('/api/event-lab/inject/duplicate', eventLabRecordSchema, { method: 'POST', body: { eventId } })
  }

  async function injectPoison(eventId: string) {
    return request('/api/event-lab/inject/poison', eventLabRecordSchema, { method: 'POST', body: { eventId } })
  }

  return { list, detail, injectDuplicate, injectPoison }
}
