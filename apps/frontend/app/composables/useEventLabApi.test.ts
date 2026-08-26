// @vitest-environment nuxt
import { describe, it, expect, vi, beforeEach } from 'vitest'

const mocks = vi.hoisted(() => ({ request: vi.fn() }))

vi.mock('~/composables/useApiClient', () => ({
  useApiClient: () => ({ request: mocks.request }),
}))

import { useEventLabApi } from './useEventLabApi'

describe('VT-KAFKA-004 detail 404 -> null', () => {
  beforeEach(() => mocks.request.mockReset())
  it('maps 404 detail to null data', async () => {
    mocks.request.mockResolvedValue({ data: null, status: 404, headers: {}, problem: { status: 404, title: 'Not Found', detail: 'not found' }, raw: '' })
    const { detail } = useEventLabApi()
    const res = await detail('00000000-0000-0000-0000-000000000000')
    expect(res.status).toBe(404)
    expect(res.data).toBeNull()
  })
})

describe('VT-KAFKA-005 problem+json 403 populates problem', () => {
  beforeEach(() => mocks.request.mockReset())
  it('list 403 populates problem', async () => {
    mocks.request.mockResolvedValue({ data: null, status: 403, headers: {}, problem: { status: 403, title: 'Forbidden', detail: 'Access denied' }, raw: '' })
    const { list } = useEventLabApi()
    const res = await list()
    expect(res.status).toBe(403)
    expect(res.problem).not.toBeNull()
    expect(res.problem?.status).toBe(403)
  })
  it('injectDuplicate 403 populates problem', async () => {
    mocks.request.mockResolvedValue({ data: null, status: 403, headers: {}, problem: { status: 403, title: 'Forbidden', detail: 'Access denied' }, raw: '' })
    const { injectDuplicate } = useEventLabApi()
    const res = await injectDuplicate('22222222-2222-4222-8222-222222222222')
    expect(res.status).toBe(403)
    expect(res.problem?.detail).toContain('Access denied')
  })
})
