<template>
  <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
    <UCard>
      <div class="text-sm text-muted">Total orders</div>
      <div class="mt-2 text-3xl font-semibold text-highlighted">{{ summary.totalOrders }}</div>
    </UCard>

    <UCard>
      <div class="text-sm text-muted">Total amount</div>
      <div class="mt-2 text-3xl font-semibold text-highlighted">{{ summary.totalAmountMinor }}</div>
      <div class="mt-1 text-xs text-muted">minor units</div>
    </UCard>

    <UCard>
      <template #header>
        <h3 class="text-sm font-medium">By currency</h3>
      </template>
      <div v-if="summary.byCurrency.length" class="space-y-2">
        <div v-for="row in summary.byCurrency" :key="row.currency" class="flex items-center justify-between gap-3 text-sm">
          <UBadge color="neutral" variant="subtle">{{ row.currency }}</UBadge>
          <span>{{ row.orderCount }} order(s), {{ row.totalAmountMinor }} minor units</span>
        </div>
      </div>
      <p v-else class="text-sm text-muted">No currency totals yet.</p>
    </UCard>

    <UCard>
      <template #header>
        <h3 class="text-sm font-medium">By status</h3>
      </template>
      <div v-if="summary.byStatus.length" class="space-y-2">
        <div v-for="row in summary.byStatus" :key="row.status" class="flex items-center justify-between gap-3 text-sm">
          <PaymentStatusBadge :status="row.status" />
          <span>{{ row.orderCount }} order(s), {{ row.totalAmountMinor }} minor units</span>
        </div>
      </div>
      <p v-else class="text-sm text-muted">No status totals yet.</p>
    </UCard>
  </div>
</template>

<script setup lang="ts">
import type { PaymentOrderSummaryResponse } from '~/schemas/payment-order.schema'

defineProps<{
  summary: PaymentOrderSummaryResponse
}>()
</script>
