<template>
  <UTable
    :data="merchants"
    :columns="columns"
    :loading="loading"
    aria-label="Merchant registry"
    class="shrink-0"
    :ui="{
      base: 'table-fixed border-separate border-spacing-0',
      thead: '[&>tr]:bg-elevated/50 [&>tr]:after:content-none',
      tbody: '[&>tr]:last:[&>td]:border-b-0',
      th: 'py-2 first:rounded-l-lg last:rounded-r-lg border-y border-default first:border-l last:border-r',
      td: 'border-b border-default',
      separator: 'h-0'
    }"
  />
</template>

<script setup lang="ts">
import { h, resolveComponent } from 'vue'
import type { TableColumn } from '@nuxt/ui'

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

export interface Merchant {
  merchantId: string
  merchantReference: string
  displayName: string
  status: 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | string
  createdAt: string
  updatedAt: string
}

withDefaults(defineProps<{
  merchants: Merchant[]
  loading?: boolean
}>(), {
  loading: false
})

const emit = defineEmits<{
  activate: [merchant: Merchant]
  suspend: [merchant: Merchant]
}>()

function statusColor(status: string) {
  switch (status) {
    case 'DRAFT': return 'neutral' as const
    case 'ACTIVE': return 'success' as const
    case 'SUSPENDED': return 'error' as const
    default: return 'neutral' as const
  }
}

const columns: TableColumn<Merchant>[] = [
  {
    accessorKey: 'merchantReference',
    header: 'Reference'
  },
  {
    accessorKey: 'displayName',
    header: 'Display Name'
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => {
      return h(UBadge, {
        class: 'capitalize',
        variant: 'subtle',
        color: statusColor(row.original.status)
      }, () => row.original.status)
    }
  },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ row }) => {
      return h('span', { class: 'text-muted text-sm' }, new Date(row.original.createdAt).toLocaleString())
    }
  },
  {
    id: 'actions',
    cell: ({ row }) => {
      const buttons: any[] = []

      if (row.original.status === 'DRAFT') {
        buttons.push(
          h(UButton, {
            size: 'xs',
            color: 'primary',
            variant: 'ghost',
            icon: 'i-lucide-play',
            label: 'Activate',
            'aria-label': `Activate ${row.original.merchantReference}`,
            onClick: () => emit('activate', row.original)
          })
        )
      }

      if (row.original.status === 'ACTIVE') {
        buttons.push(
          h(UButton, {
            size: 'xs',
            color: 'primary',
            variant: 'ghost',
            icon: 'i-lucide-credit-card',
            label: 'New Payment',
            'aria-label': `Create payment for ${row.original.merchantReference}`,
            to: `/admin/merchants/${row.original.merchantId}/payments/new`
          })
        )
        buttons.push(
          h(UButton, {
            size: 'xs',
            color: 'error',
            variant: 'ghost',
            icon: 'i-lucide-pause',
            label: 'Suspend',
            'aria-label': `Suspend ${row.original.merchantReference}`,
            onClick: () => emit('suspend', row.original)
          })
        )
      }

      return h('div', { class: 'flex items-center justify-end gap-1' }, buttons)
    }
  }
]
</script>
