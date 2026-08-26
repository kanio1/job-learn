<template>
  <div data-testid="payment-kanban" class="grid gap-3 md:grid-cols-3 xl:grid-cols-6">
    <section
      v-for="status in stages"
      :key="status"
      :data-testid="`stage-${status}`"
      class="min-h-48 rounded-lg border border-default bg-elevated/40 p-2"
      @dragover.prevent
      @drop.prevent="onDrop($event, status)"
    >
      <h3 class="mb-2 text-xs font-semibold uppercase text-muted">{{ status }}</h3>
      <article
        v-for="order in ordersByStatus[status]"
        :key="order.paymentOrderId"
        :data-testid="`payment-card-${order.paymentOrderId}`"
        draggable="true"
        class="mb-2 rounded-md border border-default bg-default p-2 text-sm"
        @dragstart="onDragStart($event, order.paymentOrderId)"
      >
        <p class="font-mono text-xs">{{ order.clientOrderReference }}</p>
        <p class="text-xs text-muted">{{ order.amountMinor }} {{ order.currency }}</p>
        <UDropdownMenu
          v-if="moveItems(order).length > 0"
          :items="moveItems(order)"
        >
          <UButton
            size="xs"
            color="neutral"
            variant="ghost"
            :aria-label="`Move ${order.clientOrderReference}`"
          >
            Move
          </UButton>
        </UDropdownMenu>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { DropdownMenuItem } from '@nuxt/ui'
import type { PaymentOrderResponse } from '~/schemas/payment-order.schema'

type PaymentStatus = PaymentOrderResponse['status']

const props = defineProps<{
  merchantId: string
  orders: PaymentOrderResponse[]
}>()

const emit = defineEmits<{
  updated: [order: PaymentOrderResponse]
  rollback: [order: PaymentOrderResponse]
}>()

const stages: PaymentStatus[] = ['CREATED', 'AUTHORIZED', 'CAPTURED', 'CANCELLED', 'EXPIRED', 'REFUNDED']
const { getOrder } = usePaymentOrdersApi()
const { authorizeOrder, captureOrder, cancelOrder } = usePaymentLifecycleApi()
const toast = useToast()
const etags = ref<Record<string, string>>({})

const ordersByStatus = computed(() => {
  const grouped: Record<PaymentStatus, PaymentOrderResponse[]> = {
    CREATED: [],
    AUTHORIZED: [],
    CAPTURED: [],
    CANCELLED: [],
    EXPIRED: [],
    REFUNDED: [],
  }
  for (const order of props.orders) {
    grouped[order.status].push(order)
  }
  return grouped
})

watch(
  () => props.orders.map(order => order.paymentOrderId).join(','),
  async () => {
    await Promise.all(props.orders.map(async (order) => {
      if (etags.value[order.paymentOrderId]) {
        return
      }
      const response = await getOrder(props.merchantId, order.paymentOrderId)
      if (response.headers.etag) {
        etags.value = { ...etags.value, [order.paymentOrderId]: response.headers.etag }
      }
    }))
  },
  { immediate: true },
)

function moveItems(order: PaymentOrderResponse): DropdownMenuItem[] {
  const targets: PaymentStatus[] = []
  if (order.status === 'CREATED') {
    targets.push('AUTHORIZED', 'CANCELLED')
  } else if (order.status === 'AUTHORIZED') {
    targets.push('CAPTURED', 'CANCELLED')
  }
  return targets.map(status => ({
    label: `Move to ${status}`,
    onSelect: () => {
      void moveOrder(order, status)
    },
  }))
}

function onDragStart(event: DragEvent, paymentOrderId: string) {
  event.dataTransfer?.setData('text/plain', paymentOrderId)
}

function onDrop(event: DragEvent, status: PaymentStatus) {
  const paymentOrderId = event.dataTransfer?.getData('text/plain')
  const order = props.orders.find(item => item.paymentOrderId === paymentOrderId)
  if (order) {
    void moveOrder(order, status)
  }
}

async function moveOrder(order: PaymentOrderResponse, target: PaymentStatus) {
  let ifMatch = etags.value[order.paymentOrderId]
  if (!ifMatch) {
    const detail = await getOrder(props.merchantId, order.paymentOrderId)
    ifMatch = detail.headers.etag
    if (ifMatch) {
      etags.value = { ...etags.value, [order.paymentOrderId]: ifMatch }
    }
  }
  if (!ifMatch) {
    toast.add({ title: 'Reload the board to obtain an ETag', color: 'warning' })
    return
  }
  const snapshot = { ...order }
  emit('updated', { ...order, status: target })
  const idempotencyKey = crypto.randomUUID()
  let response
  if (target === 'AUTHORIZED') {
    response = await authorizeOrder(props.merchantId, order.paymentOrderId, ifMatch, idempotencyKey)
  } else if (target === 'CAPTURED') {
    response = await captureOrder(
      props.merchantId,
      order.paymentOrderId,
      ifMatch,
      idempotencyKey,
      { amountMinor: order.amountMinor },
    )
  } else if (target === 'CANCELLED') {
    response = await cancelOrder(props.merchantId, order.paymentOrderId, ifMatch, idempotencyKey)
  } else {
    emit('rollback', snapshot)
    toast.add({ title: 'That column is not a lifecycle action', color: 'error' })
    return
  }
  if (response.data) {
    if (response.headers.etag) {
      etags.value = { ...etags.value, [order.paymentOrderId]: response.headers.etag }
    }
    emit('updated', response.data)
  } else {
    emit('rollback', snapshot)
    toast.add({
      title: 'Move failed',
      description: response.problem?.detail || response.problem?.title || 'Lifecycle action failed',
      color: 'error',
    })
  }
}
</script>
