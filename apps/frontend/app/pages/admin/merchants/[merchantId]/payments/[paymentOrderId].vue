<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">Payment Order Detail</h2>
      <UButton variant="ghost" :to="`/admin/merchants/${merchantId}`" label="Back to Merchant" />
    </div>

    <div v-if="loading" class="text-sm text-gray-500">Loading...</div>
    <div v-else-if="error" class="text-sm text-red-600">{{ error }}</div>
    <PaymentOrderDetail v-else :order="order" />
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const merchantId = route.params.merchantId as string
const paymentOrderId = route.params.paymentOrderId as string

const order = ref<any>(null)
const loading = ref(true)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    order.value = await $fetch(`/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`)
  } catch (e: any) {
    error.value = e?.data?.message || e?.statusMessage || 'Failed to load payment order'
  } finally {
    loading.value = false
  }
})
</script>
