<template>
  <span :data-testid="testId" :data-status="status">
    <UBadge :color="config.color" variant="subtle" size="sm">
      {{ config.label }}
    </UBadge>
  </span>
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
  DRAFT: { label: 'Draft', color: 'warning' },
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

const testId = computed(() => {
  if (props.type === 'payment') {
    return 'payment-status-badge'
  }
  if (props.type === 'merchant') {
    return 'merchant-status-badge'
  }
  return 'status-badge'
})
</script>
