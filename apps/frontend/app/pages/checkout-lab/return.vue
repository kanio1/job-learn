<template>
  <div data-testid="checkout-return" class="mx-auto max-w-lg p-6 space-y-4">
    <UAlert
      color="warning"
      variant="subtle"
      title="Return URL is not proof of payment"
      description="The query hint below is untrusted. Fulfillment status is the oracle."
    />
    <p class="text-sm">Query hint: <span data-testid="return-hint">{{ hint }}</span></p>
    <p class="text-sm">
      Fulfillment:
      <span data-testid="fulfillment-status">{{ fulfillmentStatus }}</span>
    </p>
    <LoadingState v-if="waiting" message="Waiting for notify → inbox → fulfill…" />
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: false })

const route = useRoute()
const hint = computed(() => String(route.query.status || 'none'))
const sessionId = computed(() => String(route.query.sessionId || ''))
const fulfillmentStatus = ref('UNKNOWN')
const waiting = ref(true)
const { getHostedFulfillment } = useCheckoutLabApi()

async function poll() {
  if (!sessionId.value) {
    waiting.value = false
    return
  }
  const response = await getHostedFulfillment(sessionId.value)
  fulfillmentStatus.value = response.data?.status || 'UNKNOWN'
  if (fulfillmentStatus.value === 'CONFIRMED' || fulfillmentStatus.value === 'CANCELLED' || fulfillmentStatus.value === 'EXPIRED') {
    waiting.value = false
    return
  }
}

const polling = usePaymentStatusPolling(async () => {
  await poll()
  if (!waiting.value) {
    polling.stop()
  }
  return { status: fulfillmentStatus.value }
})

onMounted(async () => {
  await poll()
  if (waiting.value) {
    polling.setAutoRefresh(true, 1000)
  }
})
</script>
