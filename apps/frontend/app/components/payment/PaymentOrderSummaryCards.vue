<template>
  <!-- Totals row -->
  <div class="space-y-4">
    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <!-- Merchant count card (optional — requires merchantCount prop) -->
      <UCard v-if="merchantCount !== undefined">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-muted">Merchants</p>
            <p class="mt-1 text-3xl font-semibold text-highlighted">{{ merchantCount }}</p>
          </div>
          <UIcon name="i-lucide-store" class="size-8 shrink-0 text-muted" />
        </div>
      </UCard>

      <!-- Total payment orders -->
      <UCard>
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-muted">Total orders</p>
            <p class="mt-1 text-3xl font-semibold text-highlighted">{{ summary.totalOrders }}</p>
          </div>
          <UIcon name="i-lucide-receipt" class="size-8 shrink-0 text-muted" />
        </div>
      </UCard>

      <!-- Total amount (kept from original) -->
      <UCard>
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-muted">Total amount</p>
            <p class="mt-1 text-3xl font-semibold text-highlighted">{{ summary.totalAmountMinor }}</p>
            <p class="mt-0.5 text-xs text-muted">minor units</p>
          </div>
          <UIcon name="i-lucide-coins" class="size-8 shrink-0 text-muted" />
        </div>
      </UCard>

      <!-- By currency (kept from original, compact) -->
      <UCard>
        <template #header>
          <h3 class="text-sm font-medium">By currency</h3>
        </template>
        <div v-if="summary.byCurrency.length" class="space-y-2">
          <div
            v-for="row in summary.byCurrency"
            :key="row.currency"
            class="flex items-center justify-between gap-2 text-sm"
          >
            <UBadge color="neutral" variant="subtle" size="sm">{{ row.currency }}</UBadge>
            <span class="text-muted">{{ row.orderCount }} / {{ row.totalAmountMinor }}</span>
          </div>
        </div>
        <p v-else class="text-sm text-muted">No currency data.</p>
      </UCard>
    </div>

    <!-- Per-status count cards: one card per Payment_Status value -->
    <div class="grid gap-4 grid-cols-2 md:grid-cols-3 xl:grid-cols-6">
      <UCard
        v-for="status in ALL_STATUSES"
        :key="status"
        class="min-w-0"
      >
        <div class="space-y-2">
          <BusinessStatusBadge :status="status" type="payment" />
          <p class="text-2xl font-semibold text-highlighted">
            {{ countForStatus(status) }}
          </p>
          <p class="text-xs text-muted">order(s)</p>
        </div>
      </UCard>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Extended summary cards component.
 *
 * Renders:
 * - Optional merchant-count card (from the merchant list `totalElements`)
 * - Total payment-order count card (from `summary.totalOrders`)
 * - Total amount card (from `summary.totalAmountMinor`)
 * - By-currency compact card (from `summary.byCurrency`)
 * - One per-status count card for every Payment_Status value
 *   (count sourced from `summary.byStatus`; absent statuses show 0 — no
 *   client-side recomputation, the backend's absence of a status implies 0)
 *
 * All values come strictly from backend responses — no fabricated metrics.
 *
 * Requirements: 1.1, 1.7, 3.10
 * Design: Component tree and required-component mapping; Property 1
 */

import type { PaymentOrderSummaryResponse } from '~/schemas/payment-order.schema'

const props = defineProps<{
  summary: PaymentOrderSummaryResponse
  /** Optional merchant count from GET /api/merchants totalElements. */
  merchantCount?: number
}>()

/** All Payment_Status values in a stable display order. */
const ALL_STATUSES = [
  'CREATED',
  'AUTHORIZED',
  'CAPTURED',
  'CANCELLED',
  'EXPIRED',
  'REFUNDED',
] as const

/**
 * Look up the order count for a given status from the backend summary payload.
 * Returns 0 if the backend did not include an entry for that status.
 * No client-side summation or recomputation is performed — the value is either
 * present in the backend response or treated as 0 (absence = no orders in that state).
 */
function countForStatus(status: string): number {
  return props.summary.byStatus.find((row) => row.status === status)?.orderCount ?? 0
}
</script>
