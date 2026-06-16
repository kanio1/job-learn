<template>
  <div>
    <LoadingState v-if="loading" message="Loading merchant details…" />

    <ErrorState
      v-else-if="error"
      :problem="errorProblem"
      :message="errorMessage"
      :on-retry="reload"
    />

    <UCard v-else-if="merchant">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-sm font-semibold">Merchant</span>
          <BusinessStatusBadge :status="merchant.status" type="merchant" />
        </div>
      </template>

      <dl class="space-y-2 text-sm">
        <div class="flex gap-2">
          <dt class="w-32 shrink-0 font-medium text-gray-500 dark:text-gray-400">Reference</dt>
          <dd class="font-mono text-gray-900 dark:text-gray-100">{{ merchant.merchantReference }}</dd>
        </div>
        <div class="flex gap-2">
          <dt class="w-32 shrink-0 font-medium text-gray-500 dark:text-gray-400">Display Name</dt>
          <dd class="text-gray-900 dark:text-gray-100">{{ merchant.displayName }}</dd>
        </div>
        <div class="flex gap-2">
          <dt class="w-32 shrink-0 font-medium text-gray-500 dark:text-gray-400">ID</dt>
          <dd class="font-mono text-xs text-gray-600 dark:text-gray-400 break-all">{{ merchant.merchantId }}</dd>
        </div>
      </dl>
    </UCard>
  </div>
</template>

<script setup lang="ts">
/**
 * Fetches and displays merchant business fields using useMerchantsApi.
 * Shows LoadingState while fetching and ErrorState on failure.
 *
 * Requirements: 2.8
 */

import type { ProblemDetails } from '~/types/api'
import type { MerchantResponse } from '~/composables/useMerchantsApi'

const props = defineProps<{
  merchantId: string
}>()

const loading = ref(false)
const merchant = ref<MerchantResponse | null>(null)
const errorProblem = ref<ProblemDetails | null>(null)
const errorMessage = ref<string | undefined>(undefined)

const error = computed(() => errorProblem.value !== null || errorMessage.value !== undefined)

async function reload() {
  loading.value = true
  errorProblem.value = null
  errorMessage.value = undefined

  try {
    const { getMerchant } = useMerchantsApi()
    const response = await getMerchant(props.merchantId)

    if (response.data) {
      merchant.value = response.data
    } else {
      errorProblem.value = response.problem
      if (!response.problem) {
        errorMessage.value = 'Failed to load merchant details.'
      }
    }
  } catch {
    errorMessage.value = 'Failed to load merchant details.'
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>
