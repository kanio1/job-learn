<template>
  <div data-testid="problem-details-card">
  <UCard>
    <template #header>
      <div class="flex items-center gap-2">
        <UIcon name="i-lucide-alert-triangle" class="text-red-500" />
        <span class="text-sm font-semibold text-red-600 dark:text-red-400">Problem Details</span>
      </div>
    </template>

    <dl class="space-y-2 text-sm">
      <div class="flex gap-2">
        <dt class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Type</dt>
        <dd class="break-all font-mono text-gray-900 dark:text-gray-100">{{ problem.type ?? '—' }}</dd>
      </div>
      <div class="flex gap-2">
        <dt class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Title</dt>
        <dd class="text-gray-900 dark:text-gray-100">{{ problem.title ?? '—' }}</dd>
      </div>
      <div class="flex gap-2">
        <dt class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Status</dt>
        <dd>
          <HttpStatusBadge v-if="problem.status != null" :status="problem.status" />
          <span v-else class="text-gray-400">—</span>
        </dd>
      </div>
      <div class="flex gap-2">
        <dt class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Detail</dt>
        <dd class="text-gray-900 dark:text-gray-100">{{ problem.detail ?? '—' }}</dd>
      </div>
      <div class="flex gap-2">
        <dt class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Instance</dt>
        <dd class="break-all font-mono text-gray-900 dark:text-gray-100">{{ problem.instance ?? '—' }}</dd>
      </div>

      <div v-if="problem.correlationId" class="flex gap-2">
        <dt class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Correlation ID</dt>
        <dd
          data-testid="correlation-id-value"
          class="break-all font-mono text-xs text-gray-900 dark:text-gray-100"
        >{{ problem.correlationId }}</dd>
      </div>

      <div v-if="problem.requiredHeader" class="flex gap-2">
        <dt class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Required Header</dt>
        <dd
          data-testid="required-header-value"
          class="font-mono text-gray-900 dark:text-gray-100"
        >{{ problem.requiredHeader }}</dd>
      </div>

      <div v-if="problem.retryable !== undefined" class="flex gap-2">
        <dt class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Retryable</dt>
        <dd data-testid="retryable-value">
          <UBadge :color="problem.retryable ? 'success' : 'error'" variant="subtle" size="sm">
            {{ problem.retryable ? 'Yes' : 'No' }}
          </UBadge>
          <span v-if="problem.retryAfterSeconds" class="ml-2 text-xs text-gray-600 dark:text-gray-400">
            (retry after {{ problem.retryAfterSeconds }}s)
          </span>
        </dd>
      </div>

      <div v-if="problem.details?.length" class="space-y-1">
        <dt class="font-medium text-gray-500 dark:text-gray-400">Field Errors</dt>
        <ul data-testid="field-errors-list" class="space-y-1 pl-2">
          <li
            v-for="err in problem.details"
            :key="err.field"
            :data-field="err.field"
            class="text-xs"
          >
            <span class="font-mono font-medium">{{ err.field }}</span>:
            <span class="text-gray-700 dark:text-gray-300">{{ err.message }}</span>
          </li>
        </ul>
      </div>
    </dl>
  </UCard>
  </div>
</template>

<script setup lang="ts">
/**
 * Renders all standard RFC 7807 problem+json members.
 * Each absent/null/undefined member shows "—" as an explicit empty indicator.
 *
 * Requirements: 8.5, 12.7
 */

import HttpStatusBadge from '~/components/shared/HttpStatusBadge.vue'
import type { ProblemDetails } from '~/types/api'

defineProps<{
  problem: ProblemDetails
}>()
</script>
