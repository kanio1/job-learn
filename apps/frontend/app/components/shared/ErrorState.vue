<template>
  <div data-testid="error-state" role="alert" aria-label="Request failed">
    <!-- Problem details card when a structured problem response is available -->
    <div v-if="problem" class="space-y-3">
      <ProblemDetailsCard :problem="problem" />
      <UButton
        v-if="onRetry"
        variant="soft"
        color="error"
        size="sm"
        icon="i-lucide-refresh-cw"
        data-testid="error-state-retry"
        :aria-label="retryLabel"
        @click="onRetry"
      >
        {{ retryLabel }}
      </UButton>
    </div>

    <!-- Fallback: message or generic error in a UAlert -->
    <div v-else class="space-y-3">
      <UAlert
        color="error"
        variant="soft"
        icon="i-lucide-alert-circle"
        :description="message || 'An unexpected error occurred. Please try again.'"
      />
      <UButton
        v-if="onRetry"
        variant="soft"
        color="error"
        size="sm"
        icon="i-lucide-refresh-cw"
        data-testid="error-state-retry"
        :aria-label="retryLabel"
        @click="onRetry"
      >
        {{ retryLabel }}
      </UButton>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Error state surface.
 * - If `problem` is present: renders ProblemDetailsCard.
 * - Otherwise: renders `message` (or a generic fallback) inside a UAlert.
 * - Never renders token values — token-safe.
 *
 * Requirements: 9.4, 9.5
 */

import ProblemDetailsCard from '~/components/shared/ProblemDetailsCard.vue'
import type { ProblemDetails } from '~/types/api'

withDefaults(defineProps<{
  problem?: ProblemDetails | null
  message?: string
  retryLabel?: string
  onRetry?: () => void
}>(), {
  retryLabel: 'Retry',
})
</script>
