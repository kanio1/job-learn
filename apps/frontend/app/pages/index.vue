<template>
  <UDashboardPanel>
    <template #header>
      <UDashboardNavbar title="Overview">
        <template #right>
          <!-- Error Lab navigation control — Req 1.3 -->
          <UButton
            to="/error-lab"
            icon="i-lucide-flask-conical"
            color="neutral"
            variant="ghost"
            size="sm"
          >
            Error Lab
          </UButton>
        </template>
      </UDashboardNavbar>
    </template>

    <div class="p-4 space-y-8">
      <!-- ─────────────────────────────────────────────────────────────
           Summary cards section
           Populated exclusively from GET /api/merchants and
           GET /api/merchants/{merchantId}/payment-orders/summary
           No client-side recomputation of counts (Req 1.1, 1.7)
           ───────────────────────────────────────────────────────────── -->
      <section v-if="canReadOverviewSummary" aria-label="Summary">
        <h2 class="text-base font-semibold mb-4">Platform Summary</h2>

        <!-- Loading state for summary section — Req 1.4 -->
        <LoadingState v-if="summaryLoading" message="Loading summary…" />

        <!-- Error state for summary — Req 1.6 -->
        <ErrorState
          v-else-if="summaryError"
          :problem="summaryProblem"
          :message="summaryError"
          :on-retry="fetchSummary"
        />

        <!-- Summary cards from backend data only — Req 1.1, 1.7 -->
        <div v-else class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <!-- Merchant count — from GET /api/merchants -->
          <UCard v-if="canReadMerchants">
            <div class="flex items-center gap-3">
              <UIcon name="i-lucide-store" class="text-2xl text-primary" />
              <div>
                <div class="text-sm text-muted">Merchants</div>
                <div class="text-2xl font-semibold text-highlighted">{{ merchantCount }}</div>
              </div>
            </div>
          </UCard>

          <!-- Payment order count — from summary endpoint -->
          <UCard v-if="canReadPaymentOrders && selectedMerchantId">
            <div class="flex items-center gap-3">
              <UIcon name="i-lucide-receipt" class="text-2xl text-primary" />
              <div>
                <div class="text-sm text-muted">Payment Orders</div>
                <div class="text-2xl font-semibold text-highlighted">{{ totalOrders }}</div>
              </div>
            </div>
          </UCard>

          <!-- Per-Payment_Status count cards — from summary.byStatus (Req 1.1) -->
          <template v-for="statusEntry in canReadPaymentOrders ? byStatus : []" :key="statusEntry.status">
            <UCard>
              <div class="flex items-center justify-between">
                <BusinessStatusBadge :status="statusEntry.status" />
                <span class="text-2xl font-semibold text-highlighted">{{ statusEntry.orderCount }}</span>
              </div>
            </UCard>
          </template>
        </div>
      </section>

      <!-- ─────────────────────────────────────────────────────────────
           Recent payment orders section
           ≤10 orders, ordered by creation time descending
           from GET /api/merchants/{merchantId}/payment-orders (Req 1.2)
           ───────────────────────────────────────────────────────────── -->
      <section v-if="canReadPaymentOrders && selectedMerchantId" aria-label="Recent payment orders">
        <h2 class="text-base font-semibold mb-4">Recent Payment Orders</h2>

        <!-- Loading state — Req 1.4 -->
        <LoadingState v-if="ordersLoading" message="Loading recent orders…" />

        <!-- Error state — Req 1.6 -->
        <ErrorState
          v-else-if="ordersError"
          :problem="ordersProblem"
          :message="ordersError"
          :on-retry="fetchRecentOrders"
        />

        <!-- Empty state when no orders for selected merchant — Req 1.5 -->
        <EmptyStateCard
          v-else-if="recentOrders.length === 0"
          description="No payment orders found for this merchant."
          action-label="Create a payment order"
          :action-to="selectedMerchantId ? `/admin/merchants/${selectedMerchantId}/payments/new` : '/admin/merchants'"
        />

        <!-- Recent orders table — Req 1.2 -->
        <UCard v-else>
          <UTable
            :data="recentOrders"
            :columns="orderColumns"
          />
        </UCard>
      </section>

      <!-- ─────────────────────────────────────────────────────────────
           No merchant scope info card — shown when there are no merchants
           ───────────────────────────────────────────────────────────── -->
      <div v-if="canReadMerchants && !summaryLoading && !summaryError && merchantCount === 0 && !ordersLoading">
        <EmptyStateCard
          description="No merchants registered yet. Create a merchant to see payment order data."
          action-label="Go to Merchants"
          action-to="/admin/merchants"
        />
      </div>
    </div>
  </UDashboardPanel>
