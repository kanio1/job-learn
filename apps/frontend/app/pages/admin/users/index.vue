<template>
  <UDashboardPanel id="users">
    <template #header>
      <UDashboardNavbar title="Users" :ui="{ right: 'gap-3' }">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>

        <template #right>
          <UTooltip v-if="showManagementActions" text="Refresh users">
            <UButton
              color="neutral"
              variant="ghost"
              square
              icon="i-lucide-refresh-cw"
              aria-label="Refresh users"
              @click="loadUsers"
            />
          </UTooltip>

          <CreateUserForm
            v-if="showManagementActions"
            ref="createForm"
            :is-platform-admin="isPlatformAdmin"
            @created="handleCreated"
          />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <UAlert
        v-if="forbidden"
        color="warning"
        variant="subtle"
        icon="i-lucide-shield-alert"
        title="You don't have access to manage users"
        description="User management is available only to platform and tenant administrators."
        role="alert"
        data-testid="forbidden-state"
      />

      <template v-else>
        <div class="flex flex-wrap items-end gap-3">
          <UFormField label="Search users" class="min-w-64 flex-1">
            <UInput
              v-model="search"
              icon="i-lucide-search"
              placeholder="Username or email"
              aria-label="Search users"
              class="w-full"
              @keyup.enter="applyFilters"
            />
          </UFormField>

          <UFormField label="Role">
            <USelect
              v-model="roleFilter"
              :items="roleFilterItems"
              value-key="value"
              label-key="label"
              aria-label="Filter by role"
              class="min-w-48"
            />
          </UFormField>

          <UFormField label="Status">
            <USelect
              v-model="statusFilter"
              :items="statusFilterItems"
              value-key="value"
              label-key="label"
              aria-label="Filter by status"
              class="min-w-36"
            />
          </UFormField>

          <UButton icon="i-lucide-filter" variant="soft" @click="applyFilters">
            Apply filters
          </UButton>
          <UButton v-if="hasActiveFilters" color="neutral" variant="ghost" @click="clearFilters">
            Clear filters
          </UButton>
        </div>

        <UAlert
          v-if="pageConflict"
          color="warning"
          variant="subtle"
          icon="i-lucide-triangle-alert"
          title="Username or email already exists"
          :description="pageConflict"
          role="alert"
          data-testid="conflict-state"
        />

        <LoadingState v-if="loading" message="Loading users…" />

        <ErrorState
          v-else-if="loadError"
          :problem="loadProblem"
          :message="loadError"
          :on-retry="loadUsers"
        />

        <EmptyStateCard
          v-else-if="userList && userList.users.length === 0 && !hasActiveFilters"
          description="No users yet. Create the first managed user to begin."
          action-label="Create user"
          @action="createForm?.openModal()"
        />

        <div
          v-else-if="userList && userList.users.length === 0"
          data-testid="filtered-empty-state"
        >
          <EmptyStateCard
            :description="filteredEmptyDescription"
            action-label="Clear filters"
            @action="clearFilters"
          />
        </div>

        <template v-else-if="userList">
          <UserTable
            :users="userList.users"
            :can-assign-roles="canAssignRoles"
            @edit="openDrawer($event, 'details')"
            @roles="openDrawer($event, 'roles')"
            @toggle-enabled="toggleEnabled"
          />

          <div class="flex flex-wrap items-center justify-between gap-3 border-t border-default pt-4">
            <p class="text-sm text-muted">
              Page {{ userList.page + 1 }} · {{ userList.users.length }} user(s) shown
            </p>
            <UPagination
              v-if="userList.totalEstimate > userList.size"
              :default-page="userList.page + 1"
              :total="userList.totalEstimate"
              :page-count="userList.size"
              aria-label="Users pagination"
              @update:page="changePage"
            />
          </div>
        </template>
      </template>

      <EditUserDrawer
        v-model:open="drawerOpen"
        :user="selectedUser"
        :mode="drawerMode"
        @updated="handleUpdated"
      />
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
import CreateUserForm from '~/components/user/CreateUserForm.vue'
import EditUserDrawer from '~/components/user/EditUserDrawer.vue'
import EmptyStateCard from '~/components/shared/EmptyStateCard.vue'
import ErrorState from '~/components/shared/ErrorState.vue'
import LoadingState from '~/components/shared/LoadingState.vue'
import type { ProblemDetails } from '~/types/api'
import type { CompositeRole, UserDetail, UserList, UserSummary } from '~/schemas/user.schema'
import { COMPOSITE_ROLES } from '~/utils/rbacMatrix'

definePageMeta({
  layout: 'dashboard',
})

const route = useRoute()
const router = useRouter()
const toast = useToast()
const { can, hasRole } = useAuthorization()
const { listUsers, updateUser } = useUsersApi()

const createForm = ref<{ openModal: () => void } | null>(null)
const userList = ref<UserList | null>(null)
const loading = ref(false)
const forbiddenByApi = ref(false)
const loadError = ref<string | null>(null)
const loadProblem = ref<ProblemDetails | null>(null)
const pageConflict = ref<string | null>(null)

