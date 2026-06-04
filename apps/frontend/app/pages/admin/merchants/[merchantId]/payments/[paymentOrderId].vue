<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">Payment Order Detail</h2>
      <UButton variant="ghost" :to="`/admin/merchants/${merchantId}/payments`" label="Back to payment orders" />
    </div>

    <UAlert v-if="store.insufficientAuthority" color="error" variant="subtle" title="Insufficient permissions" description="You do not have permission to view this payment order." />
    <UAlert v-else-if="store.error && !store.loading" color="error" variant="subtle" :title="store.error" />
    <div v-else-if="store.loading" class="text-sm text-gray-500">Loading...</div>
    <PaymentOrderDetail v-else-if="store.currentOrder" :order="store.currentOrder" />
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  layout: 'dashboard',
})

const route = useRoute()
const merchantId = route.params.merchantId as string
const paymentOrderId = route.params.paymentOrderId as string

const store = usePaymentOrdersStore()

onMounted(async () => {
  try {
    await store.loadDetail(merchantId, paymentOrderId)
  } catch {
    // Error is handled by the store
  }
})
</script>
