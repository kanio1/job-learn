<template>
  <UBadge
    :color="color"
    variant="subtle"
    size="sm"
  >
    {{ status }}
  </UBadge>
</template>

<script setup lang="ts">
const props = defineProps<{ status: string }>()

const color = computed(() => {
  switch (props.status) {
    case 'DRAFT': return 'neutral'
    case 'ACTIVE': return 'success'
    case 'SUSPENDED': return 'error'
    default: return 'neutral'
  }
})
</script>
