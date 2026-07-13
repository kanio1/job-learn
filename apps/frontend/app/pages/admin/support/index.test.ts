// @vitest-environment nuxt
import { flushPromises } from '@vue/test-utils'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { defineComponent, h } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SupportPage from './index.vue'

const mocks = vi.hoisted(() => ({ request: vi.fn() }))

vi.mock('~/composables/useApiClient', () => ({
  useApiClient: () => ({ request: mocks.request }),
}))

const PassThroughStub = defineComponent({
  setup(_, { slots }) {
    return () => h('div', [slots.header?.(), slots.leading?.(), slots.body?.(), slots.default?.()])
  },
})

const FormFieldStub = defineComponent({
  props: {
    label: String,
    hint: String,
    required: Boolean,
  },
  setup(props, { slots }) {
    return () => h('div', [
      h('span', `${props.label}${props.required ? ' required' : ''}`),
      slots.default?.(),
      props.hint ? h('span', { role: 'note' }, props.hint) : null,
    ])
  },
})

const InputStub = defineComponent({
  inheritAttrs: false,
  props: { modelValue: { type: String, default: '' }, placeholder: String },
  emits: ['update:modelValue'],
  setup(props, { attrs, emit }) {
    return () => h('input', {
      ...attrs,
      value: props.modelValue,
      placeholder: props.placeholder,
      onInput: (event: Event) => emit('update:modelValue', (event.target as HTMLInputElement).value),
    })
  },
})

const ButtonStub = defineComponent({
  inheritAttrs: false,
  props: {
    disabled: Boolean,
    loading: Boolean,
    to: String,
    label: String,
  },
  setup(props, { attrs, slots }) {
    return () => props.to
      ? h('a', { ...attrs, href: props.to }, props.label ?? slots.default?.())
      : h('button', { ...attrs, type: 'button', disabled: props.disabled || props.loading }, slots.default?.())
  },
})

const BusinessStatusBadgeStub = defineComponent({
  props: { status: String },
  setup(props) {
    return () => h('span', { role: 'status', 'aria-label': `Payment status ${props.status}` }, props.status)
  },
})

const TableStub = defineComponent({
  props: {
    data: { type: Array, default: () => [] },
    columns: { type: Array, default: () => [] },
  },
  setup(props, { attrs }) {
    return () => h('table', attrs, [
      h('thead', [h('tr', (props.columns as any[]).map(column => h('th', column.header ?? '')))]),
      h('tbody', (props.data as any[]).map(row => h('tr', (props.columns as any[]).map(column => {
        const content = column.cell
          ? column.cell({ row: { original: row } })
          : row[column.accessorKey]
        return h('td', content)
      })))),
    ])
  },
})

function apiResponse(data: unknown, problem: unknown = null) {
  return { data, status: problem ? 500 : 200, headers: {}, problem, raw: '' }
}

async function mountPage() {
  return mountSuspended(SupportPage, {
    route: '/admin/support',
    global: {
      stubs: {
        UDashboardPanel: PassThroughStub,
        UDashboardNavbar: PassThroughStub,
        UDashboardSidebarCollapse: true,
        UCard: PassThroughStub,
        UFormField: FormFieldStub,
        UInput: InputStub,
        UButton: ButtonStub,
        UTable: TableStub,
        UIcon: true,
        BusinessStatusBadge: BusinessStatusBadgeStub,
        LoadingState: { props: ['message'], template: '<div role="status">{{ message }}</div>' },
        ErrorState: { props: ['message'], template: '<div role="alert">{{ message }}</div>' },
      },
    },
  })
}

describe('QA-HARDEN-01.05 — Support Search gating', () => {
  beforeEach(() => mocks.request.mockReset())

  it('requires a non-blank Merchant ID even when Client Order Reference is present', async () => {
    const wrapper = await mountPage()
    const merchant = wrapper.get('input[aria-label="Merchant ID (required)"]')
    const clientReference = wrapper.get('input[aria-label="Client order reference"]')
    const search = wrapper.get('button')

    expect(wrapper.text()).toContain('Required — narrows search to a single merchant')
    expect(search.attributes('disabled')).toBeDefined()

    await clientReference.setValue('ORDER-101')
    await merchant.setValue('   ')
    expect(search.attributes('disabled')).toBeDefined()
    await search.trigger('click')
    expect(mocks.request).not.toHaveBeenCalled()
  })

  it('trims Merchant ID and optional client reference in the exact request', async () => {
    mocks.request.mockResolvedValue(apiResponse({ content: [] }))
    const wrapper = await mountPage()

    await wrapper.get('input[aria-label="Merchant ID (required)"]').setValue('  merchant-101  ')
    await wrapper.get('input[aria-label="Client order reference"]').setValue('  ORDER-101  ')
    const search = wrapper.get('button')
    expect(search.attributes('disabled')).toBeUndefined()
    await search.trigger('click')
    await flushPromises()

    expect(mocks.request).toHaveBeenCalledWith(
      '/api/merchants/merchant-101/payment-orders',
      expect.anything(),
      { query: { clientOrderReference: 'ORDER-101' } },
    )
  })
})

describe('QA-HARDEN-01.09 — Support Search results', () => {
  beforeEach(() => mocks.request.mockReset())

  it('renders typed status, formatted date and a uniquely named exact detail link', async () => {
    const localeSpy = vi.spyOn(Date.prototype, 'toLocaleString').mockReturnValue('13.07.2026, 12:34:56')
    mocks.request.mockResolvedValue(apiResponse({
      content: [{
        paymentOrderId: 'payment-202',
        merchantId: 'merchant-101',
        clientOrderReference: 'ORDER-101',
        status: 'AUTHORIZED',
        amountMinor: 1250,
        currency: 'PLN',
        createdAt: '2026-07-13T10:34:56.123456Z',
      }],
    }))
    const wrapper = await mountPage()

    await wrapper.get('input[aria-label="Merchant ID (required)"]').setValue('merchant-101')
    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(wrapper.find('span[role="status"]').text()).toBe('AUTHORIZED')
    expect(wrapper.text()).toContain('13.07.2026, 12:34:56')
    expect(wrapper.text()).not.toContain('2026-07-13T10:34:56.123456Z')
    const view = wrapper.get('a[aria-label="View payment order ORDER-101"]')
    expect(view.attributes('href')).toBe('/admin/merchants/merchant-101/payments/payment-202')
    expect(localeSpy).toHaveBeenCalledOnce()
    localeSpy.mockRestore()
  })

  it('distinguishes an empty successful result from an error result', async () => {
    mocks.request.mockResolvedValueOnce(apiResponse({ content: [] }))
    const emptyWrapper = await mountPage()
    await emptyWrapper.get('input[aria-label="Merchant ID (required)"]').setValue('merchant-101')
    await emptyWrapper.get('button').trigger('click')
    await flushPromises()
    expect(emptyWrapper.text()).toContain('No results')
    expect(emptyWrapper.find('[role="alert"]').exists()).toBe(false)

    mocks.request.mockResolvedValueOnce(apiResponse(null, { detail: 'Search unavailable' }))
    const errorWrapper = await mountPage()
    await errorWrapper.get('input[aria-label="Merchant ID (required)"]').setValue('merchant-101')
    await errorWrapper.get('button').trigger('click')
    await flushPromises()
    expect(errorWrapper.get('[role="alert"]').text()).toContain('Search unavailable')
    expect(errorWrapper.text()).not.toContain('No payment orders match the search criteria')
  })
})
