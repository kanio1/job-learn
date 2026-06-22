<template>
  <USlideover
    v-model:open="open"
    title="Audit event details"
    description="Read-only event metadata"
  >
    <template #content>
      <div v-if="entry" data-testid="audit-entry-drawer" class="flex h-full flex-col">
        <div class="flex items-start justify-between gap-4 border-b border-default p-6">
          <div>
            <h2 class="text-lg font-semibold text-highlighted">Audit event</h2>
            <p class="mt-1 text-sm text-muted">{{ entry.id }}</p>
          </div>
          <UButton
            color="neutral"
            variant="ghost"
            icon="i-lucide-x"
            aria-label="Close audit event details"
            @click="open = false"
          />
        </div>

        <div class="flex-1 overflow-y-auto p-6">
          <dl class="space-y-4 text-sm">
            <DetailRow label="Occurred at" testid="audit-entry-occurred-at" :value="formatDate(entry.occurredAt)" />
            <DetailRow label="Actor" testid="audit-entry-actor-display" :value="entry.actorDisplay" />
            <DetailRow label="Action" testid="audit-entry-action" :value="readable(entry.action)" />
            <DetailRow label="Target type" testid="audit-entry-target-type" :value="readable(entry.targetType)" />
            <DetailRow label="Target id" testid="audit-entry-target-id" :value="entry.targetId" mono />
            <DetailRow label="Tenant" testid="audit-entry-tenant-id" :value="entry.tenantId" mono />
            <DetailRow label="Correlation id" testid="audit-entry-correlation-id" :value="entry.correlationId || '—'" mono />
            <div class="grid gap-1 border-b border-default pb-4 sm:grid-cols-[9rem_1fr]">
              <dt class="font-medium text-muted">Outcome</dt>
              <dd data-testid="audit-entry-outcome">
                <UBadge :color="outcomeColor" variant="subtle">{{ entry.outcome }}</UBadge>
              </dd>
            </div>
          </dl>
        </div>
      </div>
    </template>
  </USlideover>
</template>

<script setup lang="ts">
import type { AuditEvent } from '~/schemas/audit.schema'

const props = defineProps<{
  entry: AuditEvent | null
}>()

const open = defineModel<boolean>('open', { required: true })

const outcomeColor = computed<'success' | 'warning' | 'error'>(() => {
  if (props.entry?.outcome === 'SUCCESS') return 'success'
  if (props.entry?.outcome === 'DENIED') return 'warning'
  return 'error'
})

const dateFormatter = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'full',
  timeStyle: 'long',
})

function formatDate(value: string): string {
  return dateFormatter.format(new Date(value))
}

function readable(value: string): string {
  return value.replaceAll('_', ' ')
}

const DetailRow = defineComponent({
  props: {
    label: { type: String, required: true },
    value: { type: String, required: true },
    testid: { type: String, required: true },
    mono: { type: Boolean, default: false },
  },
  setup(rowProps) {
    return () => h('div', { class: 'grid gap-1 border-b border-default pb-4 sm:grid-cols-[9rem_1fr]' }, [
      h('dt', { class: 'font-medium text-muted' }, rowProps.label),
      h('dd', {
        class: rowProps.mono ? 'break-all font-mono text-xs text-highlighted' : 'text-highlighted',
        'data-testid': rowProps.testid,
      }, rowProps.value),
    ])
  },
})
</script>

