<template>
  <UDashboardPanel id="merchant-detail" data-testid="merchant-detail-panel">
    <template #header>
      <UDashboardNavbar :title="merchant?.displayName ?? 'Merchant Details'">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
        <template #right>
          <UButton
            color="neutral"
            variant="ghost"
            icon="i-lucide-arrow-left"
            label="Back to merchants"
            @click="navigateTo('/admin/merchants')"
          />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <LoadingState v-if="loading" message="Loading merchant…" />

      <ErrorState
        v-else-if="loadError"
        :problem="loadProblem"
        :message="loadError"
        :on-retry="load"
      />

      <div v-else-if="merchant" class="space-y-4">
        <!-- Merchant Info -->
        <UCard>
          <template #header>
            <div class="flex items-center justify-between gap-2">
              <span class="text-sm font-semibold">Merchant Information</span>
              <MerchantStatusBadge :status="merchant.status" data-testid="merchant-status-badge" />
            </div>
          </template>

          <dl class="space-y-2 text-sm">
            <div class="flex gap-2">
              <dt class="w-36 shrink-0 font-medium text-gray-500 dark:text-gray-400">Name</dt>
              <dd data-testid="merchant-name" class="text-gray-900 dark:text-gray-100">{{ merchant.displayName }}</dd>
            </div>
            <div class="flex gap-2">
              <dt class="w-36 shrink-0 font-medium text-gray-500 dark:text-gray-400">Reference</dt>
              <dd data-testid="merchant-reference" class="font-mono text-xs text-gray-900 dark:text-gray-100">{{ merchant.merchantReference }}</dd>
            </div>
            <div class="flex gap-2">
              <dt class="w-36 shrink-0 font-medium text-gray-500 dark:text-gray-400">Created</dt>
              <dd class="text-gray-900 dark:text-gray-100">{{ new Date(merchant.createdAt).toLocaleString() }}</dd>
            </div>
            <div class="flex gap-2">
              <dt class="w-36 shrink-0 font-medium text-gray-500 dark:text-gray-400">Updated</dt>
              <dd class="text-gray-900 dark:text-gray-100">{{ new Date(merchant.updatedAt).toLocaleString() }}</dd>
            </div>
          </dl>
        </UCard>

        <!-- Status Actions -->
        <UCard v-if="can.canUpdateMerchantStatus">
          <template #header>
            <span class="text-sm font-semibold">Status Actions</span>
          </template>
          <div class="flex flex-wrap gap-2">
            <UButton
              v-if="merchant.status === 'DRAFT'"
              data-testid="action-activate-merchant"
              color="success"
              variant="soft"
              icon="i-lucide-circle-check"
              :loading="activating"
              @click="handleActivate"
            >Activate</UButton>
            <UButton
              v-if="merchant.status === 'ACTIVE'"
              data-testid="action-suspend-merchant"
              color="warning"
              variant="soft"
              icon="i-lucide-pause-circle"
              :loading="suspending"
              @click="handleSuspend"
            >Suspend</UButton>
          </div>
        </UCard>

        <!-- Risk Review -->
        <UCard data-testid="merchant-risk-panel">
          <template #header>
            <span class="text-sm font-semibold">Risk Review</span>
          </template>
          <div class="flex items-center gap-4">
            <UBadge
              data-testid="merchant-risk-status"
              :color="merchant.riskFlagged ? 'error' : 'success'"
              variant="subtle"
              :icon="merchant.riskFlagged ? 'i-lucide-flag' : 'i-lucide-shield-check'"
            >
              {{ merchant.riskFlagged ? 'Risk flagged' : 'No risk flag' }}
            </UBadge>
            <UButton
              v-if="can.canUpdateMerchantRiskFlag"
              data-testid="merchant-risk-toggle"
              :color="merchant.riskFlagged ? 'neutral' : 'error'"
              variant="soft"
              :icon="merchant.riskFlagged ? 'i-lucide-x-circle' : 'i-lucide-flag'"
              :loading="updatingRiskFlag"
              @click="handleRiskFlagToggle"
            >
              {{ merchant.riskFlagged ? 'Clear risk flag' : 'Mark as risk flagged' }}
            </UButton>
          </div>
        </UCard>

        <!-- ETag + Correlation ID -->
        <UCard v-if="responseEtag || responseCorrelationId">
          <template #header>
            <span class="text-sm font-semibold">Response Metadata</span>
          </template>
          <div class="space-y-2 text-sm">
            <EtagDisplay v-if="responseEtag" :etag="responseEtag" />
            <div v-if="responseCorrelationId" class="flex gap-2">
              <span class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Correlation ID</span>
              <span class="font-mono text-xs break-all text-gray-900 dark:text-gray-100">{{ responseCorrelationId }}</span>
            </div>
          </div>
        </UCard>

        <!-- Response Headers -->
        <UCard v-if="hasResponseHeaders">
          <template #header>
            <span class="text-sm font-semibold">Response Headers</span>
          </template>
          <HeaderKeyValuePanel :headers="responseHeaders" />
        </UCard>

        <!-- Link to Payment Orders -->
        <div class="flex">
          <UButton
            data-testid="merchant-payment-orders-link"
            color="primary"
            variant="soft"
            icon="i-lucide-credit-card"
            :to="`/admin/merchants/${merchantId}/payments`"
          >
            View Payment Orders
          </UButton>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'dashboard' })

