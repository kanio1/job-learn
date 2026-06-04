<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">New Payment Order</h2>
      <UButton variant="ghost" :to="`/admin/merchants/${merchantId}/payments`" label="Back to payment orders" />
    </div>

    <CreatePaymentOrderForm :merchant-id="merchantId" @created="onCreated" />
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  layout: 'dashboard',
})

const route = useRoute()
const merchantId = route.params.merchantId as string

function onCreated(paymentOrderId: string) {
  navigateTo(`/admin/merchants/${merchantId}/payments/${paymentOrderId}`)
}
</script>
