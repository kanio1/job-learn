// @vitest-environment nuxt
import { flushPromises } from '@vue/test-utils'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuditPage from './index.vue'

const mocks = vi.hoisted(() => ({
  canViewAuditLog: true,
  listAudit: vi.fn(),
  getEntry: vi.fn(),
}))

vi.mock('~/composables/useAuthorization', () => ({
  useAuthorization: () => ({
    can: {
      value: {
        canViewAuditLog: mocks.canViewAuditLog,
      },
    },
  }),
}))

vi.mock('~/composables/useAuditApi', () => ({
  useAuditApi: () => ({
    list: mocks.listAudit,
    getEntry: mocks.getEntry,
  }),
}))

const AuditFiltersStub = {
  name: 'AuditFilters',
  template: '<div data-testid="audit-filters-stub" />',
}

const AuditTableStub = {
  name: 'AuditTable',
  template: '<div data-testid="audit-table" />',
}

const AuditEntryDrawerStub = {
  name: 'AuditEntryDrawer',
  props: ['open', 'entry'],
  template: '<div v-if="open && entry" data-testid="audit-entry-drawer" />',
}

function response(overrides: Record<string, unknown> = {}) {
  return {
    data: null,
    status: 200,
    headers: {},
    problem: null,
    raw: '',
    ...overrides,
  }
}

function emptyListResponse() {
  return response({
    data: {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    },
  })
}

async function mountPage() {
  return mountSuspended(AuditPage, {
    route: '/admin/audit',
    global: {
      stubs: {
        AuditFilters: AuditFiltersStub,
        AuditTable: AuditTableStub,
        AuditEntryDrawer: AuditEntryDrawerStub,
        UTooltip: { template: '<div><slot /></div>' },
      },
    },
  })
}

function expectOnlyState(wrapper: Awaited<ReturnType<typeof mountPage>>, expected: string) {
  const stateIds = [
    'audit-state-loading',
    'audit-state-empty',
    'audit-state-filtered-empty',
    'audit-state-error',
    'audit-state-forbidden',
    'audit-state-deep-link-not-found',
  ]
  const visibleStates = stateIds.filter(stateId => wrapper.find(`[data-testid="${stateId}"]`).exists())
  expect(visibleStates).toEqual([expected])
}

function expectNoConfidentialData(html: string) {
  for (const marker of [
    'access_token',
    'refresh_token',
    'Authorization',
    'Bearer',
    'client_secret',
    'password',
    'temporaryPassword',
    'PAN',
    'CVV',
    'payload',
    'requestBody',
    'responseBody',
    'actorSubject',
    'actor_subject',
  ]) {
    expect(html).not.toContain(marker)
  }
}

describe('/admin/audit read-only UI states', () => {
  beforeEach(() => {
    mocks.canViewAuditLog = true
    mocks.listAudit.mockReset()
    mocks.getEntry.mockReset()
  })

  it('renders only the loading state while the audit list request is pending', async () => {
    let resolveList!: (value: ReturnType<typeof emptyListResponse>) => void
    mocks.listAudit.mockReturnValue(new Promise(resolve => { resolveList = resolve }))

    const wrapper = await mountPage()

    expectOnlyState(wrapper, 'audit-state-loading')
    expectNoConfidentialData(wrapper.html())

    resolveList(emptyListResponse())
    await flushPromises()
  })

  it('renders the unfiltered empty state for an empty first page', async () => {
    mocks.listAudit.mockResolvedValue(emptyListResponse())

    const wrapper = await mountPage()
    await flushPromises()

    expectOnlyState(wrapper, 'audit-state-empty')
    expect(wrapper.text()).toContain('No audit events are available')
    expectNoConfidentialData(wrapper.html())
  })

  it('renders the filtered-empty state when a safe filter is active', async () => {
    mocks.listAudit.mockResolvedValue(emptyListResponse())

    const wrapper = await mountPage()
    await flushPromises()
    await wrapper.vm.$router.replace({ query: { actor: 'missing-actor' } })
    await flushPromises()

    expectOnlyState(wrapper, 'audit-state-filtered-empty')
    expect(wrapper.text()).toContain('No audit events match the active filters')
    expectNoConfidentialData(wrapper.html())
  })

  it('renders a safe error state for a failed list request', async () => {
    mocks.listAudit.mockResolvedValue(response({
      status: 502,
      problem: {
        type: 'about:blank',
        title: 'Audit service unavailable',
        status: 502,
        detail: 'Audit events cannot be loaded right now.',
      },
    }))

    const wrapper = await mountPage()
    await flushPromises()

    expectOnlyState(wrapper, 'audit-state-error')
    expect(wrapper.text()).toContain('Audit events cannot be loaded right now.')
    expectNoConfidentialData(wrapper.html())
  })

  it('renders forbidden without calling the API when frontend capability denies access', async () => {
    mocks.canViewAuditLog = false

    const wrapper = await mountPage()
    await flushPromises()

    expectOnlyState(wrapper, 'audit-state-forbidden')
    expect(mocks.listAudit).not.toHaveBeenCalled()
    expect(wrapper.find('[data-testid="audit-table"]').exists()).toBe(false)
    expectNoConfidentialData(wrapper.html())
  })

  it('renders forbidden when the backend denies an otherwise capable session', async () => {
    mocks.listAudit.mockResolvedValue(response({
      status: 403,
      problem: {
        type: 'about:blank',
        title: 'Forbidden',
        status: 403,
        detail: 'Audit log access denied.',
      },
    }))

    const wrapper = await mountPage()
    await flushPromises()

    expectOnlyState(wrapper, 'audit-state-forbidden')
    expect(wrapper.text()).not.toContain('platform:audit:read')
    expect(wrapper.text()).not.toContain('tenant:audit:read')
    expect(wrapper.find('[data-testid="audit-table"]').exists()).toBe(false)
    expectNoConfidentialData(wrapper.html())
  })

  it('renders deep-link-not-found distinctly and leaves the drawer empty', async () => {
    mocks.listAudit.mockResolvedValue(emptyListResponse())
    mocks.getEntry.mockResolvedValue(null)

    const wrapper = await mountPage()
    await flushPromises()
    await wrapper.vm.$router.replace({
      query: { entry: '97bff4c0-6f43-4b83-bf44-09acbcf71a45' },
    })
    await flushPromises()

    expectOnlyState(wrapper, 'audit-state-deep-link-not-found')
    expect(mocks.getEntry).toHaveBeenCalledWith('97bff4c0-6f43-4b83-bf44-09acbcf71a45')
    expect(wrapper.find('[data-testid="audit-entry-drawer"]').exists()).toBe(false)
    expectNoConfidentialData(wrapper.html())
  })
})
