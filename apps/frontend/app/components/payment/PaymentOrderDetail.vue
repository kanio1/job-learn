<template>
  <UCard v-if="order">
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
    createdAt: string
    updatedAt: string
  } | null
}>()
</script>
