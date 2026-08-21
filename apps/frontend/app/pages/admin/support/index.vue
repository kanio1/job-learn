<template>
  <UDashboardPanel id="support">
    <template #header>
      <UDashboardNavbar :title="$t('support.title')">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
        <template #right>
          <AppLocaleSelect />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <UTabs :items="tabItems" class="w-full">
        <template #search>
      <div class="space-y-4 pt-4">
        <UCard>
          <template #header>
            <span class="text-sm font-semibold">Search Payment Orders</span>
          </template>

          <div class="flex flex-wrap gap-3 items-end">
            <UFormField label="Merchant ID" required hint="Required — narrows search to a single merchant">
              <UInput
                v-model="searchMerchantId"
                data-testid="support-search-merchant-id"
                placeholder="merchant UUID"
                aria-label="Merchant ID (required)"
                class="min-w-64"
              />
            </UFormField>
            <UFormField label="Client Order Reference">
              <UInput
                v-model="clientOrderReference"
                data-testid="support-search-client-ref"
                placeholder="e.g. SEED-ALPHA-001-CREATED"
                aria-label="Client order reference"
                class="min-w-64"
              />
            </UFormField>
            <UButton
              data-testid="support-search-button"
              icon="i-lucide-search"
              color="primary"
              :loading="loading"
              :disabled="!canSearch"
              @click="handleSearch"
            >
              Search
            </UButton>
          </div>
        </UCard>

        <LoadingState v-if="loading" message="Searching…" />

        <ErrorState
          v-else-if="searchError"
          :message="searchError"
          :problem="searchProblem"
        />

        <UCard v-else-if="results.length > 0" data-testid="support-search-results">
          <template #header>
            <span class="text-sm font-semibold">Results ({{ results.length }})</span>
          </template>
          <div role="table" aria-label="Support search results">
            <UTable
              :data="results"
              :columns="columns"
            />
          </div>
        </UCard>

        <UCard v-else-if="searched && results.length === 0">
          <div class="flex flex-col items-center gap-2 py-6 text-center">
            <UIcon name="i-lucide-search-x" class="size-8 text-gray-400" />
            <p class="text-sm font-medium text-gray-700 dark:text-gray-300">No results</p>
            <p class="text-xs text-gray-500 dark:text-gray-400">No payment orders match the search criteria.</p>
          </div>
        </UCard>
      </div>
        </template>
        <template #queue>
          <div class="pt-4">
            <SupportKanban />
          </div>
        </template>
      </UTabs>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
import { h, resolveComponent } from 'vue'
import { z } from 'zod'
import type { TableColumn } from '@nuxt/ui'
import type { ProblemDetails } from '~/types/api'

definePageMeta({ layout: 'dashboard' })

const UButton = resolveComponent('UButton')
const BusinessStatusBadge = resolveComponent('BusinessStatusBadge')

const { request } = useApiClient()

const clientOrderReference = ref('')
const searchMerchantId = ref('')
const loading = ref(false)
const searchError = ref<string | null>(null)
const searchProblem = ref<ProblemDetails | null>(null)
const results = ref<SupportPaymentOrder[]>([])
const searched = ref(false)

const { t } = useI18n()
const tabItems = computed(() => [
  { label: 'Search', slot: 'search' as const },
  { label: t('support.queue'), slot: 'queue' as const },
])

const canSearch = computed(() => searchMerchantId.value.trim() !== '')

const paymentOrderSchema = z.object({
  paymentOrderId: z.string(),
  merchantId: z.string(),
  clientOrderReference: z.string(),
  status: z.string(),
  amountMinor: z.number(),
  currency: z.string(),
  createdAt: z.string(),
}).passthrough()

type SupportPaymentOrder = z.infer<typeof paymentOrderSchema>

const listSchema = z.object({ content: z.array(paymentOrderSchema) }).passthrough()

const { amount, dateOnly } = useLocaleFormat()

const columns = computed<TableColumn<SupportPaymentOrder>[]>(() => [
  {
    accessorKey: 'clientOrderReference',
    header: 'Client Reference',
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => h(BusinessStatusBadge, { status: row.original.status, type: 'payment' }),
  },
  {
    accessorKey: 'amountMinor',
    header: 'Amount',
    cell: ({ row }) => h('span', { 'data-testid': 'support-amount' }, amount(row.original.amountMinor, row.original.currency)),
  },
  {
    accessorKey: 'currency',
    header: 'Currency',
  },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ row }) => h('span', { class: 'text-sm', 'data-testid': 'support-created-at' }, dateOnly(row.original.createdAt)),
  },
  {
    id: 'view',
    header: '',
    cell: ({ row }) => {
      const mid = row.original.merchantId
      const pid = row.original.paymentOrderId
      if (!mid || !pid) return null
      return h(UButton, {
        size: 'xs',
        variant: 'ghost',
        color: 'primary',
        icon: 'i-lucide-external-link',
        label: 'View',
        'aria-label': `View payment order ${row.original.clientOrderReference}`,
        to: `/admin/merchants/${mid}/payments/${pid}`,
      })
    },
  },
])

async function handleSearch() {
  if (!canSearch.value) return
  loading.value = true
  searchError.value = null
  searchProblem.value = null
  searched.value = true

  const mid = searchMerchantId.value.trim()

  const query: Record<string, string> = {}
  if (clientOrderReference.value.trim()) {
    query.clientOrderReference = clientOrderReference.value.trim()
  }

  const response = await request(
    `/api/merchants/${mid}/payment-orders`,
    listSchema,
    { query }
  )

  loading.value = false

  if (response.data) {
    results.value = response.data.content ?? []
  } else {
    searchError.value = response.problem?.detail ?? 'Search failed'
    searchProblem.value = response.problem
  }
}
</script>
