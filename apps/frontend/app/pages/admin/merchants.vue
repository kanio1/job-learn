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
            v-if="!insufficientAuthority"
            icon="i-lucide-plus"
            size="md"
            class="rounded-full"
            aria-label="Create merchant"
            @click="showCreateModal = true"
          />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <div v-if="!insufficientAuthority && !error" class="flex flex-wrap items-center justify-between gap-1.5">
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

      <UAlert
        v-if="insufficientAuthority"
        color="warning"
        variant="subtle"
        icon="i-lucide-shield-alert"
        title="You do not have permission to view merchants"
        description="Authenticated users without merchant read authority cannot view registry data or controls."
        role="alert"
      />

      <UAlert
        v-else-if="error"
        color="error"
        variant="subtle"
        icon="i-lucide-circle-alert"
        title="Merchant list unavailable"
        :description="error"
        role="alert"
      >
        <template #actions>
          <UButton
            size="sm"
            color="error"
            variant="soft"
            aria-label="Retry loading merchants"
            @click="loadMerchants()"
          >
            Retry
          </UButton>
        </template>
      </UAlert>

      <template v-else>
        <MerchantTable
          v-if="filteredMerchants.length > 0 || loading"
          :merchants="filteredMerchants"
          :loading="loading"
          @activate="activateMerchant"
          @suspend="suspendMerchant"
        />

        <UCard v-else :ui="{ body: 'py-16 sm:py-20' }">
          <UEmpty
            icon="i-lucide-store"
            title="Registry is empty"
            :description="emptyDescription"
          >
            <template #actions>
              <UButton icon="i-lucide-plus" @click="showCreateModal = true">
                Create your first merchant
              </UButton>
            </template>
          </UEmpty>
        </UCard>

        <div class="flex items-center justify-between gap-3 border-t border-default pt-4 mt-auto">
          <div class="text-sm text-muted">
            {{ filteredMerchants.length }} of {{ merchants.length }} merchant(s) shown.
          </div>
        </div>
      </template>

      <UModal v-model:open="showCreateModal" title="Create merchant">
        <template #content>
          <div class="p-6">
            <div class="mb-6">
              <p class="text-lg font-semibold text-highlighted">Create merchant</p>
              <p class="mt-1 text-sm text-muted">Register a merchant in DRAFT before activating it for payment-readiness testing.</p>
            </div>
            <CreateMerchantForm
              :key="formKey"
              :error="createError"
              :submitting="submittingCreate"
              @submit="createMerchant"
              @cancel="showCreateModal = false"
            />
          </div>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
import type { CreateMerchantForm as CreateMerchantFormData } from '~/schemas/merchant.schema'
import CreateMerchantForm from '~/components/merchant/CreateMerchantForm.vue'

definePageMeta({
  layout: 'dashboard'
})

interface Merchant {
  merchantId: string
  merchantReference: string
  displayName: string
  status: string
  createdAt: string
  updatedAt: string
}

const merchants = ref<Merchant[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
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
  { label: 'Suspended', value: 'SUSPENDED' }
]

const filteredMerchants = computed(() => {
  const term = search.value.trim().toLowerCase()

  return merchants.value.filter((merchant) => {
    const matchesStatus = statusFilter.value === 'all' || merchant.status === statusFilter.value
    const matchesSearch = !term || [merchant.merchantReference, merchant.displayName, merchant.status]
      .some(value => value.toLowerCase().includes(term))

    return matchesStatus && matchesSearch
  })
})

const emptyDescription = computed(() => {
  if (merchants.value.length === 0) {
    return 'No merchants have been registered yet. Create the first merchant to start the Phase 1 activation workflow.'
  }

  return 'No merchants match the current filters.'
})

async function loadMerchants() {
  loading.value = true
  error.value = null
  insufficientAuthority.value = false
  try {
    const data = await $fetch<{ merchants: Merchant[] }>('/api/merchants')
    merchants.value = data.merchants || []
  } catch (e: any) {
    if (e?.statusCode === 403) {
      merchants.value = []
      insufficientAuthority.value = true
    } else if (e?.statusCode === 503 || e?.data?.error === 'backend_unavailable') {
      error.value = 'Backend service is unavailable. Start the backend on http://localhost:8080 and retry.'
    } else {
      error.value = 'Failed to load merchants. Please try again.'
    }
  } finally {
    loading.value = false
  }
}

async function createMerchant(form: CreateMerchantFormData) {
  createError.value = null
  submittingCreate.value = true
  try {
    await $fetch('/api/merchants', {
      method: 'POST',
      body: {
        merchantReference: form.merchantReference,
        displayName: form.displayName
      }
    })
    showCreateModal.value = false
    formKey.value += 1
    useToast().add({ title: 'Merchant created', color: 'primary' })
    await loadMerchants()
  } catch (e: any) {
    if (e?.statusCode === 409) {
      createError.value = 'A merchant with this reference already exists'
    } else if (e?.data?.error === 'validation') {
      createError.value = e.data.message || 'Invalid merchant data'
    } else {
      createError.value = 'Failed to create merchant'
    }
  } finally {
    submittingCreate.value = false
  }
}

async function activateMerchant(merchant: Merchant) {
  try {
    await $fetch(`/api/merchants/${merchant.merchantId}/activate`, { method: 'POST' })
    useToast().add({ title: `${merchant.merchantReference} activated`, color: 'primary' })
    await loadMerchants()
  } catch (e: any) {
    useToast().add({ title: e?.data?.message || 'Failed to activate', color: 'error' })
  }
}

async function suspendMerchant(merchant: Merchant) {
  try {
    await $fetch(`/api/merchants/${merchant.merchantId}/suspend`, { method: 'POST' })
    useToast().add({ title: `${merchant.merchantReference} suspended`, color: 'warning' })
    await loadMerchants()
  } catch (e: any) {
    useToast().add({ title: e?.data?.message || 'Failed to suspend', color: 'error' })
  }
}

onMounted(() => {
  loadMerchants()
})
</script>
