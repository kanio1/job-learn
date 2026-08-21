<template>
  <section aria-label="Live Operations" data-testid="ops-feed">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-base font-semibold">Live Operations</h2>
      <UChip
        :show="true"
        :color="connected ? 'success' : 'neutral'"
        inset
      >
        <span data-testid="ops-feed-chip">{{ connected ? 'connected' : 'disconnected' }}</span>
      </UChip>
    </div>
    <UCard>
      <ol v-if="events.length > 0" data-testid="ops-feed-timeline" class="space-y-2">
        <li
          v-for="event in events"
          :key="event.eventId"
          :data-testid="`ops-feed-row-${event.eventId}`"
          class="flex gap-3 text-sm"
        >
          <time class="text-muted shrink-0" :datetime="event.occurredAt">{{ formatTime(event.occurredAt) }}</time>
          <span data-testid="ops-feed-label">{{ event.label }}</span>
        </li>
      </ol>
      <p v-else class="text-sm text-muted" data-testid="ops-feed-empty">No live events yet.</p>
    </UCard>
  </section>
</template>

<script setup lang="ts">
const { events, connected } = useOpsFeed()

function formatTime(iso: string): string {
  try {
    return new Intl.DateTimeFormat(undefined, { timeStyle: 'medium' }).format(new Date(iso))
  }
  catch {
    return iso
  }
}
</script>
