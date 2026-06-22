<template>
  <BusinessStatusBadge :status="status" type="merchant" />
</template>

<script setup lang="ts">
/**
 * Thin wrapper — delegates to BusinessStatusBadge.
 * Kept for backward compatibility; no breaking changes.
 */
defineProps<{
  status: string
}>()
</script>
