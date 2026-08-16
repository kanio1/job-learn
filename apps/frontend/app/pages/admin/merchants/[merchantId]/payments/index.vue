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
            data-testid="export-payment-orders-csv"
            icon="i-lucide-download"
            color="neutral"
            variant="ghost"
            square
            aria-label="Export payment orders CSV"
            @click="handleExportCsv"
          />
          <UButton
            data-testid="export-payment-orders-async"
            icon="i-lucide-hourglass"
            color="neutral"
            variant="ghost"
            :loading="asyncExportPolling"
            aria-label="Export payment orders asynchronously"
            @click="handleAsyncExport"
          >
            Export (async)
          </UButton>
          <UButton
            v-if="can.canReadPlatformPayments && can.canRunLifecycle"
            data-testid="run-expiration-sweep"
            color="neutral"
            variant="outline"
            @click="handleExpirationSweep"
          >
            Run expiration sweep
          </UButton>
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
        <p v-if="asyncExportStatus" data-testid="async-export-status" class="text-sm text-muted">
          Async export: {{ asyncExportStatus }}
        </p>
        <UAlert
          v-if="offline"
          color="warning"
          variant="subtle"
          icon="i-lucide-wifi-off"
          title="You are offline"
          description="Payment list cannot refresh until the browser is online. Retry when connectivity returns."
          data-testid="payments-offline-banner"
        />
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
import { paymentOrderListQuerySchema } from '~/schemas/payment-order.schema'

definePageMeta({
  layout: 'dashboard',
})

const route = useRoute()
const router = useRouter()
const merchantId = route.params.merchantId as string
const { listOrders, getOrderSummary } = usePaymentOrdersApi()
const { can } = useAuthorization()
const toast = useAppToast()
const offline = ref(false)
const asyncExportPolling = ref(false)
const asyncExportStatus = ref('')

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
  ...paymentQueryFromRoute(),
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
  void syncQueryToUrl(query)
  reloadList()
}

function paymentQueryFromRoute(): PaymentOrderListQuery {
  const rawQuery = {
    status: singleQueryValue(route.query.status),
    currency: singleQueryValue(route.query.currency),
    fromDate: singleQueryValue(route.query.fromDate),
    toDate: singleQueryValue(route.query.toDate),
    minAmount: numberQueryValue(route.query.minAmount),
    maxAmount: numberQueryValue(route.query.maxAmount),
    clientOrderReference: singleQueryValue(route.query.clientOrderReference),
    page: numberQueryValue(route.query.page) ?? 0,
    size: numberQueryValue(route.query.size) ?? 20,
    sort: singleQueryValue(route.query.sort) ?? 'createdAt,desc',
  }

  const parsed = paymentOrderListQuerySchema.safeParse(rawQuery)

  if (parsed.success) {
    return parsed.data
  }

  return {
    page: 0,
    size: 20,
    sort: 'createdAt,desc',
  }
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

async function syncQueryToUrl(query: PaymentOrderListQuery) {
  const nextQuery: Record<string, string> = {}

  if (query.status) nextQuery.status = query.status
  if (query.currency) nextQuery.currency = query.currency
  if (query.fromDate) nextQuery.fromDate = query.fromDate
  if (query.toDate) nextQuery.toDate = query.toDate
  if (query.minAmount !== undefined) nextQuery.minAmount = String(query.minAmount)
  if (query.maxAmount !== undefined) nextQuery.maxAmount = String(query.maxAmount)
  if (query.clientOrderReference) nextQuery.clientOrderReference = query.clientOrderReference
  if (query.page && query.page > 0) nextQuery.page = String(query.page)
  if (query.size && query.size !== 20) nextQuery.size = String(query.size)
  if (query.sort && query.sort !== 'createdAt,desc') nextQuery.sort = query.sort

  await router.replace({ query: nextQuery })
}

// ---------------------------------------------------------------------------
// CSV export — triggers a browser download via programmatic anchor click.
// Content-Disposition: attachment on the BFF response makes the browser save
// the response as a file rather than navigating.
// ---------------------------------------------------------------------------
function handleExportCsv() {
  const link = document.createElement('a')
  link.href = `/api/merchants/${merchantId}/payment-orders/export`
  link.click()
}

async function handleAsyncExport() {
  asyncExportPolling.value = true
  asyncExportStatus.value = 'PENDING'
  try {
    const created = await $fetch.raw<{ jobId: string, status: string }>(
      `/api/merchants/${merchantId}/payment-orders/export-jobs`,
      { method: 'POST' },
    )
    const jobId = created._data?.jobId
    const location = created.headers.get('location')
    if (!jobId) {
      throw new Error('Export job did not return a job id')
    }
    for (let i = 0; i < 20; i++) {
      const job = await $fetch<{ status: string }>(
        `/api/merchants/${merchantId}/payment-orders/export-jobs/${jobId}`,
      )
      asyncExportStatus.value = job.status
      if (job.status === 'READY') {
        const link = document.createElement('a')
        link.href = `/api/merchants/${merchantId}/payment-orders/export-jobs/${jobId}/content`
        link.click()
        toast.success('Export ready', location || jobId)
        return
      }
      if (job.status === 'FAILED') {
        toast.error('Export failed', 'The async export job failed.')
        return
      }
      await new Promise(resolve => setTimeout(resolve, 300))
    }
    toast.error('Export still pending', 'Poll the job again from the network tab.')
  } catch (error: any) {
    toast.error('Async export failed', error?.data?.detail || error?.message || 'Request failed')
  } finally {
    asyncExportPolling.value = false
  }
}

async function handleExpirationSweep() {
  const result = await $fetch<{ expiredCount: number }>('/api/payment-ops/expiration-sweep', { method: 'POST' })
  toast.success('Expiration sweep complete', `${result.expiredCount} order(s) expired`)
  await reload()
}

function syncOffline() {
  offline.value = !navigator.onLine
}

// ---------------------------------------------------------------------------
// Initial load
// ---------------------------------------------------------------------------
onMounted(() => {
  syncOffline()
  window.addEventListener('online', syncOffline)
  window.addEventListener('offline', syncOffline)
  reload()
})

onUnmounted(() => {
  window.removeEventListener('online', syncOffline)
  window.removeEventListener('offline', syncOffline)
})
</script>
