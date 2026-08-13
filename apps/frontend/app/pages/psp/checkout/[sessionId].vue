<template>
  <div data-testid="psp-hosted-checkout" class="flex min-h-screen items-center justify-center bg-gray-50 dark:bg-gray-950">
    <UCard class="w-full max-w-md">
      <template #header>
        <span class="text-sm font-semibold text-gray-500">Hosted checkout (lab PSP)</span>
        <p data-testid="psp-hosted-lang" class="text-xs text-muted">{{ langCopy }}</p>
      </template>
      <LoadingState v-if="loading" message="Loading session…" />
      <ErrorState v-else-if="problem" :problem="problem" />
      <div v-else-if="expired" class="space-y-3" data-testid="psp-link-expired">
        <UAlert color="error" title="Payment link expired" description="Approve is blocked. Clock-based expiry, not a sleep." />
      </div>
      <div v-else-if="session" class="space-y-4">
        <p class="text-sm">
          {{ session.extOrderId }} — {{ session.amountMinor }} {{ session.currency }}
        </p>
        <p v-if="session.validityUntil" class="text-sm">
          Validity:
          <ExpirationCountdown :expires-at="session.validityUntil" />
        </p>
        <p class="text-xs text-muted">No Keycloak cookie on this page. Public hosted DTO omits notifyUrl. Simulate requires Lab-Simulate-Token from GET.</p>
        <div v-if="!outcome" class="flex gap-2">
          <UButton data-testid="psp-approve" color="success" class="flex-1 justify-center" @click="choose('COMPLETED')">
            Approve
          </UButton>
          <UButton data-testid="psp-decline" color="error" variant="outline" class="flex-1 justify-center" @click="choose('CANCELED')">
            Decline
          </UButton>
        </div>
      </div>
      <div v-if="outcome" data-testid="psp-outcome" class="mt-4">
        <UAlert :color="outcome === 'COMPLETED' ? 'success' : 'error'" :title="outcome === 'COMPLETED' ? 'Payment approved' : 'Payment declined'" />
        <UButton v-if="session?.continueUrl" class="mt-3" :to="returnUrl" variant="ghost">
          Return to merchant
        </UButton>
      </div>
      <ApiDebugPanel v-if="debugRequest && debugResponse" class="mt-4" :request="debugRequest" :response="debugResponse" />
    </UCard>
  </div>
</template>

<script setup lang="ts">
import type { ProblemDetails } from '~/types/api'
import type { HostedCheckoutSession } from '~/schemas/checkout-lab.schema'

definePageMeta({ layout: false })

const route = useRoute()
const sessionId = computed(() => String(route.params.sessionId || ''))
const lang = computed(() => String(route.query.lang || 'en').toLowerCase())
const langCopy = computed(() => (lang.value === 'pl' ? 'Język: Polski' : 'Language: English'))
const { getHostedSession, simulate } = useCheckoutLabApi()
const loading = ref(true)
const session = ref<HostedCheckoutSession | null>(null)
const simulateToken = ref<string | null>(null)
const problem = ref<ProblemDetails | null>(null)
const outcome = ref<string | null>(null)
const expired = ref(false)
const debugRequest = ref<{ method: string, path: string, headers: Record<string, string> } | null>(null)
const debugResponse = ref<{ status: number, headers: Record<string, string>, body: string } | null>(null)

const returnUrl = computed(() => {
  const base = session.value?.continueUrl || '/checkout-lab/return'
  const join = base.includes('?') ? '&' : '?'
  return `${base}${join}sessionId=${sessionId.value}&status=${outcome.value === 'COMPLETED' ? 'success' : 'failure'}`
})

async function load() {
  loading.value = true
  debugRequest.value = {
    method: 'GET',
    path: `/api/checkout-lab/hosted/sessions/${sessionId.value}`,
    headers: {},
  }
  const response = await getHostedSession(sessionId.value)
  debugResponse.value = {
    status: response.status,
    headers: {},
    body: response.raw,
  }
  loading.value = false
  if (response.problem) {
    problem.value = response.problem
    return
  }
  session.value = response.data
  simulateToken.value = response.data?.simulateToken ?? null
  if (response.data?.status === 'EXPIRED') {
    expired.value = true
  }
}

async function choose(next: string) {
  debugRequest.value = {
    method: 'POST',
    path: `/api/checkout-lab/hosted/sessions/${sessionId.value}/simulate`,
    headers: {
      'Content-Type': 'application/json',
      'Lab-Simulate-Token': simulateToken.value ? `${simulateToken.value.slice(0, 8)}…` : '(missing)',
    },
  }
  const response = await simulate(sessionId.value, next, simulateToken.value)
  debugResponse.value = {
    status: response.status,
    headers: {},
    body: response.raw,
  }
  if (response.status === 409) {
    expired.value = true
    problem.value = response.problem
    return
  }
  if (response.problem) {
    problem.value = response.problem
    return
  }
  outcome.value = next
  session.value = response.data
  simulateToken.value = response.data?.simulateToken ?? null
}

onMounted(load)
</script>
