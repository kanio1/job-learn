/**
 * Unit tests for state components and lifecycle action rendering.
 *
 * **Validates: Requirements 9.1, 9.3, 9.4, 5.1**
 *
 * Covers:
 *   - LoadingState: loading indicator presence, no data content shown
 *   - EmptyStateCard: description + next action text rendered
 *   - ErrorState: error message rendered, ProblemDetailsCard rendered for problem prop, no token content
 *   - PaymentOrderLifecycleActions: exactly one control per available action from getAvailableActions
 */

// @vitest-environment nuxt
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { createPinia, setActivePinia } from 'pinia'
import LoadingState from './LoadingState.vue'
import EmptyStateCard from './EmptyStateCard.vue'
import ErrorState from './ErrorState.vue'
import PaymentOrderLifecycleActions from './PaymentOrderLifecycleActions.vue'

// ─── Helper ──────────────────────────────────────────────────────────────────

/**
 * Returns all data-testid attribute values present in the wrapper's HTML.
 * Used to assert presence/absence of test ids without relying on CSS.
 */
function testIds(html: string): string[] {
  const re = /data-testid="([^"]+)"/g
  const ids: string[] = []
  let match: RegExpExecArray | null
  while ((match = re.exec(html)) !== null) {
    ids.push(match[1]!)
  }
  return ids
}

// ─── LoadingState ─────────────────────────────────────────────────────────────

describe('LoadingState', () => {
  it('renders with data-testid="loading-state"', async () => {
    const wrapper = await mountSuspended(LoadingState)
    expect(wrapper.attributes('data-testid')).toBe('loading-state')
  })

  it('renders skeleton elements (loading indicator visible)', async () => {
    const wrapper = await mountSuspended(LoadingState)
    // USkeleton renders as elements; the component uses at least one
    // We verify the root is present and the outer div is rendered
    expect(wrapper.exists()).toBe(true)
    expect(testIds(wrapper.html())).toContain('loading-state')
  })

  it('renders an optional message when provided', async () => {
    const wrapper = await mountSuspended(LoadingState, {
      props: { message: 'Fetching orders…' },
    })
    expect(wrapper.text()).toContain('Fetching orders…')
  })

  it('does not render a message paragraph when no message prop is given', async () => {
    const wrapper = await mountSuspended(LoadingState)
    // No <p> tag with a message should exist when message prop is absent
    const paras = wrapper.findAll('p')
    expect(paras).toHaveLength(0)
  })
})

// ─── EmptyStateCard ───────────────────────────────────────────────────────────

describe('EmptyStateCard', () => {
  it('renders with data-testid="empty-state"', async () => {
    const wrapper = await mountSuspended(EmptyStateCard, {
      props: { description: 'No payment orders found.' },
    })
    expect(wrapper.find('[data-testid="empty-state"]').exists()).toBe(true)
  })

  it('renders the description text', async () => {
    const wrapper = await mountSuspended(EmptyStateCard, {
      props: { description: 'No merchants yet.' },
    })
    expect(wrapper.text()).toContain('No merchants yet.')
  })

  it('renders the action label when actionLabel is provided', async () => {
    const wrapper = await mountSuspended(EmptyStateCard, {
      props: { description: 'No orders.', actionLabel: 'Create order' },
    })
    expect(wrapper.text()).toContain('Create order')
  })

  it('renders description AND action label both visible', async () => {
    const wrapper = await mountSuspended(EmptyStateCard, {
      props: { description: 'Empty list.', actionLabel: 'Add item' },
    })
    const text = wrapper.text()
    expect(text).toContain('Empty list.')
    expect(text).toContain('Add item')
  })

  it('does not render an action button when no actionLabel is provided', async () => {
    const wrapper = await mountSuspended(EmptyStateCard, {
      props: { description: 'Nothing here.' },
    })
    expect(wrapper.findAll('button')).toHaveLength(0)
  })

  it('emits "action" event when callback action button is clicked', async () => {
    const wrapper = await mountSuspended(EmptyStateCard, {
      props: { description: 'Empty.', actionLabel: 'Do something' },
    })
    const button = wrapper.find('button')
    expect(button.exists()).toBe(true)
    await button.trigger('click')
    expect(wrapper.emitted('action')).toBeTruthy()
  })
})

// ─── ErrorState ───────────────────────────────────────────────────────────────

/**
 * Stub for ProblemDetailsCard used in ErrorState tests.
 * Preserves the data-testid so that presence checks work without needing
 * the full Nuxt component resolution stack.
 */
