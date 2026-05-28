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
    default: return 'neutral'
  }
})
</script>
