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

    <span v-if="availableActions.includes('capture')" data-testid="action-lifecycle-capture" class="flex items-center gap-1">
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

    <span v-if="availableActions.includes('refund')" data-testid="action-lifecycle-refund" class="flex items-center gap-1">
      <UInput
        v-model.number="refundAmount"
        data-testid="refund-amount-input"
        type="number"
        size="sm"
        placeholder="Minor units (empty = full)"
        aria-label="Refund amount in minor units"
        class="w-40"
        :min="1"
      />
      <UButton
        data-testid="lifecycle-refund"
        color="error"
        variant="soft"
        size="sm"
        icon="i-lucide-rotate-ccw"
        @click="triggerAction('refund', refundAmount ?? null)"
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
const refundAmount = ref<number | null>(null)

const availableActions = computed<string[]>(() => {
  if (!canRunLifecycle.value) return []
  const status = store.currentOrder?.paymentOrderId === props.paymentOrderId
    ? store.currentOrder?.status
    : undefined
  return store.getAvailableActions(status)
})

function triggerAction(action: string, amountMinor: number | null = null) {
  emit('action-triggered', action, amountMinor)
}
</script>
