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
/**
 * Merchant registry table.
 *
 * Row actions:
 * - PENDING merchants: Activate button (`data-testid="activate-merchant-button"`, Req 12.2)
 * - ACTIVE merchants: New Payment + Suspend buttons
 * - SUSPENDED merchants: Activate button (`data-testid="activate-merchant-button"`, Req 2.6)
 *
 * Emits `activate` and `suspend` to let the parent page call useMerchantsApi and
 * show the resulting status (Req 2.6) or an error (Req 2.9).
 */

import { h, resolveComponent } from 'vue'
import type { TableColumn } from '@nuxt/ui'

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

export interface Merchant {
  merchantId: string
  merchantReference: string
  displayName: string
  status: 'PENDING' | 'ACTIVE' | 'SUSPENDED'
  createdAt: string
  updatedAt: string
  riskFlagged: boolean
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

const { can } = useAuthorization()
const canUpdateMerchantStatus = computed(() => can.value.canUpdateMerchantStatus)
const canCreatePaymentOrder = computed(() => can.value.canCreatePaymentOrder)

function statusColor(status: string) {
  switch (status) {
    case 'PENDING': return 'neutral' as const
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
    header: 'Display Name',
    cell: ({ row }) => {
      return h(UButton, {
        variant: 'link',
        color: 'primary',
        size: 'sm',
        label: row.original.displayName,
        to: `/admin/merchants/${row.original.merchantId}`,
      })
    }
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
    id: 'riskFlagged',
    header: 'Risk',
    cell: ({ row }) => {
      if (!row.original.riskFlagged) return null
      return h(UBadge, {
        'data-testid': 'merchant-risk-badge',
        variant: 'subtle',
        color: 'error',
        icon: 'i-lucide-flag',
      }, () => 'Risk flagged')
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
      const status = row.original.status

      // Activate: available for PENDING and SUSPENDED merchants (Req 2.6)
      if ((status === 'PENDING' || status === 'SUSPENDED') && canUpdateMerchantStatus.value) {
        buttons.push(
          h('span', { 'data-testid': 'action-activate-merchant' }, [
            h(UButton, {
              size: 'xs',
              color: 'primary',
              variant: 'ghost',
              icon: 'i-lucide-play',
              label: 'Activate',
              'aria-label': `Activate ${row.original.merchantReference}`,
              'data-testid': 'activate-merchant-button',
              onClick: () => emit('activate', row.original)
            })
          ])
        )
      }

      // ACTIVE merchants: navigate to payments or suspend
      if (status === 'ACTIVE') {
        if (canCreatePaymentOrder.value) {
          buttons.push(
            h('span', { 'data-testid': 'action-create-payment-order' }, [
              h(UButton, {
                size: 'xs',
                color: 'primary',
                variant: 'ghost',
                icon: 'i-lucide-credit-card',
                label: 'New Payment',
                'aria-label': `Create payment for ${row.original.merchantReference}`,
                to: `/admin/merchants/${row.original.merchantId}/payments/new`
              })
            ])
          )
        }
        if (canUpdateMerchantStatus.value) {
          buttons.push(
            h('span', { 'data-testid': 'action-suspend-merchant' }, [
              h(UButton, {
                size: 'xs',
                color: 'error',
                variant: 'ghost',
                icon: 'i-lucide-pause',
                label: 'Suspend',
                'aria-label': `Suspend ${row.original.merchantReference}`,
                onClick: () => emit('suspend', row.original)
              })
            ])
          )
        }
      }

      return h('div', { class: 'flex items-center justify-end gap-1' }, buttons)
    }
  }
]
</script>