const search = ref(queryText(route.query.search))
const roleFilter = ref(queryRole(route.query.role))
const statusFilter = ref(queryStatus(route.query.status))
const currentPage = ref(queryPage(route.query.page))
const pageSize = 20

const selectedUser = ref<UserDetail | null>(null)
const drawerMode = ref<'details' | 'roles'>('details')
const drawerOpen = ref(false)

const canManageUsers = computed(() => can.value.canManageUsers)
const canAssignRoles = computed(() => can.value.canAssignRoles)
const isPlatformAdmin = computed(() => hasRole('PLATFORM_ADMIN'))
const forbidden = computed(() => !canManageUsers.value || forbiddenByApi.value)
const showManagementActions = computed(() => canManageUsers.value && !forbiddenByApi.value)
const hasActiveFilters = computed(() => Boolean(
  search.value.trim() || roleFilter.value !== 'all' || statusFilter.value !== 'all'
))

const roleFilterItems = [
  { label: 'All roles', value: 'all' },
  ...COMPOSITE_ROLES.map(role => ({ label: role.replaceAll('_', ' '), value: role })),
]

const statusFilterItems = [
  { label: 'All statuses', value: 'all' },
  { label: 'Enabled', value: 'enabled' },
  { label: 'Disabled', value: 'disabled' },
]

const filteredEmptyDescription = computed(() => {
  const active = [
    search.value.trim() ? `search “${search.value.trim()}”` : '',
    roleFilter.value !== 'all' ? `role ${roleFilter.value.replaceAll('_', ' ')}` : '',
    statusFilter.value !== 'all' ? `status ${statusFilter.value}` : '',
  ].filter(Boolean).join(', ')
  return `No users match the active filters: ${active}.`
})

async function loadUsers() {
  if (!canManageUsers.value) {
    loading.value = false
    return
  }

  loading.value = true
  forbiddenByApi.value = false
  loadError.value = null
  loadProblem.value = null

  const response = await listUsers({
    search: search.value.trim() || undefined,
    role: roleFilter.value === 'all' ? undefined : roleFilter.value,
    status: statusFilter.value === 'all' ? undefined : statusFilter.value,
    page: currentPage.value,
    size: pageSize,
  })

  if (response.status === 403) {
    forbiddenByApi.value = true
    userList.value = null
  } else if (response.data) {
    userList.value = response.data
  } else {
    userList.value = null
    loadProblem.value = response.problem
    loadError.value = response.problem?.detail || response.problem?.title || 'Failed to load users.'
  }
  loading.value = false
}

async function applyFilters() {
  currentPage.value = 0
  await syncRouteAndLoad()
}

async function clearFilters() {
  search.value = ''
  roleFilter.value = 'all'
  statusFilter.value = 'all'
  currentPage.value = 0
  await syncRouteAndLoad()
}

async function changePage(page: number) {
  currentPage.value = page - 1
  await syncRouteAndLoad()
}

async function syncRouteAndLoad() {
  await router.replace({
    query: {
      ...(search.value.trim() ? { search: search.value.trim() } : {}),
      ...(roleFilter.value !== 'all' ? { role: roleFilter.value } : {}),
      ...(statusFilter.value !== 'all' ? { status: statusFilter.value } : {}),
      page: String(currentPage.value),
      size: String(pageSize),
    },
  })
  await loadUsers()
}

function openDrawer(user: UserSummary, mode: 'details' | 'roles') {
  selectedUser.value = user
  drawerMode.value = mode
  drawerOpen.value = true
}

async function toggleEnabled(user: UserSummary) {
  pageConflict.value = null
  const response = await updateUser(user.id, { enabled: !user.enabled })
  if (response.data) {
    toast.add({
      title: response.data.enabled ? 'User enabled' : 'User disabled',
      description: response.data.username,
      color: 'success',
    })
    await loadUsers()
  } else if (response.status === 409) {
    pageConflict.value = response.problem?.detail || 'Username or email already exists.'
  } else {
    toast.add({
      title: 'User update failed',
      description: response.problem?.detail || response.problem?.title || 'Try again.',
      color: 'error',
    })
  }
}

async function handleCreated() {
  await loadUsers()
}

async function handleUpdated() {
  await loadUsers()
}

function queryText(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function queryRole(value: unknown): CompositeRole | 'all' {
  return typeof value === 'string' && COMPOSITE_ROLES.includes(value as CompositeRole)
    ? value as CompositeRole
    : 'all'
}

function queryStatus(value: unknown): 'enabled' | 'disabled' | 'all' {
  return value === 'enabled' || value === 'disabled' ? value : 'all'
}

function queryPage(value: unknown): number {
  const parsed = typeof value === 'string' ? Number.parseInt(value, 10) : 0
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0
}

watch(canManageUsers, (allowed) => {
  if (allowed) loadUsers()
}, { immediate: true })
</script>