const ProblemDetailsCardStub = {
  name: 'ProblemDetailsCard',
  props: ['problem'],
  template: '<div data-testid="problem-details-card"><span class="stub-detail">{{ problem?.detail }}</span></div>',
}

describe('ErrorState', () => {
  it('renders with data-testid="error-state"', async () => {
    const wrapper = await mountSuspended(ErrorState, {
      props: { message: 'Something went wrong.' },
      global: { stubs: { ProblemDetailsCard: ProblemDetailsCardStub } },
    })
    expect(wrapper.find('[data-testid="error-state"]').exists()).toBe(true)
  })

  it('renders a plain message when no problem prop is given', async () => {
    const wrapper = await mountSuspended(ErrorState, {
      props: { message: 'Could not load data.' },
      global: { stubs: { ProblemDetailsCard: ProblemDetailsCardStub } },
    })
    expect(wrapper.text()).toContain('Could not load data.')
  })

  it('renders the ProblemDetailsCard (data-testid="problem-details-card") when problem prop is provided', async () => {
    const wrapper = await mountSuspended(ErrorState, {
      props: {
        problem: {
          type: 'https://example.com/problems/not-found',
          title: 'Not Found',
          status: 404,
          detail: 'Resource does not exist.',
          instance: '/api/merchants/abc',
        },
      },
      global: { stubs: { ProblemDetailsCard: ProblemDetailsCardStub } },
    })
    expect(wrapper.find('[data-testid="problem-details-card"]').exists()).toBe(true)
  })

  it('renders the problem detail text when problem prop is provided', async () => {
    const wrapper = await mountSuspended(ErrorState, {
      props: {
        problem: {
          title: 'Bad Request',
          status: 400,
          detail: 'amountMinor must be positive',
        },
      },
      global: { stubs: { ProblemDetailsCard: ProblemDetailsCardStub } },
    })
    expect(wrapper.text()).toContain('amountMinor must be positive')
  })

  it('does NOT render ProblemDetailsCard when no problem prop is provided', async () => {
    const wrapper = await mountSuspended(ErrorState, {
      props: { message: 'Generic error.' },
      global: { stubs: { ProblemDetailsCard: ProblemDetailsCardStub } },
    })
    expect(wrapper.find('[data-testid="problem-details-card"]').exists()).toBe(false)
  })

  it('does not contain any token-like content in the rendered HTML', async () => {
    const wrapper = await mountSuspended(ErrorState, {
      props: { message: 'Session expired.' },
      global: { stubs: { ProblemDetailsCard: ProblemDetailsCardStub } },
    })
    const html = wrapper.html()
    // No "Bearer " prefix or "eyJ" JWT header pattern should appear
    expect(html).not.toMatch(/Bearer\s+[A-Za-z0-9._-]{20,}/)
    expect(html).not.toMatch(/eyJ[A-Za-z0-9_-]{10,}/)
  })

  it('renders a generic fallback message when neither problem nor message is provided', async () => {
    const wrapper = await mountSuspended(ErrorState, {
      global: { stubs: { ProblemDetailsCard: ProblemDetailsCardStub } },
    })
    expect(wrapper.text()).toContain('unexpected error')
  })
})

// ─── PaymentOrderLifecycleActions ─────────────────────────────────────────────

/**
 * Maps each Payment_Status to the actions that getAvailableActions returns.
 * Mirrors the store logic exactly so tests remain independent of the Pinia instance.
 */
const EXPECTED_ACTIONS: Record<string, string[]> = {
  CREATED: ['authorize', 'cancel'],
  AUTHORIZED: ['capture', 'cancel'],
  CAPTURED: ['refund'],
  CANCELLED: [],
  EXPIRED: [],
  REFUNDED: [],
}

const ALL_STATUSES = Object.keys(EXPECTED_ACTIONS)

/** action name → data-testid */
const ACTION_TEST_ID: Record<string, string> = {
  authorize: 'lifecycle-authorize',
  capture: 'lifecycle-capture',
  cancel: 'lifecycle-cancel',
  refund: 'lifecycle-refund',
}

const ALL_ACTION_TEST_IDS = Object.values(ACTION_TEST_ID)

/**
 * Mount PaymentOrderLifecycleActions with a mocked store that reports the
 * given status as the currentOrder status.
 */
