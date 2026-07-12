<template>
  <UDashboardPanel id="merchants">
    <template #header>
      <UDashboardNavbar title="Merchants" :ui="{ right: 'gap-3' }">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>

        <template #right>
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
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <!-- Filter toolbar — shown only when list is available -->
      <div
        v-if="!insufficientAuthority && !loadError && !loading"
        class="flex flex-wrap items-center justify-between gap-1.5"
      >
        <UInput
          v-model="search"
          class="max-w-sm"
          icon="i-lucide-search"
          placeholder="Filter merchants..."
          aria-label="Filter merchants"
        />

        <div class="flex flex-wrap items-center gap-1.5">
          <USelect
            v-model="statusFilter"
            :items="statusItems"
            :ui="{ trailingIcon: 'group-data-[state=open]:rotate-180 transition-transform duration-200' }"
            placeholder="Filter status"
            class="min-w-32"
            aria-label="Filter status"
          />

          <UButton
            label="Display"
            color="neutral"
            variant="outline"
            trailing-icon="i-lucide-settings-2"
            disabled
          />
        </div>
      </div>

      <!-- 403 / insufficient authority -->
      <UAlert
        v-if="insufficientAuthority"
        color="warning"
        variant="subtle"
        icon="i-lucide-shield-alert"
        title="You do not have permission to view merchants"
        description="Authenticated users without merchant read authority cannot view registry data or controls."
        role="alert"
      />

      <!-- Loading state while GET /api/merchants is in flight (Req 2.2) -->
      <LoadingState v-else-if="loading" message="Loading merchants…" />

      <!-- Error state when the request fails (Req 2.2, 2.9) -->
      <ErrorState
        v-else-if="loadError"
        :problem="loadProblem"
        :message="loadError"
        :on-retry="loadMerchants"
      />

      <!-- List content -->
      <template v-else>
        <!-- Empty state when no merchants exist (Req 2.3) -->
        <div v-if="filteredMerchants.length === 0 && merchants.length === 0" data-testid="empty-state">
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

        <!-- Filtered-results empty state -->
        <EmptyStateCard
          v-else-if="filteredMerchants.length === 0"
          description="No merchants match the current filters."
        />

        <template v-else>
          <MerchantTable
            :merchants="filteredMerchants"
            :loading="false"
            @activate="handleActivate"
            @suspend="handleSuspend"
          />

          <div class="flex items-center justify-between gap-3 border-t border-default pt-4 mt-auto">
            <div class="text-sm text-muted">
              {{ filteredMerchants.length }} of {{ merchants.length }} merchant(s) shown.
            </div>
          </div>
        </template>
      </template>

      <!-- Create merchant modal -->
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
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
/**
 * Merchants list page.
 *
 * Uses `useMerchantsApi` for all merchant API calls (Req 2.1, 2.4, 2.6).
 * Displays LoadingState while in progress, EmptyStateCard on zero merchants,
 * ErrorState on failure (Req 2.2, 2.3, 2.9).
 * Shows updated merchant status from activate/suspend success responses (Req 2.6).
 */

import type { CreateMerchantForm as CreateMerchantFormData } from '~/schemas/merchant.schema'
import type { MerchantResponse } from '~/composables/useMerchantsApi'
import type { ProblemDetails } from '~/types/api'
import CreateMerchantForm from '~/components/merchant/CreateMerchantForm.vue'

definePageMeta({
  layout: 'dashboard'
})

const toast = useToast()
const { listMerchants, createMerchant, activateMerchant, suspendMerchant } = useMerchantsApi()
const { can } = useAuthorization()

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------

const merchants = ref<MerchantResponse[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)
const loadProblem = ref<ProblemDetails | null>(null)
const insufficientAuthority = ref(false)

const showCreateModal = ref(false)
const createError = ref<string | null>(null)
const submittingCreate = ref(false)
const formKey = ref(0)

const search = ref('')
const statusFilter = ref('all')

const statusItems = [
  { label: 'All', value: 'all' },
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Suspended', value: 'SUSPENDED' },
]

const canCreateMerchant = computed(() => can.value.canCreateMerchant)

// ---------------------------------------------------------------------------
// Computed
// ---------------------------------------------------------------------------

const filteredMerchants = computed(() => {
  const term = search.value.trim().toLowerCase()

  return merchants.value.filter((merchant) => {
    const matchesStatus =
      statusFilter.value === 'all' || merchant.status === statusFilter.value
    const matchesSearch =
      !term ||
      [merchant.merchantReference, merchant.displayName, merchant.status].some((v) =>
        v.toLowerCase().includes(term)
      )
    return matchesStatus && matchesSearch
  })
})

// ---------------------------------------------------------------------------
// Data loading (Req 2.1, 2.2)
// ---------------------------------------------------------------------------

async function loadMerchants() {
  loading.value = true
  loadError.value = null
  loadProblem.value = null
  insufficientAuthority.value = false

  const response = await listMerchants()

  if (response.status === 403) {
    insufficientAuthority.value = true
  } else if (response.data) {
    merchants.value = response.data.content
  } else {
    // Request failed — surface via ErrorState
    loadProblem.value = response.problem
    loadError.value =
      response.problem?.detail ||
      response.problem?.title ||
      'Failed to load merchants. Please try again.'
  }

  loading.value = false
}

// ---------------------------------------------------------------------------
// Create merchant (Req 2.4, 2.5, 2.9)
// ---------------------------------------------------------------------------

async function handleCreate(form: CreateMerchantFormData) {
  createError.value = null
  submittingCreate.value = true

  const response = await createMerchant(form)

  if (response.data) {
    closeCreateModal()
    toast.add({ title: 'Merchant created', color: 'primary' })
    await loadMerchants()
  } else {
    // Retain form input on error — form key is NOT incremented (Req 2.9)
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

// ---------------------------------------------------------------------------
// Activate / Suspend (Req 2.6, 2.9)
// ---------------------------------------------------------------------------

async function handleActivate(merchant: MerchantResponse) {
  const response = await activateMerchant(merchant.merchantId)

  if (response.data) {
    // Update local state with the status from the response (Req 2.6)
    updateMerchantInList(response.data)
    toast.add({ title: `${merchant.merchantReference} activated`, color: 'primary' })
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

async function handleSuspend(merchant: MerchantResponse) {
  const response = await suspendMerchant(merchant.merchantId)

  if (response.data) {
    // Update local state with the status from the response (Req 2.6)
    updateMerchantInList(response.data)
    toast.add({ title: `${merchant.merchantReference} suspended`, color: 'warning' })
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

/** Replace the matching entry in the local list with the updated merchant. */
function updateMerchantInList(updated: MerchantResponse) {
  const idx = merchants.value.findIndex((m) => m.merchantId === updated.merchantId)
  if (idx !== -1) {
    merchants.value = [
      ...merchants.value.slice(0, idx),
      updated,
      ...merchants.value.slice(idx + 1),
    ]
  }
}

// ---------------------------------------------------------------------------
// Lifecycle
// ---------------------------------------------------------------------------

onMounted(() => {
  loadMerchants()
})
</script>