</template>

<script setup lang="ts">
/**
 * Overview landing page — Req 1.1–1.7
 *
 * Summary cards (merchant count, payment-order count, per Payment_Status count)
 * are populated ONLY from backend responses:
 *   - Merchant count: GET /api/merchants → totalElements
 *   - Order count + per-status counts: GET /api/merchants/{merchantId}/payment-orders/summary
 *
 * NOTE: Payment order data is merchant-scoped. The backend exposes no global
 * cross-merchant list endpoint. This page fetches the merchant list first, then
 * uses the first merchant (alphabetically / by index) as the default scope for
 * the summary and recent-orders calls. If a platform:payments:read token is
 * present, all merchants are visible; this page still uses the first merchant
 * as a practical default rather than iterating all merchants.
 *
 * Recent orders: ≤10, creation time descending, from the list endpoint.
 * Bearer token: never exposed — all calls go through the Nuxt server proxy.
 */

import type { ProblemDetails } from '~/types/api'
import type { PaymentOrderResponse } from '~/schemas/payment-order.schema'
import type { TableColumn } from '@nuxt/ui'
import { h, resolveComponent } from 'vue'

const { listMerchants } = useMerchantsApi()
const { getOrderSummary, listOrders } = usePaymentOrdersApi()
const { can } = useAuthorization()

const canReadMerchants = computed(() => can.value.canReadMerchants)
const canReadPaymentOrders = computed(() => can.value.canReadMerchantPayments || can.value.canReadPlatformPayments)
const canReadOverviewSummary = computed(() => canReadMerchants.value || canReadPaymentOrders.value)

// ─── State ────────────────────────────────────────────────────────────────────

/** The merchant we use as scope for summary + recent orders */
const selectedMerchantId = ref<string | null>(null)

/** Merchant count from GET /api/merchants */
const merchantCount = ref(0)

/** Total orders from summary endpoint */
const totalOrders = ref(0)

/** Per-status breakdown from summary endpoint — Req 1.1 */
const byStatus = ref<Array<{ status: string; orderCount: number }>>([])

/** Recent orders ≤10, creation desc — Req 1.2 */
const recentOrders = ref<PaymentOrderResponse[]>([])

// ─── Loading / error state per section ────────────────────────────────────────

const summaryLoading = ref(false)
const summaryError = ref<string | null>(null)
const summaryProblem = ref<ProblemDetails | null>(null)

const ordersLoading = ref(false)
const ordersError = ref<string | null>(null)
const ordersProblem = ref<ProblemDetails | null>(null)

// ─── Timeout constant ─────────────────────────────────────────────────────────

const TIMEOUT_MS = 10_000

// ─── Column definitions for the recent-orders table ──────────────────────────

const BusinessStatusBadgeComponent = resolveComponent('BusinessStatusBadge')
const UButtonComponent = resolveComponent('UButton')

const orderColumns: TableColumn<PaymentOrderResponse>[] = [
  { accessorKey: 'clientOrderReference', header: 'Reference' },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => h(BusinessStatusBadgeComponent, { status: row.original.status }),
  },
  { accessorKey: 'currency', header: 'Currency' },
  { accessorKey: 'amountMinor', header: 'Amount (minor)' },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ row }) => h('span', { class: 'text-sm text-muted' }, formatDate(row.original.createdAt)),
  },
  {
    id: 'actions',
    cell: ({ row }) =>
      h(UButtonComponent, {
        to: `/admin/merchants/${row.original.merchantId}/payments/${row.original.paymentOrderId}`,
        variant: 'ghost',
        size: 'xs',
        icon: 'i-lucide-arrow-right',
        label: 'View',
      }),
  },
]

// ─── Fetch helpers ────────────────────────────────────────────────────────────

/**
 * Fetch merchants + summary (the "summary section").
 * Populates merchantCount, totalOrders, byStatus, and selectedMerchantId.
 */
