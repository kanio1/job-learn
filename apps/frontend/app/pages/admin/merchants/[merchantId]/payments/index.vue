<template>
  <UDashboardPanel id="merchant-payments">
    <template #header>
      <UDashboardNavbar title="Payment Orders" :ui="{ right: 'gap-3' }">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>

        <template #right>
          <UButton color="neutral" variant="ghost" :to="'/admin/merchants'" label="Merchants" />
          <UButton
            icon="i-lucide-refresh-cw"
            color="neutral"
            variant="ghost"
            square
            aria-label="Refresh payment orders"
            @click="reload"
          />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <div class="space-y-6">
        <!-- Merchant identifier -->
        <div>
          <p class="text-sm text-muted">Merchant</p>
          <p class="font-mono text-sm text-highlighted">{{ merchantId }}</p>
        </div>

        <!-- Authorization denied state (403 from summary/list) -->
        <UAlert
          v-if="authDenied"
          color="warning"
          variant="subtle"
          icon="i-lucide-shield-alert"
          title="You do not have permission to view payment orders"
          description="The backend rejected this payment list or summary request. No payment data is rendered in this state."
          role="alert"
        />

        <!-- Summary loading skeleton -->
        <div v-else-if="summaryLoading" class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <USkeleton v-for="index in 4" :key="index" class="h-32 rounded-xl" />
        </div>

        <!-- Summary error -->
        <ErrorState
          v-else-if="summaryError"
          :message="summaryError"
          :problem="summaryProblem"
          :on-retry="reloadSummary"
        />

        <!-- Summary cards -->
        <PaymentOrderSummaryCards v-else-if="summary" :summary="summary" />

        <!-- Payment order list table with filters/pagination/states built in -->
        <PaymentOrderListTable
          v-if="!authDenied"
          :merchant-id="merchantId"
          :list="listData"
          :loading="listLoading"
          :error="listError"
          :problem="listProblem"
          :current-query="currentQuery"
          :on-retry="reloadList"
          @query-change="onQueryChange"
        />
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
import type { ProblemDetails } from '~/types/api'
import type { PaymentOrderListResponse, PaymentOrderSummaryResponse, PaymentOrderListQuery } from '~/schemas/payment-order.schema'

definePageMeta({
  layout: 'dashboard',
})

const route = useRoute()
const merchantId = route.params.merchantId as string
const { listOrders, getOrderSummary } = usePaymentOrdersApi()

// ---------------------------------------------------------------------------
// Summary state
// ---------------------------------------------------------------------------
const summary = ref<PaymentOrderSummaryResponse | null>(null)
const summaryLoading = ref(false)
const summaryError = ref<string | null>(null)
const summaryProblem = ref<ProblemDetails | null>(null)
const authDenied = ref(false)

// ---------------------------------------------------------------------------
// List state
// ---------------------------------------------------------------------------
const listData = ref<PaymentOrderListResponse | null>(null)
const listLoading = ref(false)
const listError = ref<string | null>(null)
const listProblem = ref<ProblemDetails | null>(null)

// ---------------------------------------------------------------------------
// Query state (filter + pagination) — defaults: page=0, size=20
// ---------------------------------------------------------------------------
const currentQuery = ref<PaymentOrderListQuery>({
  page: 0,
  size: 20,
  sort: 'createdAt,desc',
})

// ---------------------------------------------------------------------------
// Load summary
// ---------------------------------------------------------------------------
async function reloadSummary() {
  summaryLoading.value = true
  summaryError.value = null
  summaryProblem.value = null
  authDenied.value = false

  const response = await getOrderSummary(merchantId)

  summaryLoading.value = false

  if (response.status === 403) {
    authDenied.value = true
    return
  }

  if (response.data) {
    summary.value = response.data
  } else {
    summaryProblem.value = response.problem
    summaryError.value = response.problem?.detail ?? 'Failed to load payment order summary.'
  }
}

// ---------------------------------------------------------------------------
// Load list
// ---------------------------------------------------------------------------
async function reloadList() {
  listLoading.value = true
  listError.value = null
  listProblem.value = null

  const response = await listOrders(merchantId, currentQuery.value)

  listLoading.value = false

  if (response.status === 403) {
    // Auth denied is already shown via summary; silently skip list
    return
  }

  if (response.data) {
    listData.value = response.data
  } else {
    listProblem.value = response.problem
    listError.value = response.problem?.detail ?? 'Failed to load payment orders.'
  }
}

// ---------------------------------------------------------------------------
// Combined reload
// ---------------------------------------------------------------------------
async function reload() {
  await reloadSummary()
  if (!authDenied.value) {
    await reloadList()
  }
}

// ---------------------------------------------------------------------------
// Filter / pagination event from table component
// ---------------------------------------------------------------------------
function onQueryChange(query: PaymentOrderListQuery) {
  currentQuery.value = query
  reloadList()
}

// ---------------------------------------------------------------------------
// Initial load
// ---------------------------------------------------------------------------
onMounted(() => {
  reload()
})
</script>
