<template>
  <UCard data-testid="eventlab-delivery-card">
    <template #header>
      <h3 class="font-semibold">Delivery Proof</h3>
    </template>
    <div v-if="pending" data-testid="eventlab-delivery-pending">Pending — waiting for eventlab-inspector ≤5s</div>
    <div v-else-if="record" data-testid="eventlab-delivery-processed">
      <UBadge :label="record.status" :color="record.status==='DEAD' ? 'error' : 'success'" data-testid="eventlab-delivery-status" />
      <span class="ml-2" data-testid="eventlab-delivery-group">{{ record.consumerGroup }}</span>
      <UAlert v-if="record.status==='DEAD'" data-testid="eventlab-delivery-dlt-banner" title="Dead-letter topic" description="Record on lab.event-lab.dlq.v1" color="warning" />
    </div>
    <div v-else data-testid="eventlab-delivery-empty">No delivery record</div>
  </UCard>
</template>
<script setup lang="ts">
import type { EventLabRecord } from '~/schemas/event-lab.schema'
defineProps<{ record: EventLabRecord | null; pending: boolean }>()
</script>