async function fetchSummary(): Promise<void> {
  if (!canReadMerchants.value) {
    merchantCount.value = 0
    totalOrders.value = 0
    byStatus.value = []
    selectedMerchantId.value = null
    return
  }

  summaryLoading.value = true
  summaryError.value = null
  summaryProblem.value = null

  const timeout = new Promise<never>((_, reject) =>
    setTimeout(() => reject(new Error('timeout')), TIMEOUT_MS)
  )

  try {
    // Step 1: fetch merchant list — Req 1.1 (merchant count from /api/merchants)
    const merchantsRes = await Promise.race([listMerchants(), timeout])
    if (merchantsRes.problem) {
      summaryProblem.value = merchantsRes.problem
      summaryError.value = merchantsRes.problem.detail ?? 'Failed to load merchants.'
      return
    }
    if (!merchantsRes.data) {
      summaryError.value = 'Merchant list response was invalid.'
      return
    }

    // Merchant count comes directly from the backend response — no client recompute
    merchantCount.value = merchantsRes.data.totalElements

    if (merchantsRes.data.content.length === 0) {
      // No merchants → no summary to fetch
      totalOrders.value = 0
      byStatus.value = []
      return
    }

    // Step 2: use the first merchant as the default summary scope
    // This is merchant-scoped; see module comment above for the reasoning.
    const firstMerchant = merchantsRes.data.content[0]!
    selectedMerchantId.value = firstMerchant.merchantId

    if (!canReadPaymentOrders.value) {
      totalOrders.value = 0
      byStatus.value = []
      return
    }

    // Step 3: fetch summary for the selected merchant
    const summaryRes = await Promise.race([getOrderSummary(firstMerchant.merchantId), timeout])
    if (summaryRes.problem) {
      summaryProblem.value = summaryRes.problem
      summaryError.value = summaryRes.problem.detail ?? 'Failed to load payment order summary.'
      return
    }
    if (!summaryRes.data) {
      summaryError.value = 'Payment order summary response was invalid.'
      return
    }

    // Counts come directly from the backend summary response — Req 1.1, 1.7
    totalOrders.value = summaryRes.data.totalOrders
    byStatus.value = summaryRes.data.byStatus.map(entry => ({
      status: entry.status,
      orderCount: entry.orderCount,
    }))
  }
  catch (err: unknown) {
    if (err instanceof Error && err.message === 'timeout') {
      summaryError.value = 'The summary request did not complete within 10 seconds.'
    }
    else {
      summaryError.value = 'An unexpected error occurred while loading the summary.'
    }
  }
  finally {
    summaryLoading.value = false
  }
}

/**
 * Fetch recent payment orders for the selected merchant scope.
 * ≤10 orders, ordered by creation time descending — Req 1.2.
 */
async function fetchRecentOrders(): Promise<void> {
  if (!canReadPaymentOrders.value || !selectedMerchantId.value) {
    // No merchant scope available — nothing to fetch
    return
  }

  ordersLoading.value = true
  ordersError.value = null
  ordersProblem.value = null

  const timeout = new Promise<never>((_, reject) =>
    setTimeout(() => reject(new Error('timeout')), TIMEOUT_MS)
  )

  try {
    const ordersRes = await Promise.race([
      listOrders(selectedMerchantId.value, {
        page: 0,
        size: 10,
        sort: 'createdAt,desc',
      }),
      timeout,
    ])

    if (ordersRes.problem) {
      ordersProblem.value = ordersRes.problem
      ordersError.value = ordersRes.problem.detail ?? 'Failed to load recent payment orders.'
      return
    }
    if (!ordersRes.data) {
      ordersError.value = 'Recent payment orders response was invalid.'
      return
    }

    // Slice defensively to at most 10 even if the backend returns more
    recentOrders.value = ordersRes.data.content.slice(0, 10)
  }
  catch (err: unknown) {
    if (err instanceof Error && err.message === 'timeout') {
      ordersError.value = 'The recent orders request did not complete within 10 seconds.'
    }
    else {
      ordersError.value = 'An unexpected error occurred while loading recent orders.'
    }
  }
  finally {
    ordersLoading.value = false
  }
}

/** Format ISO date string to a readable local date+time */
function formatDate(isoString: string): string {
  try {
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(new Date(isoString))
  }
  catch {
    return isoString
  }
}

// ─── Initial data load ────────────────────────────────────────────────────────

onMounted(async () => {
  // Run summary fetch first so we have selectedMerchantId for the orders fetch
  await fetchSummary()
  await fetchRecentOrders()
})
</script>