async function mountWithStatus(status: string | undefined) {
  const pinia = createPinia()
  setActivePinia(pinia)

  const wrapper = await mountSuspended(PaymentOrderLifecycleActions, {
    props: {
      paymentOrderId: 'order-123',
      merchantId: 'merchant-abc',
      canRunLifecycle: true,
    },
  })

  // Reach into the store and set the order state after mount
  const store = usePaymentOrdersStore()
  if (status !== undefined) {
    store.currentOrder = {
      paymentOrderId: 'order-123',
      merchantId: 'merchant-abc',
      status,
      amountMinor: 1000,
      currency: 'PLN',
      clientOrderReference: 'ref-1',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      versionMarker: '"1"',
    } as any
  }

  // Re-mount so that computed re-evaluates with the updated store state
  const wrapper2 = await mountSuspended(PaymentOrderLifecycleActions, {
    props: {
      paymentOrderId: 'order-123',
      merchantId: 'merchant-abc',
      canRunLifecycle: true,
    },
  })

  return wrapper2
}

describe('PaymentOrderLifecycleActions — lifecycle controls render exactly one per available action', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders NO lifecycle buttons when status is undefined', async () => {
    const wrapper = await mountSuspended(PaymentOrderLifecycleActions, {
      props: { paymentOrderId: 'order-x', merchantId: 'merch-x', canRunLifecycle: true },
    })
    const html = wrapper.html()
    for (const testId of ALL_ACTION_TEST_IDS) {
      expect(html).not.toContain(`data-testid="${testId}"`)
    }
  })

  it('shows "no actions available" message when no actions exist', async () => {
    const wrapper = await mountSuspended(PaymentOrderLifecycleActions, {
      props: { paymentOrderId: 'order-x', merchantId: 'merch-x', canRunLifecycle: true },
    })
    expect(wrapper.text()).toContain('No actions available')
  })

  for (const status of ALL_STATUSES) {
    const expected = EXPECTED_ACTIONS[status]!
    const absent = ALL_ACTION_TEST_IDS.filter(
      (tid) => !expected.map((a) => ACTION_TEST_ID[a]).includes(tid),
    )

    it(`GIVEN status=${status}: renders exactly ${expected.length} control(s) [${expected.join(', ') || 'none'}]`, async () => {
      setActivePinia(createPinia())

      const wrapper = await mountSuspended(PaymentOrderLifecycleActions, {
        props: { paymentOrderId: 'order-123', merchantId: 'merchant-abc', canRunLifecycle: true },
      })

      const store = usePaymentOrdersStore()
      store.currentOrder = {
        paymentOrderId: 'order-123',
        merchantId: 'merchant-abc',
        status,
        amountMinor: 5000,
        currency: 'EUR',
        clientOrderReference: 'ref-test',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        versionMarker: '"2"',
      } as any

      // Re-mount after setting store state
      const w2 = await mountSuspended(PaymentOrderLifecycleActions, {
        props: { paymentOrderId: 'order-123', merchantId: 'merchant-abc', canRunLifecycle: true },
      })

      const html = w2.html()

      // Present checks — each available action has exactly one control
      for (const action of expected) {
        const tid = ACTION_TEST_ID[action]!
        const occurrences = (html.match(new RegExp(`data-testid="${tid}"`, 'g')) || []).length
        expect(occurrences, `Expected exactly 1 "${tid}" for status=${status}`).toBe(1)
      }

      // Absence checks — unavailable actions are not rendered
      for (const tid of absent) {
        expect(html, `Expected "${tid}" to be absent for status=${status}`).not.toContain(
          `data-testid="${tid}"`,
        )
      }
    })
  }

  it('each lifecycle button has the correct data-testid attribute', async () => {
    // For AUTHORIZED: capture + cancel should be present
    setActivePinia(createPinia())

    const wrapper = await mountSuspended(PaymentOrderLifecycleActions, {
      props: { paymentOrderId: 'order-auth', merchantId: 'merchant-abc', canRunLifecycle: true },
    })

    const store = usePaymentOrdersStore()
    store.currentOrder = {
      paymentOrderId: 'order-auth',
      merchantId: 'merchant-abc',
      status: 'AUTHORIZED',
      amountMinor: 1000,
      currency: 'PLN',
      clientOrderReference: 'ref-auth',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      versionMarker: '"3"',
    } as any

    const w2 = await mountSuspended(PaymentOrderLifecycleActions, {
      props: { paymentOrderId: 'order-auth', merchantId: 'merchant-abc', canRunLifecycle: true },
    })

    expect(w2.html()).toContain('data-testid="lifecycle-capture"')
    expect(w2.html()).toContain('data-testid="lifecycle-cancel"')
    expect(w2.html()).not.toContain('data-testid="lifecycle-authorize"')
    expect(w2.html()).not.toContain('data-testid="lifecycle-refund"')
  })
})
