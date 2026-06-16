<template>
  <div data-testid="empty-state">
    <UCard>
      <div class="flex flex-col items-center gap-4 py-8 text-center">
        <UIcon name="i-lucide-inbox" class="text-4xl text-gray-300 dark:text-gray-600" />
        <p class="text-sm text-gray-500 dark:text-gray-400">{{ description }}</p>
        <UButton
          v-if="actionLabel && actionTo"
          :to="actionTo"
          variant="soft"
          size="sm"
        >
          {{ actionLabel }}
        </UButton>
        <UButton
          v-else-if="actionLabel"
          variant="soft"
          size="sm"
          @click="emit('action')"
        >
          {{ actionLabel }}
        </UButton>
      </div>
    </UCard>
  </div>
</template>

<script setup lang="ts">
/**
 * Empty state card with description and optional navigation action.
 * - When `actionTo` is provided: renders a router-link button.
 * - When only `actionLabel` is provided: renders a callback button emitting `action`.
 *
 * Requirements: 9.3
 */

defineProps<{
  description: string
  actionLabel?: string
  actionTo?: string
}>()

const emit = defineEmits<{
  action: []
}>()
</script>
