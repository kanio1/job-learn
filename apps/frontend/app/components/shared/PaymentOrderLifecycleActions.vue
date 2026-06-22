<template>
  <div class="flex flex-wrap gap-2">
    <UAlert
      v-if="!canRunLifecycle"
      color="warning"
      variant="subtle"
      icon="i-lucide-shield-alert"
      description="You do not have permission to run payment lifecycle actions."
      role="alert"
    />

    <span v-if="availableActions.includes('authorize')" data-testid="action-lifecycle-authorize">
      <UButton
        data-testid="lifecycle-authorize"
        color="info"
        variant="soft"
        size="sm"
        icon="i-lucide-shield-check"
        @click="triggerAction('authorize')"
      >
        Authorize
      </UButton>
    </span>

    <span v-if="availableActions.includes('capture')" data-testid="action-lifecycle-capture">
      <UButton
        data-testid="lifecycle-capture"
        color="success"
        variant="soft"
        size="sm"
        icon="i-lucide-check-circle"
        @click="triggerAction('capture')"
      >
        Capture
      </UButton>
    </span>

    <span v-if="availableActions.includes('cancel')" data-testid="action-lifecycle-cancel">
      <UButton
        data-testid="lifecycle-cancel"
        color="warning"
        variant="soft"
        size="sm"
        icon="i-lucide-x-circle"
        @click="triggerAction('cancel')"
      >
        Cancel
      </UButton>
    </span>

    <span v-if="availableActions.includes('refund')" data-testid="action-lifecycle-refund">
      <UButton
        data-testid="lifecycle-refund"
        color="error"
        variant="soft"
        size="sm"
        icon="i-lucide-rotate-ccw"
        @click="triggerAction('refund')"
      >
        Refund
      </UButton>
    </span>

    <p
      v-if="canRunLifecycle && availableActions.length === 0"
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
  canRunLifecycle?: boolean
}>()

const emit = defineEmits<{
  'action-triggered': [action: string]
}>()

const store = usePaymentOrdersStore()
const { can } = useAuthorization()
const canRunLifecycle = computed(() => props.canRunLifecycle ?? can.value.canRunLifecycle)

const availableActions = computed<string[]>(() => {
  if (!canRunLifecycle.value) return []
  const status = store.currentOrder?.paymentOrderId === props.paymentOrderId
    ? store.currentOrder?.status
    : undefined
  return store.getAvailableActions(status)
})

function triggerAction(action: string) {
  emit('action-triggered', action)
}
</script>
