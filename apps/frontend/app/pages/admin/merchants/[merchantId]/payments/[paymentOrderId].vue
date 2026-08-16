<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">Payment Order Detail</h2>
      <UButton variant="ghost" :to="`/admin/merchants/${merchantId}/payments`" label="Back to payment orders" />
    </div>

    <UAlert
      v-if="insufficientAuthority"
      color="error"
      variant="subtle"
      title="Insufficient permissions"
      description="You do not have permission to view this payment order."
    />
    <ErrorState
      v-else-if="pageError && !loading"
      :message="pageError"
      :on-retry="handleRetry"
    />
    <LoadingState v-else-if="loading" message="Loading payment order…" />
    <template v-else>
      <PaymentOrderDetail
        v-if="store.currentOrder || (detailResponse && detailResponse.problem)"
        :order="store.currentOrder"
        :history="store.history"
        :api-status="detailResponse?.status"
        :api-headers="detailResponse?.headers"
        :raw-body="detailResponse?.raw"
        :problem="detailResponse?.problem"
      />

      <UCard v-if="store.currentOrder" data-testid="payment-status-polling">
        <template #header>
          <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div class="space-y-1">
              <h3 class="text-base font-semibold">Payment Status</h3>
              <div class="flex items-center gap-2">
                <BusinessStatusBadge
                  :status="store.currentOrder.status"
                  type="payment"
                  data-testid="payment-status-current"
                />
                <span
                  v-if="statusPolling.isRefreshing.value"
                  class="text-xs text-gray-500"
                  data-testid="payment-status-refreshing"
                >
                  Refreshing...
                </span>
              </div>
            </div>

            <div class="flex flex-wrap items-center gap-2">
              <UCheckbox
                :model-value="statusPolling.autoRefreshEnabled.value"
                label="Auto refresh"
                data-testid="payment-status-auto-refresh"
                @update:model-value="statusPolling.setAutoRefresh(Boolean($event))"
              />
              <UButton
                color="neutral"
                variant="outline"
                icon="i-lucide-refresh-cw"
                :loading="statusPolling.isRefreshing.value"
                :disabled="statusPolling.isRefreshing.value"
                data-testid="payment-status-refresh"
                @click="handleStatusRefreshClick"
              >
                Refresh status
              </UButton>
            </div>
          </div>
        </template>

        <div class="space-y-2 text-sm">
          <p class="text-gray-500" data-testid="payment-status-last-checked">
            Last checked:
            <span v-if="statusPolling.lastCheckedAt.value">
              {{ new Date(statusPolling.lastCheckedAt.value).toLocaleString() }}
            </span>
            <span v-else>Not checked yet</span>
          </p>
          <UAlert
            v-if="statusPolling.error.value"
            color="error"
            variant="subtle"
            title="Status refresh failed"
            :description="statusPolling.error.value"
            data-testid="payment-status-error"
          />
        </div>
      </UCard>

      <PaymentOrderLifecycleActions
        v-if="store.currentOrder"
        :payment-order-id="paymentOrderId"
        :merchant-id="merchantId"
        @action-triggered="openDrawer"
      />

      <EvidenceUpload
        v-if="store.currentOrder"
        :payment-order-id="paymentOrderId"
        :merchant-id="merchantId"
      />

      <RefundApprovalsCard
        v-if="store.currentOrder && can.canRunLifecycle"
        :merchant-id="merchantId"
        :payment-order-id="paymentOrderId"
        :etag="store.versionMarker"
        :order-status="store.currentOrder.status"
        @approved="handleRetry"
      />

      <InternalNotes
        v-if="store.currentOrder && can.canReadPaymentNotes"
        :payment-order-id="paymentOrderId"
        :merchant-id="merchantId"
      />
    </template>

    <!-- Lifecycle action drawer -->
    <USlideover
      :open="drawerOpen"
      side="right"
      data-testid="lifecycle-drawer"
      @update:open="onDrawerUpdateOpen"
    >
      <template #content>
        <div class="flex flex-col h-full">
          <!-- Drawer header -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-800">
            <h3 class="text-base font-semibold capitalize">{{ activeAction }} Payment Order</h3>
            <UButton
              color="neutral"
              variant="ghost"
              icon="i-lucide-x"
              size="sm"
              aria-label="Close drawer"
              @click="drawerOpen = false"
            />
          </div>

          <!-- Drawer body -->
          <div class="flex-1 overflow-y-auto px-6 py-4 space-y-4">
            <!-- Idempotency Key -->
            <IdempotencyKeyInput v-model="drawerIdempotencyKey" />

            <!-- If-Match -->
            <IfMatchInput v-model="drawerIfMatch" />

            <!-- Amount (capture + refund only) -->
            <UFormField
              v-if="activeAction === 'capture' || activeAction === 'refund'"
              label="Amount (minor units)"
              :error="amountError"
            >
              <UInput
                v-model.number="drawerAmountMinorRaw"
                type="number"
                min="1"
                max="100000000"
                placeholder="e.g. 1000 = 10.00"
                data-testid="lifecycle-amount-input"
              />
            </UFormField>

            <!-- Reason (all actions, optional) -->
            <UFormField label="Reason (optional)" :hint="drawerReason.length + '/500'">
              <UTextarea
                v-model="drawerReason"
                :maxlength="500"
                placeholder="Optional reason for this action"
                data-testid="lifecycle-reason-input"
                :rows="3"
              />
            </UFormField>

            <!-- Inline problem response -->
            <template v-if="drawerError">
              <HttpStatusBadge :status="drawerError.status" />
              <ProblemDetailsCard :problem="drawerError.problem" />
            </template>
          </div>

          <!-- Drawer footer -->
          <div class="px-6 py-4 border-t border-gray-200 dark:border-gray-800 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="drawerSubmitting"
              @click="drawerOpen = false"
            >
              Cancel
            </UButton>
            <UButton
              color="primary"
              :loading="drawerSubmitting"
              :disabled="drawerSubmitting"
              data-testid="lifecycle-submit-button"
              @click="handleSubmit"
            >
              Submit
            </UButton>
          </div>
        </div>
      </template>
    </USlideover>

    <!-- Confirmation modal for cancel / refund -->
    <ConfirmActionModal
      :open="confirmModalOpen"
      :title="confirmTitle"
      :description="confirmDescription"
      confirm-label="Confirm"
      cancel-label="Go back"
      @confirm="onModalConfirm"
      @cancel="onModalCancel"
      @update:open="confirmModalOpen = $event"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * Payment order detail page.
 * Uses usePaymentOrdersApi().getOrder() directly to capture the full ApiResponse
 * (status, headers, raw body, problem) and passes it to PaymentOrderDetail.
 * Store is still used for shared state (currentOrder, history, versionMarker).
 * Also mounts PaymentOrderLifecycleActions + USlideover drawer for lifecycle mutations.
 *
 * Requirements: 4.1–4.9, 5.1–5.12, 9.6, 12.5
 */

