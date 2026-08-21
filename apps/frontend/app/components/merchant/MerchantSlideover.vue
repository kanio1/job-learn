<template>
  <USlideover
    v-model:open="open"
    title="Merchant 360"
    :description="merchant?.merchantReference ?? 'Merchant details'"
  >
    <template #content>
      <div data-testid="merchant-360-slideover" class="flex h-full flex-col">
        <div class="flex items-start justify-between gap-4 border-b border-default p-6">
          <div>
            <h2 class="text-lg font-semibold text-highlighted">Merchant 360</h2>
            <p v-if="merchant" class="mt-1 text-sm text-muted">
              {{ merchant.displayName }} · {{ merchant.merchantReference }}
            </p>
          </div>
          <UButton
            color="neutral"
            variant="ghost"
            icon="i-lucide-x"
            aria-label="Close"
            @click="open = false"
          />
        </div>

        <div class="flex-1 space-y-4 overflow-y-auto p-6">
          <LoadingState v-if="loading" message="Loading merchant…" />

          <ErrorState
            v-else-if="loadError"
            :problem="loadProblem"
            :message="loadError"
            :on-retry="load"
          />

          <template v-else-if="merchant">
            <section class="space-y-2">
              <h3 class="text-sm font-semibold text-highlighted">Information</h3>
              <dl class="space-y-2 text-sm">
                <div class="flex gap-2">
                  <dt class="w-28 shrink-0 font-medium text-muted">Name</dt>
                  <dd data-testid="merchant-360-name">{{ merchant.displayName }}</dd>
                </div>
                <div class="flex gap-2">
                  <dt class="w-28 shrink-0 font-medium text-muted">Reference</dt>
                  <dd data-testid="merchant-360-reference" class="font-mono text-xs">{{ merchant.merchantReference }}</dd>
                </div>
                <div class="flex gap-2">
                  <dt class="w-28 shrink-0 font-medium text-muted">Status</dt>
                  <dd>
                    <MerchantStatusBadge :status="merchant.status" data-testid="merchant-360-status" />
                  </dd>
                </div>
                <div v-if="etag" class="pt-1">
                  <EtagDisplay :etag="etag" />
                </div>
              </dl>
              <div class="flex flex-wrap gap-2 pt-1">
                <UButton
                  v-if="canReadPayments"
                  data-testid="merchant-360-payments-link"
                  color="primary"
                  variant="soft"
                  icon="i-lucide-credit-card"
                  :to="`/admin/merchants/${merchant.merchantId}/payments`"
                >
                  View payment orders
                </UButton>
                <UButton
                  color="neutral"
                  variant="ghost"
                  :to="`/admin/merchants/${merchant.merchantId}`"
                >
                  Open merchant
                </UButton>
                <UButton
                  v-if="canUpdateMerchantStatus && merchant.status === 'ACTIVE'"
                  data-testid="merchant-360-suspend"
                  color="warning"
                  variant="soft"
                  icon="i-lucide-pause-circle"
                  :aria-label="`Suspend ${merchant.merchantReference}`"
                  @click="confirmSuspendOpen = true"
                >
                  Suspend
                </UButton>
              </div>
            </section>

            <ErrorState
              v-if="actionProblem"
              :problem="actionProblem"
              retry-label="Reload"
              :on-retry="reloadAfterConflict"
            />

            <section class="space-y-2">
              <h3 class="text-sm font-semibold text-highlighted">Risk</h3>
              <UBadge
                data-testid="merchant-360-risk"
                :color="merchant.riskFlagged ? 'error' : 'success'"
                variant="subtle"
                :icon="merchant.riskFlagged ? 'i-lucide-flag' : 'i-lucide-shield-check'"
              >
                {{ merchant.riskFlagged ? 'Risk flagged' : 'No risk flag' }}
              </UBadge>
            </section>

            <section class="space-y-2">
              <h3 class="text-sm font-semibold text-highlighted">Payments</h3>
              <p v-if="orders.length === 0" class="text-sm text-muted">No payment orders</p>
              <ul v-else class="space-y-1 text-sm">
                <li
                  v-for="order in orders"
                  :key="order.paymentOrderId"
                  data-testid="merchant-360-payment-row"
                >
                  <UButton
                    variant="link"
                    color="primary"
                    size="sm"
                    class="p-0"
                    :to="`/admin/merchants/${merchant.merchantId}/payments/${order.paymentOrderId}`"
                    :aria-label="`Open payment ${order.clientOrderReference}`"
                  >
                    {{ order.clientOrderReference }} · {{ order.status }} · {{ order.amountMinor }} {{ order.currency }}
                  </UButton>
                </li>
              </ul>
            </section>

            <section class="space-y-2">
              <h3 class="text-sm font-semibold text-highlighted">Notes</h3>
              <p v-if="notes.length === 0" class="text-sm text-muted">No internal notes</p>
              <ul v-else class="space-y-1 text-sm">
                <li
                  v-for="note in notes"
                  :key="note.id"
                  data-testid="merchant-360-note"
                >
                  {{ note.body }}
                </li>
              </ul>
            </section>

            <section class="space-y-2">
              <h3 class="text-sm font-semibold text-highlighted">History</h3>
              <MerchantAuditTimeline
                :merchant="merchant"
                :events="auditEvents"
                :payment-history="paymentHistory"
              />
              <UButton
                variant="link"
                color="primary"
                size="sm"
                to="/admin/audit?targetType=MERCHANT"
              >
                Open audit
              </UButton>
            </section>
          </template>
        </div>
      </div>
    </template>
  </USlideover>

  <ConfirmActionModal
    :open="confirmSuspendOpen"
    title="Suspend merchant"
    description="Suspend this merchant? Payment creation will be blocked until it is active again."
    confirm-label="Suspend"
    @update:open="confirmSuspendOpen = $event"
    @confirm="handleSuspend"
  />
