<template>
  <div data-testid="etag-display" class="flex items-center gap-2">
    <span class="text-sm font-medium text-gray-500 dark:text-gray-400">ETag</span>
    <span class="font-mono text-sm text-gray-900 dark:text-gray-100">
      {{ etag ?? '—' }}
    </span>
  </div>
</template>

<script setup lang="ts">
/**
 * Read-only display for the current resource ETag.
 * Shows "—" if the ETag is absent.
 *
 * Requirements: 8.9
 */

defineProps<{
  etag?: string | null
}>()
</script>
