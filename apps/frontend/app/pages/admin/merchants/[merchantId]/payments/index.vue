<template>
  <UDashboardPanel id="merchant-payments">
    <template #header>
      <UDashboardNavbar title="Payment Orders" :ui="{ right: 'gap-3' }">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>

        <template #right>
          <UButton color="neutral" variant="ghost" :to="'/admin/merchants'" label="Merchants" />
          <UButton
            v-if="!store.insufficientAuthority"
            icon="i-lucide-refresh-cw"
            color="neutral"
            variant="ghost"
            square
            aria-label="Refresh payment orders"
            @click="loadPayments"
          />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <div class="space-y-6">
        <div>
          <p class="text-sm text-muted">Merchant</p>
          <p class="font-mono text-sm text-highlighted">{{ merchantId }}</p>
        </div>

        <UAlert
          v-if="store.insufficientAuthority"
          color="warning"
          variant="subtle"
          icon="i-lucide-shield-alert"
          title="You do not have permission to view payment orders"
          description="The backend rejected this payment list or summary request. No payment data is rendered in this state."
          role="alert"
        />

        <UAlert
          v-else-if="store.error"
          color="error"
          variant="subtle"
          icon="i-lucide-circle-alert"
          title="Payment orders unavailable"
          :description="store.error"
          role="alert"
        >
          <template #actions>
            <UButton size="sm" color="error" variant="soft" aria-label="Retry loading payment orders" @click="loadPayments">
              Retry
            </UButton>
          </template>
        </UAlert>

        <div v-else-if="store.loading" class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <USkeleton v-for="index in 4" :key="index" class="h-32 rounded-xl" />
        </div>

        <template v-else>
          <PaymentOrderSummaryCards v-if="store.summary" :summary="store.summary" />
          <PaymentOrderListTable v-if="store.list" :merchant-id="merchantId" :list="store.list" />
        </template>
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
definePageMeta({
  layout: 'dashboard',
})

const route = useRoute()
const merchantId = route.params.merchantId as string
const store = usePaymentOrdersStore()

async function loadPayments() {
  store.clearError()
  try {
    await store.loadSummary(merchantId)
    if (!store.insufficientAuthority) {
      await store.loadList(merchantId, { page: 0, size: 20, sort: 'createdAt,desc' })
    }
  } catch {
    // Store owns user-facing error state.
  }
}

onMounted(() => {
  loadPayments()
})
</script>
