// @vitest-environment nuxt
import { flushPromises } from '@vue/test-utils'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { defineComponent, h } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PaymentOrderDetail from '../../app/components/payment/PaymentOrderDetail.vue'
import MerchantDetailPage from '../../app/pages/admin/merchants/[merchantId]/index.vue'

const mocks = vi.hoisted(() => ({
  getMerchant: vi.fn(),
  activateMerchant: vi.fn(),
  suspendMerchant: vi.fn(),
  updateMerchantRiskFlag: vi.fn(),
}))

vi.mock('~/composables/useMerchantsApi', () => ({
  useMerchantsApi: () => ({
    getMerchant: mocks.getMerchant,
    activateMerchant: mocks.activateMerchant,
    suspendMerchant: mocks.suspendMerchant,
    updateMerchantRiskFlag: mocks.updateMerchantRiskFlag,
  }),
}))

vi.mock('~/composables/useAuthorization', () => ({
  useAuthorization: () => ({
    can: { value: { canUpdateMerchantStatus: false, canUpdateMerchantRiskFlag: false } },
  }),
}))

vi.mock('~/composables/useAppToast', () => ({
  useAppToast: () => ({ success: vi.fn(), error: vi.fn(), warning: vi.fn() }),
}))

const PassThroughStub = defineComponent({
  setup(_, { slots }) {
    return () => h('div', [
      slots.header?.(),
      slots.leading?.(),
      slots.right?.(),
      slots.body?.(),
      slots.business?.(),
      slots.http?.(),
      slots.raw?.(),
      slots.history?.(),
      slots.default?.(),
    ])
  },
})

const HeadersStub = defineComponent({
  props: { headers: { type: Object, default: () => ({}) } },
  setup(props) {
    return () => h('dl', { 'data-testid': 'rendered-headers' },
      Object.entries(props.headers).flatMap(([name, value]) => [h('dt', name), h('dd', String(value))]),
    )
  },
})

function merchantResponse(data: unknown, problem: unknown = null) {
  return { data, status: problem ? 502 : 200, headers: {}, problem, raw: '' }
}

function merchant(overrides: Record<string, unknown> = {}) {
  return {
    merchantId: '11111111-1111-1111-1111-111111111111',
    merchantReference: 'MERCHANT_ALPHA_001',
    displayName: 'Alpha Merchant',
    status: 'ACTIVE',
    riskFlagged: false,
    createdAt: '2026-03-29T01:59:59.123456+01:00',
    updatedAt: '2026-10-25T02:30:00.654321+02:00',
    ...overrides,
  }
}

async function mountMerchantPage() {
  return mountSuspended(MerchantDetailPage, {
    route: '/admin/merchants/11111111-1111-1111-1111-111111111111',
    global: {
      stubs: {
        UDashboardPanel: PassThroughStub,
        UDashboardNavbar: PassThroughStub,
        UDashboardSidebarCollapse: true,
        UCard: PassThroughStub,
        UButton: { template: '<button><slot />{{ label }}</button>', props: ['label'] },
        UBadge: { template: '<span><slot /></span>' },
        MerchantStatusBadge: { props: ['status'], template: '<span>{{ status }}</span>' },
        EtagDisplay: true,
        HeaderKeyValuePanel: true,
        LoadingState: { props: ['message'], template: '<div role="status">{{ message }}</div>' },
        ErrorState: { props: ['message'], template: '<div role="alert">{{ message }}</div>' },
      },
    },
  })
}

describe('QA-HARDEN-01.07 — PaymentOrderDetail HTTP headers', () => {
  it('presents the exact Last-Modified and Idempotency-Replayed names and values', async () => {
    const wrapper = await mountSuspended(PaymentOrderDetail, {
      props: {
        order: null,
        apiStatus: 200,
        apiHeaders: {
          lastModified: 'Mon, 13 Jul 2026 10:00:00 GMT',
          idempotencyReplayed: 'true',
        },
      },
      global: {
        stubs: {
          UTabs: PassThroughStub,
          HttpStatusBadge: { props: ['status'], template: '<span>{{ status }}</span>' },
          EtagDisplay: true,
          HeaderKeyValuePanel: HeadersStub,
          RawJsonViewer: true,
        },
      },
    })

    const headers = wrapper.get('[data-testid="rendered-headers"]')
    expect(headers.findAll('dt').map(node => node.text())).toEqual([
      'Last-Modified',
      'Idempotency-Replayed',
    ])
    expect(headers.findAll('dd').map(node => node.text())).toEqual([
      'Mon, 13 Jul 2026 10:00:00 GMT',
      'true',
    ])
  })
})

describe('QA-HARDEN-01.08 — merchant detail date presentation', () => {
  beforeEach(() => mocks.getMerchant.mockReset())

  it('formats both offset/DST timestamps without exposing backend ISO precision', async () => {
    const localeSpy = vi.spyOn(Date.prototype, 'toLocaleString').mockImplementation(function () {
      return `localized:${this.toISOString()}`
    })
    mocks.getMerchant.mockResolvedValue(merchantResponse(merchant()))

    const wrapper = await mountMerchantPage()
    await flushPromises()

    expect(wrapper.text()).toContain('localized:2026-03-29T00:59:59.123Z')
    expect(wrapper.text()).toContain('localized:2026-10-25T00:30:00.654Z')
    expect(wrapper.text()).not.toContain('2026-03-29T01:59:59.123456+01:00')
    expect(wrapper.text()).not.toContain('2026-10-25T02:30:00.654321+02:00')
    expect(localeSpy).toHaveBeenCalledTimes(2)
    localeSpy.mockRestore()
  })

  it('renders the API error state rather than dates when a null timestamp response is rejected', async () => {
    mocks.getMerchant.mockResolvedValue(merchantResponse(null, {
      detail: 'Response validation failed for null createdAt',
    }))

    const wrapper = await mountMerchantPage()
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('Response validation failed for null createdAt')
    expect(wrapper.text()).not.toContain('Created')
    expect(wrapper.text()).not.toContain('Updated')
  })

  it('handles an invalid timestamp deterministically without leaking the raw value', async () => {
    const localeSpy = vi.spyOn(Date.prototype, 'toLocaleString').mockImplementation(function () {
      return Number.isNaN(this.getTime()) ? 'Invalid date' : `localized:${this.toISOString()}`
    })
    mocks.getMerchant.mockResolvedValue(merchantResponse(merchant({ createdAt: 'not-a-timestamp' })))

    const wrapper = await mountMerchantPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Invalid date')
    expect(wrapper.text()).not.toContain('not-a-timestamp')
    localeSpy.mockRestore()
  })
})