import type { ApiResponse, ProblemDetails } from '~/types/api'
import type { PaymentOrderResponse } from '~/schemas/payment-order.schema'

definePageMeta({
  layout: 'dashboard',
})

const route = useRoute()
const merchantId = route.params.merchantId as string
const paymentOrderId = route.params.paymentOrderId as string

const store = usePaymentOrdersStore()
const { getOrder } = usePaymentOrdersApi()
const { can } = useAuthorization()
const toast = useToast()

const statusPolling = usePaymentStatusPolling(refreshPaymentStatus)

// ---------------------------------------------------------------------------
// Page-level state
// ---------------------------------------------------------------------------

const loading = ref(false)
const pageError = ref<string | null>(null)
const insufficientAuthority = ref(false)

// Full ApiResponse captured for HTTP/Raw/Problem panels
const detailResponse = ref<ApiResponse<PaymentOrderResponse> | null>(null)

// ---------------------------------------------------------------------------
// Drawer state
// ---------------------------------------------------------------------------

const drawerOpen = ref(false)
const activeAction = ref<'authorize' | 'capture' | 'cancel' | 'refund' | null>(null)
const drawerIdempotencyKey = ref('')
const drawerIfMatch = ref('')
/** Raw string from number input; converted to number on submit */
const drawerAmountMinorRaw = ref<number | null>(null)
const drawerReason = ref('')
const drawerSubmitting = ref(false)
const drawerError = ref<{ problem: ProblemDetails; status: number } | null>(null)

