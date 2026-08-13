<template>
  <UDashboardPanel id="checkout-lab-inspector">
    <template #header>
      <UDashboardNavbar title="Event Inspector">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>
    <template #body>
      <div class="space-y-4 p-1">
        <UAlert
          color="info"
          variant="subtle"
          title="Session vs fulfillment dictionaries"
          description="PSP session uses CANCELED. Merchant fulfillment uses CANCELLED. That difference is intentional."
        />
        <UFormField label="Session id">
          <div class="flex gap-2">
            <UInput v-model="sessionId" class="flex-1" data-testid="inspector-session-id" />
            <UButton @click="load" data-testid="inspector-load">Load</UButton>
          </div>
        </UFormField>
        <LoadingState v-if="loading" message="Loading events…" />
        <ErrorState v-else-if="problem" :problem="problem" />
        <EmptyStateCard v-else-if="events.length === 0 && deliveries.length === 0 && anomalies.length === 0" description="Create an online booking and Approve on hosted checkout." />
        <UCard v-if="anomalies.length > 0">
          <template #header>
            <span class="font-semibold">Reconcile anomalies</span>
          </template>
          <div class="space-y-2">
            <div v-for="anomaly in anomalies" :key="anomaly.anomalyId" class="text-sm">
              <UBadge color="warning">{{ anomaly.kind }}</UBadge>
              <span class="ml-2">{{ anomaly.detail }}</span>
            </div>
          </div>
        </UCard>
        <UCard v-for="event in events" :key="event.eventId" class="space-y-2">
          <div class="flex gap-2 items-center flex-wrap">
            <UBadge data-testid="inspector-process-status">{{ event.processStatus }}</UBadge>
            <UBadge data-testid="inspector-event-type" color="neutral" variant="subtle">{{ event.eventType }}</UBadge>
            <UBadge v-if="event.ackStatus" color="neutral" variant="subtle">ACK {{ event.ackStatus }}</UBadge>
            <span class="font-mono text-xs">{{ event.eventId }}</span>
            <span class="text-xs text-muted">attempts={{ event.attempts }}</span>
          </div>
          <p v-if="event.lastError" class="text-xs text-error">lastError: {{ event.lastError }}</p>
          <HeaderKeyValuePanel
            data-testid="inspector-signature-panel"
            :headers="{ 'Lab-Signature': event.signatureHeader || '', 'Lab-Event-Id': event.eventId }"
          />
          <RawJsonViewer :content="JSON.stringify(event.payload, null, 2)" />
        </UCard>
        <UCard v-if="deliveries.length > 0">
          <template #header>
            <span class="font-semibold">Notify delivery attempts</span>
          </template>
          <div class="space-y-2">
            <div v-for="delivery in deliveries" :key="`${delivery.eventId}-${delivery.attempt}`" class="text-sm font-mono">
              {{ delivery.eventId }} attempt={{ delivery.attempt }} status={{ delivery.responseStatus }}
            </div>
          </div>
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
import type { CheckoutEvent, Fulfillment } from '~/schemas/checkout-lab.schema'
import type { ProblemDetails } from '~/types/api'

type Delivery = {
  eventId: string
  attempt: number
  responseStatus: number
  at: string
}

type AnomalyRow = {
  anomalyId: string
  sessionId?: string | null
  kind: string
  detail: string
  detectedAt: string
}

definePageMeta({ layout: 'dashboard' })

const sessionId = ref('')
const events = ref<CheckoutEvent[]>([])
const deliveries = ref<Delivery[]>([])
const anomalies = ref<AnomalyRow[]>([])
const loading = ref(false)
const problem = ref<ProblemDetails | null>(null)
const { listEvents, listDeliveries, listAnomalies } = useCheckoutLabApi()

async function load() {
  if (!sessionId.value) return
  loading.value = true
  problem.value = null
  events.value = []
  deliveries.value = []

  const [eventsResponse, deliveriesResponse, anomaliesResponse] = await Promise.all([
    listEvents(sessionId.value),
    listDeliveries(sessionId.value),
    listAnomalies(),
  ])
  loading.value = false

  if (eventsResponse.problem) {
    problem.value = eventsResponse.problem
    return
  }
  events.value = eventsResponse.data ?? []
  deliveries.value = deliveriesResponse.data ?? []
  anomalies.value = (anomaliesResponse.data ?? []).filter(
    anomaly => !anomaly.sessionId || anomaly.sessionId === sessionId.value,
  )
}
</script>