import type { MerchantResponse } from '~/composables/useMerchantsApi'
import type { ProblemDetails } from '~/types/api'

const route = useRoute()
const merchantId = route.params.merchantId as string

const { getMerchant, activateMerchant, suspendMerchant, updateMerchantRiskFlag } = useMerchantsApi()
const { can } = useAuthorization()
const { success: toastSuccess, error: toastError, warning: toastWarning } = useAppToast()

const merchant = ref<MerchantResponse | null>(null)
const loading = ref(true)
const loadError = ref<string | null>(null)
const loadProblem = ref<ProblemDetails | null>(null)
const activating = ref(false)
const suspending = ref(false)
const updatingRiskFlag = ref(false)

const responseEtag = ref<string | undefined>()
const responseCorrelationId = ref<string | undefined>()
const responseHeaders = ref<Record<string, string>>({})

const hasResponseHeaders = computed(() => Object.keys(responseHeaders.value).length > 0)

async function load() {
  loading.value = true
  loadError.value = null
  loadProblem.value = null

  const response = await getMerchant(merchantId)
  loading.value = false

  if (response.data) {
    merchant.value = response.data
    responseEtag.value = response.headers.etag
    responseCorrelationId.value = response.headers.correlationId
    const h = response.headers
    const headers: Record<string, string> = {}
    if (h.etag) headers['ETag'] = h.etag
    if (h.cacheControl) headers['Cache-Control'] = h.cacheControl
    if (h.vary) headers['Vary'] = h.vary
    if (h.correlationId) headers['X-Correlation-ID'] = h.correlationId
    responseHeaders.value = headers
  } else {
    loadError.value = response.problem?.detail ?? 'Failed to load merchant'
    loadProblem.value = response.problem
  }
}

async function handleActivate() {
  activating.value = true
  const response = await activateMerchant(merchantId)
  activating.value = false
  if (response.data) {
    merchant.value = response.data
    toastSuccess('Merchant activated')
  } else {
    toastError(response.problem?.detail ?? 'Activation failed')
  }
}

async function handleSuspend() {
  suspending.value = true
  const response = await suspendMerchant(merchantId)
  suspending.value = false
  if (response.data) {
    merchant.value = response.data
    toastWarning('Merchant suspended')
  } else {
    toastError(response.problem?.detail ?? 'Suspend failed')
  }
}

async function handleRiskFlagToggle() {
  if (!merchant.value) return
  updatingRiskFlag.value = true
  const newValue = !merchant.value.riskFlagged
  const response = await updateMerchantRiskFlag(merchantId, newValue)
  updatingRiskFlag.value = false
  if (response.data) {
    merchant.value = response.data
    toastSuccess(newValue ? 'Risk flag set' : 'Risk flag cleared')
  } else {
    toastError(response.problem?.detail ?? 'Risk flag update failed')
  }
}

await load()
</script>
