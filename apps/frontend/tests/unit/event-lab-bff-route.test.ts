// @vitest-environment nuxt
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createEvent } from 'h3'

const backendApiMock = vi.hoisted(() => vi.fn())

vi.mock('~~/server/utils/backendApi', () => ({
  backendApi: backendApiMock,
}))

// The real shipped route (no test double of the route itself):
import handler from '~~/server/api/event-lab/index.get'

function makeEvent(query: Record<string, string> = {}) {
  const qs = new URLSearchParams(query).toString()
  // h3 createEvent expects an app; pass a minimal stub with the needed fields.
  const event = createEvent({} as never)
  ;(event as unknown as { node: { req: { url: string; method: string } } }).node = {
    req: { url: '/api/event-lab' + (qs ? `?${qs}` : ''), method: 'GET' },
  }
  return event
}

describe('PW-KAFKA-API-007 / BFF /api/event-lab list route', () => {
  beforeEach(() => {
    backendApiMock.mockReset()
    backendApiMock.mockResolvedValue([{ eventId: '1' }])
  })

  it('passes through the proxied list body as-is (200 array)', async () => {
    const event = makeEvent()
    const body = await handler(event)
    expect(backendApiMock).toHaveBeenCalledWith(event, '/api/event-lab', { method: 'GET' })
    expect(Array.isArray(body)).toBe(true)
  })

  it('forwards targetId and eventId query params to the backend path', async () => {
    const event = makeEvent({ targetId: 'abc', eventId: '11111111-1111-1111-1111-111111111111' })
    await handler(event)
    expect(backendApiMock).toHaveBeenCalledWith(
      event,
      '/api/event-lab?targetId=abc&eventId=11111111-1111-1111-1111-111111111111',
      { method: 'GET' },
    )
  })

  it('rejects unknown query parameters with a 400 (whitelist)', async () => {
    const event = makeEvent({ unknown: '1' })
    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
    expect(backendApiMock).not.toHaveBeenCalled()
  })
})