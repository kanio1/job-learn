<template>
  <div
    data-testid="loading-state"
    role="status"
    :aria-label="message ?? 'Loading content'"
    class="space-y-3 p-4"
  >
    <USkeleton class="h-4 w-3/4" />
    <USkeleton class="h-4 w-1/2" />
    <USkeleton class="h-4 w-2/3" />
    <p v-if="message" class="text-sm text-gray-500 dark:text-gray-400 mt-2">{{ message }}</p>
  </div>
</template>

<script setup lang="ts">
/**
 * Generic loading state using USkeleton for visual feedback.
 *
 * Requirements: 9.1
 */

defineProps<{
  message?: string
}>()
</script>