// ---------------------------------------------------------------------------
// Confirm modal state
// ---------------------------------------------------------------------------

const confirmModalOpen = ref(false)

const confirmTitle = computed<string>(() => {
  if (activeAction.value === 'cancel') return 'Confirm Cancel'
  if (activeAction.value === 'refund') return 'Confirm Refund'
  return 'Confirm Action'
})

const confirmDescription = computed<string>(() => {
  if (activeAction.value === 'cancel') return 'Are you sure you want to cancel this payment order? This action cannot be undone.'
  if (activeAction.value === 'refund') return 'Are you sure you want to refund this payment order? This action cannot be undone.'
  return 'Are you sure you want to proceed with this action?'
})

// ---------------------------------------------------------------------------
// Validation
// ---------------------------------------------------------------------------

const amountError = computed<string | undefined>(() => {
  const v = drawerAmountMinorRaw.value
  if (v === null || v === undefined) return undefined
  if (!Number.isInteger(v)) return 'Amount must be a whole number'
  if (v < 1) return 'Amount must be at least 1'
  if (v > 100_000_000) return 'Amount must be at most 100,000,000'
  return undefined
})

function validateDrawer(): boolean {
  if (!drawerIdempotencyKey.value) {
    toast.add({ title: 'Validation error', description: 'Idempotency key is required', color: 'error' })
    return false
  }
  if (drawerIdempotencyKey.value.length > 255) {
    toast.add({ title: 'Validation error', description: 'Idempotency key must not exceed 255 characters', color: 'error' })
    return false
  }
  if ((activeAction.value === 'capture' || activeAction.value === 'refund') && drawerAmountMinorRaw.value !== null) {
    const v = drawerAmountMinorRaw.value
    if (!Number.isInteger(v) || v < 1 || v > 100_000_000) {
      toast.add({ title: 'Validation error', description: 'Amount must be an integer between 1 and 100,000,000', color: 'error' })
      return false
    }
  }
  return true
}

// ---------------------------------------------------------------------------
// Drawer open/close
// ---------------------------------------------------------------------------

function openDrawer(action: string, amountMinor: number | null = null) {
  activeAction.value = action as typeof activeAction.value
  drawerIdempotencyKey.value = crypto.randomUUID()
  drawerIfMatch.value = store.versionMarker ?? ''
  drawerAmountMinorRaw.value = amountMinor
  drawerReason.value = ''
  drawerError.value = null
  drawerOpen.value = true
}

/** Handle USlideover @update:open — only allow close when not in the middle of submitting */
function onDrawerUpdateOpen(val: boolean) {
  if (!val && drawerSubmitting.value) return
  drawerOpen.value = val
}

// ---------------------------------------------------------------------------
// Submit flow
// ---------------------------------------------------------------------------

function handleSubmit() {
  if (!validateDrawer()) return

  if (activeAction.value === 'cancel' || activeAction.value === 'refund') {
    // Gate with confirmation modal
    confirmModalOpen.value = true
  } else {
    // authorize / capture proceed immediately
    executeAction()
  }
}

function onModalConfirm() {
  confirmModalOpen.value = false
  executeAction()
}

function onModalCancel() {
  // Dismiss modal, keep drawer open with all entered values intact
  confirmModalOpen.value = false
}

