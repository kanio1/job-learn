<template>
  <UBadge :color="config.color" variant="subtle" size="sm">
    {{ config.label }}
  </UBadge>
</template>

<script setup lang="ts">
/**
 * Unified status badge for both Merchant and Payment_Status values.
 * Each status has a distinct text label so it is distinguishable without color.
 *
 * Requirements: 2.7, 8.1, 8.12
 */

const props = defineProps<{
  status: string
  type?: 'merchant' | 'payment'
}>()

interface BadgeConfig {
  label: string
  color: 'neutral' | 'success' | 'error' | 'warning' | 'info' | 'primary' | 'secondary'
}

const STATUS_MAP: Record<string, BadgeConfig> = {
  // Merchant statuses
  PENDING: { label: 'Pending', color: 'warning' },
  ACTIVE: { label: 'Active', color: 'success' },
  SUSPENDED: { label: 'Suspended', color: 'error' },
  // Payment statuses
  CREATED: { label: 'Created', color: 'neutral' },
  AUTHORIZED: { label: 'Authorized', color: 'info' },
  CAPTURED: { label: 'Captured', color: 'success' },
  CANCELLED: { label: 'Cancelled', color: 'error' },
  EXPIRED: { label: 'Expired', color: 'warning' },
  REFUNDED: { label: 'Refunded', color: 'secondary' },
}

const FALLBACK: BadgeConfig = { label: 'Unknown', color: 'neutral' }

const config = computed<BadgeConfig>(() => STATUS_MAP[props.status] ?? FALLBACK)
</script>
