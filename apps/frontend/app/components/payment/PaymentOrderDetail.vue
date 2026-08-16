<template>
  <div data-testid="payment-order-detail" class="space-y-6">
    <UTabs :items="tabItems" class="w-full">
      <!-- Business tab -->
      <template #business>
        <div class="space-y-4 pt-4">
          <ProblemDetailsCard v-if="problem" :problem="problem" />

          <UCard v-if="order && !problem">
            <template #header>
              <div class="flex items-center justify-between">
                <h3 class="text-base font-semibold">Payment Order</h3>
                <BusinessStatusBadge :status="order.status" type="payment" />
              </div>
            </template>

            <dl class="space-y-3 text-sm">
              <div class="flex justify-between">
                <dt class="text-gray-500">Payment Order ID</dt>
                <dd class="font-mono">{{ order.paymentOrderId }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Merchant ID</dt>
                <dd class="font-mono">{{ order.merchantId }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Amount</dt>
                <dd>{{ order.amountMinor }} minor units</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Currency</dt>
                <dd>{{ order.currency }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Client Reference</dt>
                <dd class="flex items-center gap-2">
                  <span>{{ order.clientOrderReference }}</span>
                  <UButton
                    size="xs"
                    variant="ghost"
                    data-testid="copy-payment-reference"
                    aria-label="Copy payment reference"
                    @click="copyText(order.clientOrderReference)"
                  >
                    Copy
                  </UButton>
                </dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Captured Amount</dt>
                <dd>{{ order.capturedAmountMinor != null ? `${order.capturedAmountMinor} minor units` : '—' }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Refunded Amount</dt>
                <dd>{{ order.refundedAmountMinor != null ? `${order.refundedAmountMinor} minor units` : '—' }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Authorized At</dt>
                <dd>{{ order.authorizedAt ? new Date(order.authorizedAt).toLocaleString() : '—' }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Expires At</dt>
                <dd>{{ order.expiresAt ? new Date(order.expiresAt).toLocaleString() : '—' }}</dd>
              </div>
              <div v-if="order.status === 'AUTHORIZED' && order.expiresAt" class="flex justify-between">
                <dt class="text-gray-500">Authorization Window</dt>
                <dd><ExpirationCountdown :expires-at="order.expiresAt" /></dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Captured At</dt>
                <dd>{{ order.capturedAt ? new Date(order.capturedAt).toLocaleString() : '—' }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Cancelled At</dt>
                <dd>{{ order.cancelledAt ? new Date(order.cancelledAt).toLocaleString() : '—' }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Cancellation Reason</dt>
                <dd>{{ order.cancellationReason ?? '—' }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Refunded At</dt>
                <dd>{{ order.refundedAt ? new Date(order.refundedAt).toLocaleString() : '—' }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Refund Reason</dt>
                <dd>{{ order.refundReason ?? '—' }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Created At</dt>
                <dd>{{ new Date(order.createdAt).toLocaleString() }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-gray-500">Updated At</dt>
                <dd>{{ new Date(order.updatedAt).toLocaleString() }}</dd>
              </div>
            </dl>
          </UCard>
        </div>
      </template>

      <!-- HTTP tab -->
      <template #http>
        <div class="space-y-4 pt-4">
          <div class="flex items-center gap-3">
            <span class="text-sm font-medium text-gray-500 dark:text-gray-400">Status</span>
            <HttpStatusBadge v-if="apiStatus != null" :status="apiStatus" />
            <span v-else class="text-sm text-gray-400">—</span>
          </div>
          <EtagDisplay :etag="apiHeaders?.etag" />
          <HeaderKeyValuePanel :headers="displayHeaders" />
        </div>
      </template>

      <!-- Raw tab -->
      <template #raw>
        <div class="pt-4">
          <RawJsonViewer :content="rawBody ?? ''" />
        </div>
      </template>

      <!-- History tab -->
      <template #history>
        <div class="space-y-4 pt-4">
          <p
            v-if="!sortedHistory || sortedHistory.length === 0"
            class="text-sm text-gray-400 italic"
          >
            No lifecycle history recorded.
          </p>

          <div v-else class="space-y-3">
            <div
              v-for="entry in sortedHistory"
              :key="entry.statusHistoryId"
              class="flex items-start gap-3 text-sm border-l-2 border-gray-200 dark:border-gray-700 pl-3"
            >
              <div class="flex-1 space-y-1">
                <div class="flex items-center gap-2 flex-wrap">
                  <BusinessStatusBadge v-if="entry.fromStatus" :status="entry.fromStatus" type="payment" />
                  <span v-else class="text-xs text-gray-400">—</span>
                  <span class="text-gray-400">→</span>
                  <BusinessStatusBadge :status="entry.toStatus" type="payment" />
                </div>
                <div class="text-xs text-gray-500 dark:text-gray-400 space-x-1">
                  <span>{{ entry.action ?? '—' }}</span>
                  <span>·</span>
                  <span>{{ entry.actorDisplay || 'System' }}</span>
                  <span>·</span>
                  <span>{{ new Date(entry.createdAt).toLocaleString() }}</span>
                  <template v-if="entry.correlationId">
                    <span>·</span>
                    <span class="font-mono">{{ entry.correlationId }}</span>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </UTabs>
  </div>
</template>

<script setup lang="ts">
/**
 * Payment order detail with UTabs: Business | HTTP | Raw | History.
 *
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 7.1–7.8, 12.5
 */

import type { ApiHeaders, ProblemDetails } from '~/types/api'

const props = defineProps<{
  order: {
    paymentOrderId: string
    merchantId: string
    clientOrderReference: string
    amountMinor: number
    currency: string
    status: string
    capturedAmountMinor?: number | null
    refundedAmountMinor?: number | null
    authorizedAt?: string | null
    expiresAt?: string | null
    capturedAt?: string | null
    cancelledAt?: string | null
    refundedAt?: string | null
    cancellationReason?: string | null
    refundReason?: string | null
    createdAt: string
    updatedAt: string
    versionMarker?: string | null
  } | null
  history?: Array<{
    availableActions?: string[]
    statusHistoryId: string
    paymentOrderId: string
    fromStatus: string | null
    toStatus: string
    action: string | null
    actorDisplay?: string | null
    reason?: string | null
    amountMinor?: number | null
    pspReference?: string | null
    correlationId?: string | null
    createdAt: string
  }> | null
  // New optional props for HTTP/Raw/Problem display
  apiStatus?: number
  apiHeaders?: ApiHeaders
  rawBody?: string
  problem?: ProblemDetails | null
}>()

async function copyText(value: string) {
  await navigator.clipboard.writeText(value)
}

const tabItems = [
  { label: 'Business', slot: 'business' as const },
  { label: 'HTTP', slot: 'http' as const },
  { label: 'Raw', slot: 'raw' as const },
  { label: 'History', slot: 'history' as const },
]

/**
 * Build a filtered headers object from ApiHeaders for display in HeaderKeyValuePanel.
 * Only shows headers that are relevant and present, with human-readable keys.
 * Requirements: 4.2, 4.5
 */
const displayHeaders = computed<Record<string, string>>(() => {
  const h = props.apiHeaders
  if (!h) return {}
  const result: Record<string, string> = {}
  if (h.etag) result['ETag'] = h.etag
  if (h.lastModified) result['Last-Modified'] = h.lastModified
  if (h.idempotencyReplayed) result['Idempotency-Replayed'] = h.idempotencyReplayed
  if (h.vary) result['Vary'] = h.vary
  if (h.cacheControl) result['Cache-Control'] = h.cacheControl
  if (h.correlationId) result['X-Correlation-ID'] = h.correlationId
  return result
})

/**
 * Sort history entries ascending by createdAt (oldest first).
 * Requirements: 4.7, 7.1
 */
const sortedHistory = computed(() => {
  if (!props.history || props.history.length === 0) return []
  return [...props.history].sort(
    (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
  )
})
</script>