</template>

<script setup lang="ts">
import type { MerchantResponse } from '~/composables/useMerchantsApi'
import type { PaymentOrderResponse } from '~/composables/usePaymentOrdersApi'
import type { AuditEvent } from '~/schemas/audit.schema'
import type { ProblemDetails } from '~/types/api'
import { merchantIfMatch } from '~/utils/merchant-etag'

const props = defineProps<{
  merchantId: string | null
}>()

const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{
  updated: [merchant: MerchantResponse]
}>()

const { getMerchant, suspendMerchant } = useMerchantsApi()
const { listOrders } = usePaymentOrdersApi()
const { getHistory } = usePaymentLifecycleApi()
const { listNotes } = usePaymentNotesApi()
const { list: listAudit } = useAuditApi()
const { can } = useAuthorization()
const toast = useToast()

const merchant = ref<MerchantResponse | null>(null)
const orders = ref<PaymentOrderResponse[]>([])
const auditEvents = ref<AuditEvent[]>([])
const notes = ref<Array<{ id: string, body: string }>>([])
const paymentHistory = ref<Array<{ occurredAt: string, title: string, description: string }>>([])
const loading = ref(false)
const loadError = ref<string | null>(null)
const loadProblem = ref<ProblemDetails | null>(null)
const etag = ref<string | undefined>()
const confirmSuspendOpen = ref(false)
const actionProblem = ref<ProblemDetails | null>(null)

const canUpdateMerchantStatus = computed(() => can.value.canUpdateMerchantStatus)
const canReadPayments = computed(() =>
  can.value.canReadMerchantPayments || can.value.canReadPlatformPayments,
)

async function load() {
  if (!props.merchantId) {
    return
  }
  loading.value = true
  loadError.value = null
  loadProblem.value = null
  merchant.value = null
  orders.value = []
  auditEvents.value = []
  notes.value = []
  paymentHistory.value = []
  etag.value = undefined
  actionProblem.value = null
  await nextTick()

  const [merchantResponse, ordersResponse, auditResponse] = await Promise.all([
    getMerchant(props.merchantId),
    listOrders(props.merchantId, { page: 0, size: 5, sort: 'createdAt,desc' }),
    can.value.canViewAuditLog
      ? listAudit({ targetType: 'MERCHANT', size: 100 })
      : Promise.resolve({ data: null }),
  ])

  if (merchantResponse.data) {
    merchant.value = merchantResponse.data
    etag.value = merchantResponse.headers.etag
  } else {
    loadProblem.value = merchantResponse.problem
    loadError.value =
      merchantResponse.problem?.detail
      || merchantResponse.problem?.title
      || 'Failed to load merchant.'
  }

  if (ordersResponse.data) {
    orders.value = ordersResponse.data.content
    const historyEntries: Array<{ occurredAt: string, title: string, description: string }> = []
    const noteEntries: Array<{ id: string, body: string }> = []
    await Promise.all(orders.value.map(async (order) => {
      const history = await getHistory(props.merchantId!, order.paymentOrderId)
      for (const entry of history.data?.content ?? []) {
        historyEntries.push({
          occurredAt: entry.createdAt,
          title: entry.toStatus,
          description: `${order.clientOrderReference} · ${entry.action ?? 'transition'}`,
        })
      }
      if (can.value.canReadPaymentNotes) {
        const listedNotes = await listNotes(props.merchantId!, order.paymentOrderId)
        for (const note of listedNotes.data ?? []) {
          noteEntries.push({ id: note.id, body: note.body })
        }
      }
    }))
    paymentHistory.value = historyEntries
    notes.value = noteEntries
  }

  if (auditResponse.data) {
    auditEvents.value = auditResponse.data.content.filter(event => event.targetId === props.merchantId)
  }

  loading.value = false
}

async function reloadAfterConflict() {
  actionProblem.value = null
  await load()
}

async function handleSuspend() {
  if (!props.merchantId || !merchant.value) {
    return
  }
  const response = await suspendMerchant(
    props.merchantId,
    merchantIfMatch(etag.value, merchant.value.version),
  )
  if (response.data) {
    merchant.value = response.data
    if (response.headers.etag) {
      etag.value = response.headers.etag
    }
    actionProblem.value = null
    toast.add({ title: `${response.data.merchantReference} suspended`, color: 'warning' })
    emit('updated', response.data)
  } else if (response.status === 412) {
    actionProblem.value = response.problem
  } else {
    toast.add({
      title: 'Suspension failed',
      description:
        response.problem?.detail
        || response.problem?.title
        || 'Failed to suspend merchant.',
      color: 'error',
    })
  }
}

watch([open, () => props.merchantId], ([isOpen, merchantId]) => {
  if (isOpen && merchantId) {
    void load()
  }
  if (!isOpen) {
    confirmSuspendOpen.value = false
  }
})
</script>
