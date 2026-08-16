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

    <span v-if="displayedActions.includes('authorize')" data-testid="action-lifecycle-authorize">
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

    <span v-if="displayedActions.includes('capture')" data-testid="action-lifecycle-capture" class="flex items-center gap-1">
      <UInput
        v-model.number="captureAmount"
        data-testid="capture-amount-input"
        type="number"
        size="sm"
        placeholder="Minor units (empty = full)"
        aria-label="Capture amount in minor units"
        class="w-40"
        :min="1"
      />
      <UButton
        data-testid="lifecycle-capture"
        color="success"
        variant="soft"
        size="sm"
        icon="i-lucide-check-circle"
        @click="triggerAction('capture', captureAmount ?? null)"
      >
        Capture
      </UButton>
    </span>

    <span v-if="displayedActions.includes('cancel')" data-testid="action-lifecycle-cancel">
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

    <p
      v-if="canRunLifecycle && displayedActions.length === 0 && availableActions.includes('refund')"
      class="text-sm text-gray-400 italic"
      data-testid="lifecycle-refund-dual-control-hint"
    >
      Refunds use dual-control approval
    </p>
    <p
      v-else-if="canRunLifecycle && displayedActions.length === 0"
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
 * Capability comes only from useAuthorization() (session roles). Do not pass a
 * Boolean prop — Vue casts absent Boolean props to false and hides Cancel for
 * MERCHANT_MANAGER. Unit tests mock useAuthorization.
 *
 * Buttons not available for the current status are hidden (not rendered).
 * Emits `action-triggered` with the action name when a button is clicked.
 *
 * Requirements: 5.1, 8.10, 12.6
 */

const props = defineProps({
  paymentOrderId: { type: String, required: true },
  merchantId: { type: String, required: true },
})

const emit = defineEmits<{
  'action-triggered': [action: string, amountMinor: number | null]
}>()

const store = usePaymentOrdersStore()
const { can } = useAuthorization()
const canRunLifecycle = computed(() => can.value.canRunLifecycle)

const captureAmount = ref<number | null>(null)

const availableActions = computed<string[]>(() => {
  if (!canRunLifecycle.value) return []
  const status = store.currentOrder?.paymentOrderId === props.paymentOrderId
    ? store.currentOrder?.status
    : undefined
  return store.getAvailableActions(status)
})

/** Direct POST /refund is merchant dual-control only; do not render a one-click refund. */
const displayedActions = computed(() => availableActions.value.filter(action => action !== 'refund'))

function triggerAction(action: string, amountMinor: number | null = null) {
  emit('action-triggered', action, amountMinor)
}
</script>
