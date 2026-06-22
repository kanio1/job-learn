<template>
  <UBadge :color="color" variant="subtle" size="sm">
    {{ status }} {{ categoryLabel }}
  </UBadge>
</template>

<script setup lang="ts">
/**
 * Renders an HTTP status code alongside its leading-digit category label.
 * Color is assigned per category so the badge is also distinguishable without color.
 *
 * Requirements: 8.2, 6.3
 */

const props = defineProps<{
  status: number
}>()

const categoryLabel = computed<string>(() => {
  const digit = Math.floor(props.status / 100)
  switch (digit) {
    case 1: return 'Informational'
    case 2: return 'Success'
    case 3: return 'Redirection'
    case 4: return 'Client Error'
    case 5: return 'Server Error'
    default: return 'Unknown'
  }
})

const color = computed(() => {
  const digit = Math.floor(props.status / 100)
  switch (digit) {
    case 1: return 'neutral'
    case 2: return 'success'
    case 3: return 'warning'
    case 4: return 'error'
    case 5: return 'error'
    default: return 'neutral'
  }
})
</script>
