<template>
  <UDashboardPanel id="merchants">
    <template #header>
      <UDashboardNavbar :title="$t('merchants.title')" :ui="{ right: 'gap-3' }">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>

        <template #right>
          <AppLocaleSelect />
          <UTooltip text="Refresh merchants">
            <UButton
              v-if="!insufficientAuthority"
              color="neutral"
              variant="ghost"
              square
              icon="i-lucide-refresh-cw"
              aria-label="Refresh merchants"
              @click="loadMerchants()"
            />
          </UTooltip>

          <UButton
            v-if="!insufficientAuthority && canCreateMerchant"
            icon="i-lucide-plus"
            size="md"
            class="rounded-full"
            aria-label="Create merchant"
            data-testid="action-create-merchant"
            @click="showCreateModal = true"
          />
          <UButton
            v-if="!insufficientAuthority && canCreateMerchant"
            icon="i-lucide-upload"
            color="neutral"
            variant="soft"
            aria-label="Import CSV"
            data-testid="action-import-merchants"
            @click="showImportModal = true"
          >
            Import CSV
          </UButton>
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <div
        v-if="!insufficientAuthority && !loadError"
        class="flex flex-wrap items-end gap-1.5"
      >
        <UInput
          v-model="search"
          class="max-w-sm"
          icon="i-lucide-search"
          placeholder="Filter merchants..."
          aria-label="Filter merchants"
        />

        <USelect
          v-model="statusFilter"
          :items="statusItems"
          :ui="{ trailingIcon: 'group-data-[state=open]:rotate-180 transition-transform duration-200' }"
          placeholder="Filter status"
          class="min-w-32"
          aria-label="Filter status"
        />

        <USelect
          v-if="canFilterByTenant"
          v-model="tenantFilter"
          :items="tenantItems"
          class="min-w-40"
          aria-label="Filter tenant"
        />

        <UButton
          data-testid="merchant-filter-apply"
          icon="i-lucide-filter"
          variant="soft"
          label="Apply filters"
          @click="applyFilters"
        />

        <UButton
          v-if="canUpdateMerchantStatus && selectedMerchants.length > 0"
          data-testid="merchant-bulk-activate"
          color="primary"
          variant="soft"
          label="Activate selected"
          :loading="bulkActivating"
          @click="handleBulkActivate"
        />

        <UButton
          label="Display"
          color="neutral"
          variant="outline"
          trailing-icon="i-lucide-settings-2"
          disabled
        />
      </div>

      <UAlert
        v-if="insufficientAuthority"
        color="warning"
        variant="subtle"
        icon="i-lucide-shield-alert"
        title="You do not have permission to view merchants"
        description="Authenticated users without merchant read authority cannot view registry data or controls."
        role="alert"
      />

      <ErrorState
        v-else-if="loadError"
        :problem="loadProblem"
        :message="loadError"
        :on-retry="loadMerchants"
      />

      <template v-else>
        <div class="flex items-start gap-4">
          <OrgTree :tree-slug="treeSlug" class="shrink-0" />
          <div class="min-w-0 flex-1 space-y-4">
        <ErrorState
          v-if="actionProblem"
          :problem="actionProblem"
          retry-label="Reload"
          :on-retry="reloadAfterConflict"
        />
        <LoadingState v-if="loading" message="Loading merchants…" />

        <div v-if="list && list.content.length === 0 && !hasActiveFilters" data-testid="empty-state">
          <UCard :ui="{ body: 'py-16 sm:py-20' }">
            <UEmpty
              icon="i-lucide-store"
              title="Registry is empty"
              description="No merchants have been registered yet. Create the first merchant to start the activation workflow."
            >
              <template v-if="canCreateMerchant" #actions>
                <UButton icon="i-lucide-plus" @click="showCreateModal = true">
                  Create your first merchant
                </UButton>
              </template>
            </UEmpty>
          </UCard>
        </div>

        <EmptyStateCard
          v-else-if="list && list.content.length === 0"
          description="No merchants match the current filters."
        />

        <template v-else-if="list">
          <MerchantTable
            v-model:sorting="sorting"
            v-model:row-selection="rowSelection"
            :merchants="list.content"
            :loading="loading"
            :show-tenant-column="canFilterByTenant"
            @update:sorting="onSortingChange"
            @activate="handleActivate"
            @suspend="handleSuspend"
            @open360="open360"
            @save-name="handleSaveName"
          />

          <div class="flex items-center justify-between gap-3 border-t border-default pt-4 mt-auto">
            <div data-testid="merchant-registry-caption" class="text-sm text-muted">
              {{ list.totalElements }} merchant(s)
            </div>
            <UPagination
              v-if="list.totalPages > 1"
              :page="(list.page ?? 0) + 1"
              :total="list.totalElements"
              :page-count="list.size"
              aria-label="Merchant registry pagination"
              @update:page="onPageChange"
            />
          </div>
        </template>
          </div>
        </div>
      </template>

      <UModal v-model:open="showCreateModal" title="Create merchant">
        <template #content>
          <div class="p-6">
            <div class="mb-6">
              <p class="text-lg font-semibold text-highlighted">Create merchant</p>
              <p class="mt-1 text-sm text-muted">
                Register a merchant in DRAFT before activating it for payment-readiness testing.
              </p>
            </div>
            <CreateMerchantForm
              :key="formKey"
              :error="createError"
              :submitting="submittingCreate"
              @submit="handleCreate"
              @cancel="closeCreateModal"
            />
          </div>
        </template>
      </UModal>

      <MerchantSlideover
        v-model:open="slideoverOpen"
        :merchant-id="slideoverMerchantId"
        @updated="updateMerchantInList"
      />

      <MerchantImportModal
        v-model:open="showImportModal"
        @committed="loadMerchants"
      />
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
/**
 * Merchants list page — server query (q, status, tenantId, page, sort) in the URL.
 * Apply and sort headers each trigger GET /api/merchants. No client-side list filter.
 */

