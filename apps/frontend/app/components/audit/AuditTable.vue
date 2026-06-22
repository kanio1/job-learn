<template>
  <UTable
    :data="events"
    :columns="columns"
    :on-select="selectRow"
    aria-label="Audit log entries"
    data-testid="audit-table"
    class="shrink-0"
    :ui="{
      base: 'table-fixed border-separate border-spacing-0',
      thead: '[&>tr]:bg-elevated/50 [&>tr]:after:content-none',
      tbody: '[&>tr]:last:[&>td]:border-b-0',
      th: 'py-2 first:rounded-l-lg last:rounded-r-lg border-y border-default first:border-l last:border-r',
      td: 'border-b border-default align-top',
      separator: 'h-0'
    }"
  />
</template>

<script setup lang="ts">
import { h, resolveComponent } from 'vue'
import type { TableColumn, TableRow } from '@nuxt/ui'
import type { AuditEvent } from '~/schemas/audit.schema'

defineProps<{
  events: AuditEvent[]
}>()

const emit = defineEmits<{
  select: [event: AuditEvent]
}>()

const UBadge = resolveComponent('UBadge')

const dateFormatter = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
  timeStyle: 'medium',
})

const columns: TableColumn<AuditEvent>[] = [
  {
    accessorKey: 'occurredAt',
    header: 'Occurred at',
    cell: ({ row }) => h('div', {
      class: 'text-sm whitespace-nowrap',
      'data-testid': `audit-row-${row.original.id}`,
    }, dateFormatter.format(new Date(row.original.occurredAt))),
  },
  {
    accessorKey: 'actorDisplay',
    header: 'Actor',
    cell: ({ row }) => h('span', { class: 'text-sm font-medium text-highlighted' }, row.original.actorDisplay),
  },
  {
    accessorKey: 'action',
    header: 'Action',
    cell: ({ row }) => h('span', { class: 'text-sm' }, readable(row.original.action)),
  },
  {
    accessorKey: 'targetType',
    header: 'Target type',
    cell: ({ row }) => h('span', { class: 'text-sm' }, readable(row.original.targetType)),
  },
  {
    accessorKey: 'targetId',
    header: 'Target id',
    cell: ({ row }) => h('code', { class: 'break-all text-xs text-muted' }, row.original.targetId),
  },
  {
    accessorKey: 'outcome',
    header: 'Outcome',
    cell: ({ row }) => h(UBadge, {
      color: outcomeColor(row.original.outcome),
      variant: 'subtle',
    }, () => row.original.outcome),
  },
  {
    accessorKey: 'correlationId',
    header: 'Correlation id',
    cell: ({ row }) => h('code', { class: 'break-all text-xs text-muted' }, row.original.correlationId || '—'),
  },
]

function selectRow(_event: Event, row: TableRow<AuditEvent>) {
  emit('select', row.original)
}

function readable(value: string): string {
  return value.replaceAll('_', ' ')
}

function outcomeColor(outcome: AuditEvent['outcome']): 'success' | 'warning' | 'error' {
  if (outcome === 'SUCCESS') return 'success'
  if (outcome === 'DENIED') return 'warning'
  return 'error'
}
</script>

