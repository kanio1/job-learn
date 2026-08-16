// @vitest-environment nuxt
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import IfMatchInput from '../../app/components/shared/IfMatchInput.vue'
import PaymentOrderLifecycleActions from '../../app/components/shared/PaymentOrderLifecycleActions.vue'
import { usePaymentOrdersStore } from '../../app/stores/payment-orders'

vi.mock('~/composables/useAuthorization', () => ({
  useAuthorization: () => ({
    can: { value: { canRunLifecycle: true } },
  }),
}))

describe('QA-HARDEN-01.10 — If-Match guidance and accessibility', () => {
  it('associates the If-Match label and exact ETag hint with the editable input', async () => {
    const wrapper = await mountSuspended(IfMatchInput, {
      props: { modelValue: '"v3"' },
    })

    const input = wrapper.get('input')
    const label = wrapper.get('label')
    expect(label.text()).toBe('If-Match')
    expect(label.attributes('for')).toBe(input.attributes('id'))
    expect(input.attributes('placeholder')).toBe('e.g. "v3"')

    const describedBy = input.attributes('aria-describedby')
    expect(describedBy).toBeTruthy()
    const hint = wrapper.get(`[id="${describedBy}"]`)
    expect(hint.text()).toBe('The ETag from the last GET response — required for lifecycle actions')

    await input.setValue('"v4"')
    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['"v4"'])
  })
})

function setCurrentOrder(status: string) {
  const store = usePaymentOrdersStore()
  store.currentOrder = {
    paymentOrderId: 'payment-101',
    merchantId: 'merchant-101',
    clientOrderReference: 'ORDER-101',
    amountMinor: 5000,
    currency: 'PLN',
    status,
    createdAt: '2026-07-13T10:00:00Z',
    updatedAt: '2026-07-13T10:00:00Z',
  } as never
}

async function mountActions(status: string) {
  await mountSuspended(PaymentOrderLifecycleActions, {
    props: {
      paymentOrderId: 'payment-101',
      merchantId: 'merchant-101',
    },
  })
  setCurrentOrder(status)
  return mountSuspended(PaymentOrderLifecycleActions, {
    props: {
      paymentOrderId: 'payment-101',
      merchantId: 'merchant-101',
    },
  })
}

describe('QA-HARDEN-01.11 — capture/refund minor-unit guidance', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('shows and emits the capture amount in minor units only for an authorized order', async () => {
    const wrapper = await mountActions('AUTHORIZED')
    const input = wrapper.get('input[aria-label="Capture amount in minor units"]')

    expect(input.attributes('placeholder')).toBe('Minor units (empty = full)')
    expect(input.attributes('type')).toBe('number')
    expect(input.attributes('min')).toBe('1')
    expect(wrapper.find('input[aria-label="Refund amount in minor units"]').exists()).toBe(false)

    await input.setValue('2500')
    await wrapper.get('[data-testid="lifecycle-capture"]').trigger('click')
    expect(wrapper.emitted('action-triggered')?.at(-1)).toEqual(['capture', 2500])
  })

  it('does not present a direct refund control for captured orders', async () => {
    const wrapper = await mountActions('CAPTURED')
    expect(wrapper.find('input[aria-label="Refund amount in minor units"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="lifecycle-refund"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('dual-control')
  })

  it('does not present capture or refund inputs when neither action is supported', async () => {
    const wrapper = await mountActions('CREATED')

    expect(wrapper.find('input[aria-label="Capture amount in minor units"]').exists()).toBe(false)
    expect(wrapper.find('input[aria-label="Refund amount in minor units"]').exists()).toBe(false)
  })
})
