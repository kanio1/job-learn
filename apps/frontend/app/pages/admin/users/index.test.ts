// @vitest-environment nuxt
import { flushPromises } from '@vue/test-utils'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import UsersPage from './index.vue'

const mocks = vi.hoisted(() => ({
  canManageUsers: true,
  canAssignRoles: true,
  listUsers: vi.fn(),
  updateUser: vi.fn(),
  toastAdd: vi.fn(),
}))

vi.mock('~/composables/useAuthorization', () => ({
  useAuthorization: () => ({
  can: {
    value: {
      canManageUsers: mocks.canManageUsers,
      canAssignRoles: mocks.canAssignRoles,
    },
  },
  hasRole: (role: string) => role === 'PLATFORM_ADMIN' && mocks.canManageUsers,
  }),
}))

vi.mock('~/composables/useUsersApi', () => ({
  useUsersApi: () => ({
    listUsers: mocks.listUsers,
    updateUser: mocks.updateUser,
  }),
}))

vi.mock('@nuxt/ui/composables/useToast', () => ({
  useToast: () => ({ add: mocks.toastAdd }),
}))

const CreateUserFormStub = {
  name: 'CreateUserForm',
  template: '<button type="button" aria-label="Create user">Create user</button>',
  methods: { openModal: vi.fn() },
}

const EditUserDrawerStub = {
  name: 'EditUserDrawer',
  props: ['open', 'user', 'mode'],
  template: '<div />',
}

const UserTableStub = {
  name: 'UserTable',
  props: ['users', 'canAssignRoles'],
  emits: ['edit', 'roles', 'toggleEnabled'],
  template: `
    <div data-testid="users-table">
      <span>{{ users[0]?.username }}</span>
      <button
        v-if="users[0]"
        type="button"
        :aria-label="users[0].enabled ? 'Disable ' + users[0].username : 'Enable ' + users[0].username"
        @click="$emit('toggleEnabled', users[0])"
      >
        {{ users[0].enabled ? 'Disable' : 'Enable' }}
      </button>
    </div>
  `,
}

const SAFE_USER = {
  id: 'user-101',
  username: 'alice.admin',
  email: 'alice@example.test',
  enabled: true,
  tenantId: 'TENANT_ALPHA',
  merchantId: null,
  roles: ['TENANT_ADMIN'],
} as const

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

function listResponse(users: unknown[]) {
  return response({
    data: { users, page: 0, size: 20, totalEstimate: users.length },
  })
}

async function mountPage(route = '/admin/users') {
  return mountSuspended(UsersPage, {
    route,
    global: {
      stubs: {
        CreateUserForm: CreateUserFormStub,
        EditUserDrawer: EditUserDrawerStub,
        UserTable: UserTableStub,
        UTooltip: { template: '<div><slot /></div>' },
        UFormField: { template: '<label><slot /></label>' },
        UInput: {
          name: 'UInput',
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', ($event.target).value)">',
        },
        USelect: true,
      },
    },
  })
}

function expectNoConfidentialData(html: string) {
  for (const marker of [
    'Authorization',
    'Bearer',
    'access_token',
    'refresh_token',
    'client_secret',
    'temporaryPassword',
    'admin_token',
    'credentials',
  ]) {
    expect(html).not.toContain(marker)
  }
}

describe('/admin/users UI states', () => {
  beforeEach(() => {
    mocks.canManageUsers = true
    mocks.canAssignRoles = true
    mocks.listUsers.mockReset()
    mocks.updateUser.mockReset()
    mocks.toastAdd.mockReset()
  })

  it('shows the loading state while the users request is pending', async () => {
    let resolveList!: (value: ReturnType<typeof listResponse>) => void
    mocks.listUsers.mockReturnValue(new Promise(resolve => { resolveList = resolve }))

    const wrapper = await mountPage()

    expect(wrapper.text()).toContain('Loading users')
    expect(wrapper.find('[data-testid="loading-state"]').exists()).toBe(true)
    expectNoConfidentialData(wrapper.html())

    resolveList(listResponse([]))
    await flushPromises()
  })

  it('shows the unfiltered empty state when no managed users exist', async () => {
    mocks.listUsers.mockResolvedValue(listResponse([]))

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('No users yet')
    expect(wrapper.text()).toContain('Create user')
    expectNoConfidentialData(wrapper.html())
  })

  it('shows a distinct filtered-empty state when search is active', async () => {
    mocks.listUsers.mockResolvedValue(listResponse([]))

    const wrapper = await mountPage()
    await flushPromises()
    await wrapper.get('input').setValue('missing-user')
    await wrapper.vm.$nextTick()
    await flushPromises()

    expect(wrapper.find('[data-testid="filtered-empty-state"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('No users match the active filters')
    expect(wrapper.text()).toContain('missing-user')
    expectNoConfidentialData(wrapper.html())
  })

  it('shows safe problem details when loading users fails', async () => {
    mocks.listUsers.mockResolvedValue(response({
      status: 502,
      problem: {
        type: 'https://api.payment-quality.local/problems/bad-gateway',
        title: 'Bad Gateway',
        status: 502,
        detail: 'Identity administration is temporarily unavailable.',
        instance: '/api/users',
      },
    }))

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.find('[data-testid="error-state"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="problem-details-card"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Identity administration is temporarily unavailable.')
    expectNoConfidentialData(wrapper.html())
  })

  it('shows forbidden and never calls the users API for a non-admin role', async () => {
    mocks.canManageUsers = false
    mocks.canAssignRoles = false

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.find('[data-testid="forbidden-state"]').exists()).toBe(true)
    expect(wrapper.text()).toContain("You don't have access to manage users")
    expect(wrapper.find('button[aria-label="Create user"]').exists()).toBe(false)
    expect(mocks.listUsers).not.toHaveBeenCalled()
    expectNoConfidentialData(wrapper.html())
  })

  it('emits a success toast after an accessible enable/disable action succeeds', async () => {
    mocks.listUsers.mockResolvedValue(listResponse([SAFE_USER]))
    mocks.updateUser.mockResolvedValue(response({
      data: { ...SAFE_USER, enabled: false },
    }))

    const wrapper = await mountPage()
    await flushPromises()

    const disableButton = wrapper.find('button[aria-label="Disable alice.admin"]')
    expect(disableButton.exists()).toBe(true)
    await disableButton.trigger('click')
    await flushPromises()

    expect(mocks.toastAdd).toHaveBeenCalledWith(expect.objectContaining({
      title: 'User disabled',
      description: 'alice.admin',
      color: 'success',
    }))
    expectNoConfidentialData(wrapper.html())
  })

  it('shows a clear, safe conflict state for a duplicate user update', async () => {
    mocks.listUsers.mockResolvedValue(listResponse([SAFE_USER]))
    mocks.updateUser.mockResolvedValue(response({
      status: 409,
      problem: {
        title: 'Conflict',
        status: 409,
        detail: 'Username or email already exists.',
      },
    }))

    const wrapper = await mountPage()
    await flushPromises()
    await wrapper.find('button[aria-label="Disable alice.admin"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="conflict-state"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Username or email already exists')
    expectNoConfidentialData(wrapper.html())
  })
})
