<template>
  <UBadge :color="color" variant="subtle">
    {{ status }}
  </UBadge>
</template>

<script setup lang="ts">
const props = defineProps<{
  status: string
}>()

const color = computed(() => {
  switch (props.status) {
    case 'CREATED': return 'info'
    case 'AUTHORIZED': return 'warning'
    case 'CAPTURED': return 'success'
    case 'CANCELLED': return 'error'
    case 'EXPIRED': return 'neutral'
    case 'REFUNDED': return 'info'
    default: return 'neutral'
  }
})
</script>
