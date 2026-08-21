<template>
  <UTimeline
    data-testid="merchant-360-timeline"
    :items="items"
  />
</template>

<script setup lang="ts">
import type { AuditEvent } from '~/schemas/audit.schema'
import type { MerchantResponse } from '~/composables/useMerchantsApi'

const props = defineProps<{
  merchant: MerchantResponse
  events: AuditEvent[]
  paymentHistory?: Array<{ occurredAt: string, title: string, description: string }>
}>()

const items = computed(() => {
  const statusItem = {
    date: new Date(props.merchant.updatedAt).toLocaleString(),
    title: props.merchant.status,
    description: `Current status ${props.merchant.status}`,
    icon: 'i-lucide-store',
  }
  const auditItems = [...props.events]
    .sort((a, b) => new Date(a.occurredAt).getTime() - new Date(b.occurredAt).getTime())
    .map(event => ({
      date: new Date(event.occurredAt).toLocaleString(),
      title: event.action,
      description: event.actorDisplay,
      icon: 'i-lucide-scroll-text',
    }))
  const paymentItems = [...(props.paymentHistory ?? [])]
    .sort((a, b) => new Date(a.occurredAt).getTime() - new Date(b.occurredAt).getTime())
    .map(entry => ({
      date: new Date(entry.occurredAt).toLocaleString(),
      title: entry.title,
      description: entry.description,
      icon: 'i-lucide-receipt',
    }))
  return [statusItem, ...auditItems, ...paymentItems]
})
</script>
