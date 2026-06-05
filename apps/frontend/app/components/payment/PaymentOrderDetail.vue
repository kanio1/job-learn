<template>
  <div v-if="order" class="space-y-6">
    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-base font-semibold">Payment Order</h3>
          <PaymentStatusBadge :status="order.status" />
        </div>
      </template>

      <dl class="space-y-3 text-sm">
        <div class="flex justify-between">
          <dt class="text-gray-500">Payment Order ID</dt>
          <dd class="font-mono">{{ order.paymentOrderId }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-gray-500">Merchant ID</dt>
          <dd class="font-mono">{{ order.merchantId }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-gray-500">Amount</dt>
          <dd>{{ order.amountMinor }} minor units</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-gray-500">Currency</dt>
          <dd>{{ order.currency }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-gray-500">Client Reference</dt>
          <dd>{{ order.clientOrderReference }}</dd>
        </div>
        <div v-if="order.capturedAmountMinor != null" class="flex justify-between">
          <dt class="text-gray-500">Captured Amount</dt>
          <dd>{{ order.capturedAmountMinor }} minor units</dd>
        </div>
        <div v-if="order.refundedAmountMinor != null" class="flex justify-between">
          <dt class="text-gray-500">Refunded Amount</dt>
          <dd>{{ order.refundedAmountMinor }} minor units</dd>
        </div>
        <div v-if="order.authorizedAt" class="flex justify-between">
          <dt class="text-gray-500">Authorized At</dt>
          <dd>{{ new Date(order.authorizedAt).toLocaleString() }}</dd>
        </div>
        <div v-if="order.expiresAt" class="flex justify-between">
          <dt class="text-gray-500">Expires At</dt>
          <dd>{{ new Date(order.expiresAt).toLocaleString() }}</dd>
        </div>
        <div v-if="order.capturedAt" class="flex justify-between">
          <dt class="text-gray-500">Captured At</dt>
          <dd>{{ new Date(order.capturedAt).toLocaleString() }}</dd>
        </div>
        <div v-if="order.cancelledAt" class="flex justify-between">
          <dt class="text-gray-500">Cancelled At</dt>
          <dd>{{ new Date(order.cancelledAt).toLocaleString() }}</dd>
        </div>
        <div v-if="order.refundedAt" class="flex justify-between">
          <dt class="text-gray-500">Refunded At</dt>
          <dd>{{ new Date(order.refundedAt).toLocaleString() }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-gray-500">Created At</dt>
          <dd>{{ new Date(order.createdAt).toLocaleString() }}</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-gray-500">Updated At</dt>
          <dd>{{ new Date(order.updatedAt).toLocaleString() }}</dd>
        </div>
      </dl>
    </UCard>

    <UCard v-if="history && history.length > 0">
      <template #header>
        <h3 class="text-base font-semibold">Lifecycle History</h3>
      </template>

      <div class="space-y-3">
        <div v-for="entry in history" :key="entry.statusHistoryId" class="flex items-start gap-3 text-sm border-l-2 border-gray-200 pl-3">
          <div class="flex-1">
            <div class="flex items-center gap-2">
              <PaymentStatusBadge v-if="entry.fromStatus" :status="entry.fromStatus" />
              <span class="text-gray-400">→</span>
              <PaymentStatusBadge :status="entry.toStatus" />
            </div>
            <div class="mt-1 text-xs text-gray-500">
              {{ entry.action }} · {{ entry.actorSubject }} · {{ new Date(entry.createdAt).toLocaleString() }}
            </div>
          </div>
        </div>
      </div>
    </UCard>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  order: {
    paymentOrderId: string
    merchantId: string
    clientOrderReference: string
    amountMinor: number
    currency: string
    status: string
    capturedAmountMinor?: number | null
    refundedAmountMinor?: number | null
    authorizedAt?: string | null
    expiresAt?: string | null
    capturedAt?: string | null
    cancelledAt?: string | null
    refundedAt?: string | null
    cancellationReason?: string | null
    refundReason?: string | null
    createdAt: string
    updatedAt: string
  } | null
  history?: Array<{
    statusHistoryId: string
    paymentOrderId: string
    fromStatus: string | null
    toStatus: string
    action: string | null
    actorSubject: string
    idempotencyKeyHash: string | null
    correlationId: string
    createdAt: string
  }> | null
}>()
</script>