import type { CreateMerchantForm as CreateMerchantFormData, MerchantListQuery } from '~/schemas/merchant.schema'
import { merchantListQuerySchema } from '~/schemas/merchant.schema'
import type { MerchantListResponse, MerchantResponse } from '~/composables/useMerchantsApi'
import type { MerchantTableRowSelection, MerchantTableSorting } from '~/components/merchant/MerchantTable.vue'
import type { ProblemDetails } from '~/types/api'
import { merchantIfMatch } from '~/utils/merchant-etag'
import CreateMerchantForm from '~/components/merchant/CreateMerchantForm.vue'

definePageMeta({
  layout: 'dashboard'
})

const route = useRoute()
const router = useRouter()
const toast = useToast()
const { listMerchants, createMerchant, activateMerchant, suspendMerchant, patchMerchantDisplayName } = useMerchantsApi()
const { can } = useAuthorization()
const { user } = useUserSession()

const list = ref<MerchantListResponse | null>(null)
const loading = ref(true)
const loadError = ref<string | null>(null)
const loadProblem = ref<ProblemDetails | null>(null)
const actionProblem = ref<ProblemDetails | null>(null)
const insufficientAuthority = ref(false)

const showCreateModal = ref(false)
const showImportModal = ref(false)
const createError = ref<string | null>(null)
const submittingCreate = ref(false)
const formKey = ref(0)
const slideoverOpen = ref(false)
const slideoverMerchantId = ref<string | null>(null)

const search = ref('')
const statusFilter = ref('all')
const tenantFilter = ref('all')
const currentQuery = ref<MerchantListQuery>(merchantQueryFromRoute())
const treeSlug = computed(() => singleQueryValue(route.query.tree))
const sorting = ref<MerchantTableSorting>(sortingFromSort(currentQuery.value.sort))
const rowSelection = ref<MerchantTableRowSelection>({})
const bulkActivating = ref(false)

const statusItems = [
  { label: 'All', value: 'all' },
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Suspended', value: 'SUSPENDED' },
]

const tenantItems = [
  { label: 'All tenants', value: 'all' },
  { label: 'TENANT_ALPHA', value: 'TENANT_ALPHA' },
  { label: 'PLATFORM_TENANT', value: 'PLATFORM_TENANT' },
  { label: 'PLACEHOLDER_TENANT_ID', value: 'PLACEHOLDER_TENANT_ID' },
]

const canCreateMerchant = computed(() => can.value.canCreateMerchant)
const canUpdateMerchantStatus = computed(() => can.value.canUpdateMerchantStatus)
const canFilterByTenant = computed(() => user.value?.tenantId === 'PLATFORM_TENANT')
const hasActiveFilters = computed(() => Boolean(
  currentQuery.value.q
  || currentQuery.value.status
  || currentQuery.value.tenantId,
))
const selectedMerchants = computed(() => {
  if (!list.value) {
    return []
  }
  return list.value.content.filter((merchant, index) =>
    rowSelection.value[merchant.merchantId] || rowSelection.value[String(index)],
  )
})

