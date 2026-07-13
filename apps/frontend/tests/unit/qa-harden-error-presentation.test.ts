// @vitest-environment nuxt
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { defineComponent, h } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import ProblemDetailsCard from '../../app/components/shared/ProblemDetailsCard.vue'
import ErrorLabPage from '../../app/pages/error-lab.vue'

vi.mock('~/composables/useApiClient', () => ({
  useApiClient: () => ({ request: vi.fn() }),
}))

const PassThroughStub = defineComponent({
  setup(_, { slots }) {
    return () => h('div', [slots.header?.(), slots.leading?.(), slots.body?.(), slots.default?.()])
  },
})

describe('QA-HARDEN-01.04 — Problem Details label alignment', () => {
  it('uses the same w-28 label width for standard and extension fields', async () => {
    const wrapper = await mountSuspended(ProblemDetailsCard, {
      props: {
        problem: {
          type: 'https://api.payment-quality.local/problems/precondition-required',
          title: 'Precondition Required',
          status: 428,
          detail: 'If-Match is required',
          instance: '/api/payment-orders/order-101/authorize',
          correlationId: 'corr-101',
          requiredHeader: 'If-Match',
          retryable: true,
          retryAfterSeconds: 30,
          details: [{ field: 'amountMinor', message: 'must be positive' }],
        },
      },
      global: {
        stubs: {
          UCard: PassThroughStub,
          UIcon: true,
          UBadge: { template: '<span><slot /></span>' },
          HttpStatusBadge: { props: ['status'], template: '<span>{{ status }}</span>' },
        },
      },
    })

    const labels = wrapper.findAll('dt')
    expect(labels.map(label => label.text())).toEqual([
      'Type',
      'Title',
      'Status',
      'Detail',
      'Instance',
      'Correlation ID',
      'Required Header',
      'Retryable',
      'Field Errors',
    ])
    for (const label of labels) {
      expect(label.classes(), `${label.text()} width`).toContain('w-28')
      expect(label.classes(), `${label.text()} legacy width`).not.toContain('w-20')
    }
  })
})

describe('QA-HARDEN-01.06 — Error Lab educational descriptions', () => {
  it('explains the exact contract lessons for 401, 428, 429, 304 and replay', async () => {
    const wrapper = await mountSuspended(ErrorLabPage, {
      route: '/error-lab',
      global: {
        stubs: {
          UDashboardPanel: PassThroughStub,
          UDashboardNavbar: PassThroughStub,
          UDashboardSidebarCollapse: true,
          UAlert: { props: ['title', 'description'], template: '<div>{{ title }} {{ description }}</div>' },
          UCard: PassThroughStub,
          UButton: { template: '<button><slot /></button>' },
          UIcon: true,
          HttpStatusBadge: { props: ['status'], template: '<span>{{ status }}</span>' },
          LoadingState: true,
          HeaderKeyValuePanel: true,
          ProblemDetailsCard: true,
          ApiDebugPanel: true,
        },
      },
    })

    const text = wrapper.text()
    expect(text).toContain('WWW-Authenticate response header')
    expect(text).toContain('requiredHeader field in the Problem Details body')
    expect(text).toContain('retryable: true')
    expect(text).toContain('retryAfterSeconds: 30')
    expect(text).toContain('not retry immediately')
    expect(text).toContain('304 is not an error')
    expect(text).toContain('No body is returned')
    expect(text).toContain('201 Created, Idempotency-Replayed: false')
    expect(text).toContain('200, Idempotency-Replayed: true')
    expect(text).toContain('Replay ≠ Conflict')
  })
})
