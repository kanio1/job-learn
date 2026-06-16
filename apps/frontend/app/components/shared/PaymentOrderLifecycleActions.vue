<template>
  <div class="flex flex-wrap gap-2">
    <UButton
      v-if="availableActions.includes('authorize')"
      data-testid="lifecycle-authorize"
      color="info"
      variant="soft"
      size="sm"
      icon="i-lucide-shield-check"
      @click="triggerAction('authorize')"
    >
      Authorize
    </UButton>

    <UButton
      v-if="availableActions.includes('capture')"
      data-testid="lifecycle-capture"
      color="success"
      variant="soft"
      size="sm"
      icon="i-lucide-check-circle"
      @click="triggerAction('capture')"
    >
      Capture
    </UButton>

    <UButton
      v-if="availableActions.includes('cancel')"
      data-testid="lifecycle-cancel"
      color="warning"
      variant="soft"
      size="sm"
      icon="i-lucide-x-circle"
      @click="triggerAction('cancel')"
    >
      Cancel
    </UButton>

    <UButton
      v-if="availableActions.includes('refund')"
      data-testid="lifecycle-refund"
      color="error"
      variant="soft"
      size="sm"
      icon="i-lucide-rotate-ccw"
      @click="triggerAction('refund')"
    >
      Refund
    </UButton>

    <p
      v-if="availableActions.length === 0"
      class="text-sm text-gray-400 italic"
    >
      No actions available for current status
    </p>
  </div>
</template>

<script setup lang="ts">
/**
 * Renders one UButton per available lifecycle action for the given payment order.
 * Uses getAvailableActions from the payment-orders Pinia store to determine
 * which actions are currently available based on the payment order status.
 *
 * Buttons not available for the current status are hidden (not rendered).
 * Emits `action-triggered` with the action name when a button is clicked.
 *
 * Requirements: 5.1, 8.10, 12.6
 */

const props = defineProps<{
  paymentOrderId: string
  merchantId: string
}>()

const emit = defineEmits<{
  'action-triggered': [action: string]
}>()

const store = usePaymentOrdersStore()

const availableActions = computed<string[]>(() => {
  const status = store.currentOrder?.paymentOrderId === props.paymentOrderId
    ? store.currentOrder?.status
    : undefined
  return store.getAvailableActions(status)
})

function triggerAction(action: string) {
  emit('action-triggered', action)
}
</script>
