<template>
  <section data-testid="payment-status-chart" aria-labelledby="payment-status-chart-heading" class="space-y-3">
    <h2 id="payment-status-chart-heading" class="text-sm font-medium text-highlighted">
      Status distribution
    </h2>
    <table class="w-full text-sm" aria-label="Payment status counts">
      <thead>
        <tr class="text-left text-muted">
          <th scope="col" class="py-1 font-medium">Status</th>
          <th scope="col" class="py-1 font-medium">Orders</th>
          <th scope="col" class="py-1 font-medium">Amount (minor)</th>
          <th scope="col" class="py-1 font-medium">Share</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.status">
          <th scope="row" class="py-1.5 font-medium">{{ row.status }} {{ row.orderCount }}</th>
          <td class="py-1.5">{{ row.orderCount }}</td>
          <td class="py-1.5">{{ row.totalAmountMinor }}</td>
          <td class="py-1.5 w-1/3">
            <UProgress
              :model-value="row.percent"
              :max="100"
              :aria-label="`${row.status} ${row.orderCount}`"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import type { PaymentOrderSummaryResponse } from '~/schemas/payment-order.schema'

const props = defineProps<{
  summary: PaymentOrderSummaryResponse
}>()

const STATUSES = ['CREATED', 'AUTHORIZED', 'CAPTURED', 'CANCELLED', 'EXPIRED', 'REFUNDED'] as const

const rows = computed(() => {
  const totalCount = props.summary.byStatus.reduce((sum, entry) => sum + entry.orderCount, 0)
  return STATUSES.map((status) => {
    const entry = props.summary.byStatus.find(row => row.status === status)
    const orderCount = entry?.orderCount ?? 0
    const totalAmountMinor = entry?.totalAmountMinor ?? 0
    const percent = totalCount === 0 ? 0 : Math.round((orderCount / totalCount) * 100)
    return { status, orderCount, totalAmountMinor, percent }
  })
})
</script>