async function executeAction() {
  drawerSubmitting.value = true
  drawerError.value = null

  const { authorizeOrder, captureOrder, cancelOrder, refundOrder } = usePaymentLifecycleApi()
  let response: ApiResponse<PaymentOrderResponse> | undefined

  try {
    const key = drawerIdempotencyKey.value
    const ifMatch = drawerIfMatch.value
    const reason = drawerReason.value || undefined
    const amountMinor = drawerAmountMinorRaw.value ?? undefined

    if (activeAction.value === 'authorize') {
      response = await authorizeOrder(merchantId, paymentOrderId, ifMatch, key)
    } else if (activeAction.value === 'capture') {
      response = await captureOrder(merchantId, paymentOrderId, ifMatch, key, { amountMinor, reason })
    } else if (activeAction.value === 'cancel') {
      response = await cancelOrder(merchantId, paymentOrderId, ifMatch, key, { reason })
    } else if (activeAction.value === 'refund') {
      response = await refundOrder(merchantId, paymentOrderId, ifMatch, key, { amountMinor, reason })
    }

    if (!response) return

    if (response.problem && !response.data) {
      // Problem: show inline in drawer, retain idempotencyKey and ifMatch
      drawerError.value = { problem: response.problem, status: response.status }
      toast.add({
        title: `${activeAction.value} failed`,
        description: response.problem.detail ?? response.problem.title ?? 'Action failed',
        color: 'error',
      })
    } else if (response.data) {
      // Success: update store, show toast, close drawer
      store.currentOrder = response.data
      store.versionMarker = response.headers.etag || response.data.versionMarker || null
      await store.loadHistory(merchantId, paymentOrderId)

      const newStatus = response.data.status
      const newEtag = response.headers.etag ?? store.versionMarker ?? ''
      toast.add({
        title: `${activeAction.value} succeeded`,
        description: `New status: ${newStatus}${newEtag ? ` · ETag: ${newEtag}` : ''}`,
        color: 'success',
      })
      drawerOpen.value = false
    }
  } finally {
    drawerSubmitting.value = false
  }
}

async function refreshPaymentStatus() {
  const response = await getOrder(merchantId, paymentOrderId)
  detailResponse.value = response

  if (response.data) {
    store.currentOrder = response.data
    store.versionMarker = response.headers.etag || response.data.versionMarker || store.versionMarker
    if (['CANCELLED', 'EXPIRED', 'REFUNDED'].includes(response.data.status)) {
      statusPolling.stop()
    }
    return { status: response.data.status }
  }

  if (response.problem) {
    throw new Error(response.problem.detail ?? response.problem.title ?? 'Status refresh failed.')
  }

  throw new Error('Status refresh returned no payment order data.')
}

function handleStatusRefreshClick() {
  void statusPolling.refresh()
}

// ---------------------------------------------------------------------------
// Page load
// ---------------------------------------------------------------------------

async function loadAll() {
  loading.value = true
  pageError.value = null
  insufficientAuthority.value = false

  try {
    // Run detail fetch and history load in parallel
    const [response] = await Promise.all([
      getOrder(merchantId, paymentOrderId),
      store.loadHistory(merchantId, paymentOrderId),
    ])

    // Capture the full response for HTTP/Raw/Problem panels
    detailResponse.value = response

    if (response.data) {
      // Success — update the store's shared state
      store.currentOrder = response.data
      store.versionMarker = response.headers.etag || response.data.versionMarker || null
    } else if (response.problem) {
      // Problem response (4xx/5xx) — clear currentOrder, surface through panels
      store.currentOrder = null
      if (response.status === 403) {
        insufficientAuthority.value = true
      } else if (response.status !== 404 && response.status < 500) {
        // For non-404 client errors let the detail panel show the problem card
      } else {
        pageError.value = response.problem.detail ?? response.problem.title ?? 'Failed to load payment order.'
      }
    }
  } catch (e: unknown) {
    const err = e as { statusCode?: number; message?: string; statusMessage?: string }
    if (err?.statusCode === 403) {
      insufficientAuthority.value = true
    } else {
      pageError.value = err?.statusMessage || err?.message || 'Failed to load payment order.'
    }
  } finally {
    loading.value = false
  }
}

async function handleRetry() {
  store.currentOrder = null
  detailResponse.value = null
  await loadAll()
}

onMounted(loadAll)
</script>
