// @vitest-environment nuxt
import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { defineComponent, h } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ApiDebugPanel from '../../app/components/shared/ApiDebugPanel.vue'
import RawJsonViewer from '../../app/components/shared/RawJsonViewer.vue'

const routeMocks = vi.hoisted(() => ({
  fetchRaw: vi.fn(),
  getUserSession: vi.fn(),
  setHeader: vi.fn(),
  setResponseStatus: vi.fn(),
}))

mockNuxtImport('useRuntimeConfig', () => () => ({
  public: { apiBaseUrl: 'http://backend.test' },
}))
mockNuxtImport('setResponseStatus', () => routeMocks.setResponseStatus)

vi.stubGlobal('$fetch', { raw: routeMocks.fetchRaw })
vi.stubGlobal('defineEventHandler', (handler: unknown) => handler)
vi.stubGlobal('getUserSession', routeMocks.getUserSession)
vi.stubGlobal('setHeader', routeMocks.setHeader)
vi.stubGlobal('setResponseStatus', routeMocks.setResponseStatus)

const CardStub = defineComponent({
  setup(_, { slots }) {
    return () => h('section', [slots.header?.(), slots.default?.()])
  },
})

async function mountRaw(content: string) {
  return mountSuspended(RawJsonViewer, {
    props: { content },
    global: {
      stubs: {
        UCard: CardStub,
        UBadge: { template: '<span><slot /></span>' },
      },
    },
  })
}

describe('QA-HARDEN-01.01 — Error Lab 304 contract', () => {
  beforeEach(() => {
    routeMocks.fetchRaw.mockReset()
    routeMocks.getUserSession.mockReset()
    routeMocks.setHeader.mockReset()
    routeMocks.setResponseStatus.mockReset()
    routeMocks.getUserSession.mockResolvedValue({ secure: { accessToken: 'session-token' } })
  })

  it('forwards the exact 304 status and cache validators while returning no body', async () => {
    routeMocks.fetchRaw
      .mockResolvedValueOnce({
        status: 200,
        headers: new Headers(),
        _data: { content: [{ merchantId: 'merchant-101', status: 'ACTIVE' }] },
      })
      .mockResolvedValueOnce({
        status: 200,
        headers: new Headers(),
        _data: { content: [{ paymentOrderId: 'payment-202' }] },
      })
      .mockResolvedValueOnce({
        status: 200,
        headers: new Headers({ ETag: '"v3"' }),
        _data: { paymentOrderId: 'payment-202' },
      })
      .mockResolvedValueOnce({
        status: 304,
        headers: new Headers({
          ETag: '"v3"',
          'Last-Modified': 'Mon, 13 Jul 2026 10:00:00 GMT',
          'Cache-Control': 'private, no-cache',
          Vary: 'Authorization',
          'X-Correlation-ID': 'corr-304-101',
        }),
        _data: undefined,
      })

    const { default: handler } = await import('../../server/api/error-lab/trigger-304.get')
    const event = { node: { req: {}, res: {} } }
    const body = await handler(event as never)

    expect(body).toBeNull()
    expect(routeMocks.setResponseStatus).toHaveBeenCalledWith(event, 304)
    expect(routeMocks.setHeader.mock.calls).toEqual(expect.arrayContaining([
      [event, 'ETag', '"v3"'],
      [event, 'Last-Modified', 'Mon, 13 Jul 2026 10:00:00 GMT'],
      [event, 'Cache-Control', 'private, no-cache'],
      [event, 'Vary', 'Authorization'],
      [event, 'X-Correlation-ID', 'corr-304-101'],
    ]))
    expect(routeMocks.fetchRaw).toHaveBeenLastCalledWith(
      'http://backend.test/api/merchants/merchant-101/payment-orders/payment-202',
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({
          Authorization: 'Bearer session-token',
          'If-None-Match': '"v3"',
        }),
      }),
    )
  })
})

describe('QA-HARDEN-01.02 — RawJsonViewer empty-body state', () => {
  it.each(['', '   \n\t'])('renders “No body” and no pre for blank content %#', async (content) => {
    const wrapper = await mountRaw(content)

    expect(wrapper.text()).toContain('No body')
    expect(wrapper.find('pre').exists()).toBe(false)
  })

  it('renders non-empty content in pre instead of the empty state', async () => {
    const wrapper = await mountRaw('{"status":"ok"}')

    expect(wrapper.find('pre').text()).toContain('"status": "ok"')
    expect(wrapper.text()).not.toContain('No body')
  })
})

describe('QA-HARDEN-01.03 — ApiDebugPanel empty-body presentation', () => {
  it.each([undefined, ''])('keeps the response body section visible for body %s', async (body) => {
    const wrapper = await mountSuspended(ApiDebugPanel, {
      props: {
        response: { status: 304, headers: { ETag: '"v3"' }, body },
      },
      global: {
        stubs: {
          UCard: CardStub,
          UIcon: true,
          UBadge: true,
          UTabs: {
            template: '<div><slot name="request" /><slot name="response" /></div>',
          },
          HttpStatusBadge: { props: ['status'], template: '<span>{{ status }}</span>' },
          HeaderKeyValuePanel: { template: '<div />' },
          RawJsonViewer,
        },
      },
    })

    expect(wrapper.text()).toContain('Response Body')
    expect(wrapper.text()).toContain('No body')
    expect(wrapper.find('pre').exists()).toBe(false)
  })
})