function onSortingChange(next: MerchantTableSorting) {
  const sort = sortFromSorting(next)
  if (sort === currentQuery.value.sort) {
    return
  }
  currentQuery.value = { ...currentQuery.value, sort, page: 0 }
  void syncQueryToUrlAndLoad()
}

function merchantQueryFromRoute(): MerchantListQuery {
  const rawQuery = {
    q: singleQueryValue(route.query.q),
    status: singleQueryValue(route.query.status),
    tenantId: singleQueryValue(route.query.tenantId),
    page: numberQueryValue(route.query.page) ?? 0,
    size: numberQueryValue(route.query.size) ?? 20,
    sort: singleQueryValue(route.query.sort) ?? 'createdAt,desc',
  }
  const parsed = merchantListQuerySchema.safeParse(rawQuery)
  if (parsed.success) {
    return parsed.data
  }
  return {
    page: 0,
    size: 20,
    sort: 'createdAt,desc',
  }
}

function hydrateFiltersFromQuery(query: MerchantListQuery) {
  search.value = query.q ?? ''
  statusFilter.value = query.status ?? 'all'
  tenantFilter.value = query.tenantId ?? 'all'
  sorting.value = sortingFromSort(query.sort)
}

function sortingFromSort(sort: string): MerchantTableSorting {
  const [id, direction] = sort.split(',')
  if (id !== 'createdAt' && id !== 'updatedAt' && id !== 'displayName' && id !== 'status') {
    return [{ id: 'createdAt', desc: true }]
  }
  return [{ id, desc: direction !== 'asc' }]
}

function sortFromSorting(next: MerchantTableSorting): MerchantListQuery['sort'] {
  const first = next[0]
  if (!first || (first.id !== 'createdAt' && first.id !== 'updatedAt' && first.id !== 'displayName' && first.id !== 'status')) {
    return 'createdAt,desc'
  }
  return `${first.id},${first.desc ? 'desc' : 'asc'}`
}

function queryFromToolbar(page: number): MerchantListQuery {
  return merchantListQuerySchema.parse({
    q: search.value.trim() || undefined,
    status: statusFilter.value === 'all' ? undefined : statusFilter.value,
    tenantId: canFilterByTenant.value && tenantFilter.value !== 'all' ? tenantFilter.value : undefined,
    page,
    size: currentQuery.value.size,
    sort: currentQuery.value.sort,
  })
}

function applyFilters() {
  currentQuery.value = queryFromToolbar(0)
  void syncQueryToUrlAndLoad()
}

function onPageChange(page: number) {
  currentQuery.value = { ...currentQuery.value, page: page - 1 }
  void syncQueryToUrlAndLoad()
}

async function loadMerchants() {
  loading.value = true
  loadError.value = null
  loadProblem.value = null
  actionProblem.value = null
  insufficientAuthority.value = false
  await nextTick()

  const response = await listMerchants({
    q: currentQuery.value.q,
    status: currentQuery.value.status,
    tenantId: currentQuery.value.tenantId,
    page: currentQuery.value.page,
    size: currentQuery.value.size,
    sort: currentQuery.value.sort,
  })

  if (response.status === 403) {
    insufficientAuthority.value = true
    list.value = null
  } else if (response.data) {
    list.value = response.data
  } else {
    list.value = null
    loadProblem.value = response.problem
    loadError.value =
      response.problem?.detail ||
      response.problem?.title ||
      'Failed to load merchants. Please try again.'
  }

  loading.value = false
}

async function syncQueryToUrlAndLoad() {
  const query = currentQuery.value
  const nextQuery: Record<string, string> = {}
  if (query.q) nextQuery.q = query.q
  if (query.status) nextQuery.status = query.status
  if (query.tenantId) nextQuery.tenantId = query.tenantId
  if (query.page && query.page > 0) nextQuery.page = String(query.page)
  if (query.size && query.size !== 20) nextQuery.size = String(query.size)
  if (query.sort && query.sort !== 'createdAt,desc') nextQuery.sort = query.sort
  if (slideoverMerchantId.value) nextQuery.merchantId = slideoverMerchantId.value
  const tree = singleQueryValue(route.query.tree)
  if (tree) nextQuery.tree = tree
  await router.replace({ query: nextQuery })
  await loadMerchants()
}

function singleQueryValue(value: unknown): string | undefined {
  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : undefined
  }
  if (value === null || value === undefined || value === '') {
    return undefined
  }
  return String(value)
}

