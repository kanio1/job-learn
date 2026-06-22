<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">New Payment Order</h2>
      <UButton variant="ghost" :to="`/admin/merchants/${merchantId}/payments`" label="Back to payment orders" />
    </div>

    <CreatePaymentOrderForm
      :merchant-id="merchantId"
      @created="onCreated"
      @debug-request="onDebugRequest"
      @debug-response="onDebugResponse"
    />

    <!-- API Debug Panel: shows the most recent request/response from the create operation -->
    <ApiDebugPanel
      v-if="lastRequest"
      :request="lastRequest"
      :response="lastResponse ?? undefined"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * New Payment Order page (EXTENDED for task 10.2).
 *
 * Wires ApiDebugPanel to receive the most recent request/response
 * from the CreatePaymentOrderForm via custom events.
 *
 * Requirements: 3.2, 3.3, 3.4, 10.1, 10.2, 10.3, 12.3
 */

definePageMeta({
  layout: 'dashboard',
})

import CreatePaymentOrderForm from '~/components/payment/CreatePaymentOrderForm.vue'
import ApiDebugPanel from '~/components/shared/ApiDebugPanel.vue'

const route = useRoute()
const merchantId = route.params.merchantId as string

interface RequestInfo {
  method: string
  path: string
  headers?: Record<string, string>
}

interface ResponseInfo {
  status: number
  headers?: Record<string, string>
  body?: string
}

const lastRequest = ref<RequestInfo | null>(null)
const lastResponse = ref<ResponseInfo | null>(null)

function onDebugRequest(info: RequestInfo) {
  lastRequest.value = info
  // Clear response when a new request starts
  lastResponse.value = null
}

function onDebugResponse(info: ResponseInfo) {
  lastResponse.value = info
}

function onCreated(paymentOrderId: string) {
  navigateTo(`/admin/merchants/${merchantId}/payments/${paymentOrderId}`)
}
</script>