function numberQueryValue(value: unknown): number | undefined {
  const raw = singleQueryValue(value)
  if (!raw) {
    return undefined
  }
  const parsed = Number(raw)
  return Number.isInteger(parsed) ? parsed : undefined
}

async function handleCreate(form: CreateMerchantFormData) {
  createError.value = null
  submittingCreate.value = true

  const response = await createMerchant(form)

  if (response.data) {
    closeCreateModal()
    toast.add({ title: 'Merchant created', color: 'primary' })
    await loadMerchants()
  } else {
    createError.value =
      response.problem?.detail ||
      response.problem?.title ||
      'Failed to create merchant. Please try again.'
  }

  submittingCreate.value = false
}

function closeCreateModal() {
  showCreateModal.value = false
  createError.value = null
  formKey.value += 1
}

async function reloadAfterConflict() {
  actionProblem.value = null
  await loadMerchants()
}

async function handleSaveName(merchant: MerchantResponse, displayName: string) {
  const response = await patchMerchantDisplayName(
    merchant.merchantId,
    displayName,
    merchantIfMatch(undefined, merchant.version),
  )
  if (response.data) {
    updateMerchantInList(response.data)
    toast.add({ title: `${merchant.merchantReference} renamed`, color: 'primary' })
  } else if (response.status === 412) {
    actionProblem.value = response.problem
  } else {
    toast.add({
      title: 'Rename failed',
      description:
        response.problem?.detail ||
        response.problem?.title ||
        'Failed to update merchant name.',
      color: 'error',
    })
  }
}

async function handleActivate(merchant: MerchantResponse) {
  const response = await activateMerchant(
    merchant.merchantId,
    merchantIfMatch(undefined, merchant.version),
  )

  if (response.data) {
    updateMerchantInList(response.data)
    toast.add({ title: `${merchant.merchantReference} activated`, color: 'primary' })
  } else if (response.status === 412) {
    actionProblem.value = response.problem
  } else {
    toast.add({
      title: 'Activation failed',
      description:
        response.problem?.detail ||
        response.problem?.title ||
        'Failed to activate merchant.',
      color: 'error',
    })
  }
}

async function handleBulkActivate() {
  const selected = selectedMerchants.value
  if (selected.length === 0) {
    return
  }
  bulkActivating.value = true
  try {
    for (const merchant of selected) {
      await handleActivate(merchant)
    }
    rowSelection.value = {}
  } finally {
    bulkActivating.value = false
  }
}

async function handleSuspend(merchant: MerchantResponse) {
  const response = await suspendMerchant(
    merchant.merchantId,
    merchantIfMatch(undefined, merchant.version),
  )

  if (response.data) {
    updateMerchantInList(response.data)
    toast.add({ title: `${merchant.merchantReference} suspended`, color: 'warning' })
  } else if (response.status === 412) {
    actionProblem.value = response.problem
  } else {
    toast.add({
      title: 'Suspension failed',
      description:
        response.problem?.detail ||
        response.problem?.title ||
        'Failed to suspend merchant.',
      color: 'error',
    })
  }
}

function open360(merchant: MerchantResponse) {
  slideoverMerchantId.value = merchant.merchantId
  slideoverOpen.value = true
}

watch(slideoverOpen, (isOpen) => {
  const nextQuery = { ...route.query }
  if (isOpen && slideoverMerchantId.value) {
    nextQuery.merchantId = slideoverMerchantId.value
  } else {
    delete nextQuery.merchantId
  }
  void router.replace({ query: nextQuery })
})

watch(() => singleQueryValue(route.query.merchantId), (deepLink) => {
  if (!deepLink) {
    return
  }
  slideoverMerchantId.value = deepLink
  slideoverOpen.value = true
})

function updateMerchantInList(updated: MerchantResponse) {
  if (!list.value) {
    return
  }
  const idx = list.value.content.findIndex(m => m.merchantId === updated.merchantId)
  if (idx === -1) {
    return
  }
  const content = [
    ...list.value.content.slice(0, idx),
    updated,
    ...list.value.content.slice(idx + 1),
  ]
  list.value = { ...list.value, content }
}

onMounted(() => {
  currentQuery.value = merchantQueryFromRoute()
  hydrateFiltersFromQuery(currentQuery.value)
  const deepLink = singleQueryValue(route.query.merchantId)
  if (deepLink) {
    slideoverMerchantId.value = deepLink
    slideoverOpen.value = true
  }
  void loadMerchants()
})
</script>
