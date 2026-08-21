<template>
  <UTable
    v-model:sorting="sorting"
    v-model:row-selection="rowSelection"
    :data="merchants"
    :columns="columns"
    :loading="loading"
    :sorting-options="{ manualSorting: true }"
    :get-row-id="(row: Merchant) => row.merchantId"
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
 * Server sort: v-model:sorting + manualSorting. Headers for displayName,
 * status, updatedAt, and createdAt emit the column name as the button label.
 *
 * Row actions:
 * - DRAFT merchants: Activate button (`data-testid="activate-merchant-button"`)
 * - ACTIVE merchants: New Payment + Suspend buttons
 * - SUSPENDED merchants: no lifecycle action (terminal merchant state)
 */

import { h, resolveComponent } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import type { MerchantResponse } from '~/composables/useMerchantsApi'

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')
const UCheckbox = resolveComponent('UCheckbox')
const UInput = resolveComponent('UInput')

export type Merchant = MerchantResponse

export type MerchantTableSorting = Array<{ id: string, desc: boolean }>
export type MerchantTableRowSelection = Record<string, boolean>

const props = withDefaults(defineProps<{
  merchants: Merchant[]
  loading?: boolean
  showTenantColumn?: boolean
}>(), {
  loading: false,
  showTenantColumn: false,
})

const sorting = defineModel<MerchantTableSorting>('sorting', { default: () => [{ id: 'createdAt', desc: true }] })
const rowSelection = defineModel<MerchantTableRowSelection>('rowSelection', { default: () => ({}) })

const emit = defineEmits<{
  activate: [merchant: Merchant]
  suspend: [merchant: Merchant]
  open360: [merchant: Merchant]
  saveName: [merchant: Merchant, displayName: string]
}>()

const { can } = useAuthorization()
const { t } = useI18n()
const canUpdateMerchantStatus = computed(() => can.value.canUpdateMerchantStatus)
const canCreatePaymentOrder = computed(() => can.value.canCreatePaymentOrder)
const canReadPayments = computed(() =>
  can.value.canReadMerchantPayments || can.value.canReadPlatformPayments,
)
const editingMerchantId = ref<string | null>(null)
const draftName = ref('')

watch(() => props.merchants, (rows) => {
  if (!editingMerchantId.value) {
    return
  }
  const current = rows.find(row => row.merchantId === editingMerchantId.value)
  if (current && current.displayName === draftName.value.trim()) {
    editingMerchantId.value = null
  }
})

function statusColor(status: string) {
  switch (status) {
    case 'DRAFT': return 'neutral' as const
    case 'ACTIVE': return 'success' as const
    case 'SUSPENDED': return 'error' as const
    default: return 'neutral' as const
  }
}

function sortableHeader(
  column: { getIsSorted: () => false | 'asc' | 'desc', toggleSorting: (desc?: boolean) => void },
  label: string,
) {
  const isSorted = column.getIsSorted()
  return h(UButton, {
    color: 'neutral',
    variant: 'ghost',
    label,
    icon: isSorted
      ? (isSorted === 'asc' ? 'i-lucide-arrow-up-narrow-wide' : 'i-lucide-arrow-down-wide-narrow')
      : 'i-lucide-arrow-up-down',
    class: '-mx-2.5',
    onClick: () => column.toggleSorting(column.getIsSorted() === 'asc'),
  })
}

const columns = computed<TableColumn<Merchant>[]>(() => {
  const selectColumn: TableColumn<Merchant>[] = canUpdateMerchantStatus.value
    ? [{
        id: 'select',
        header: ({ table }) => h(UCheckbox, {
          'modelValue': table.getIsSomePageRowsSelected() ? 'indeterminate' : table.getIsAllPageRowsSelected(),
          'onUpdate:modelValue': (value: boolean | 'indeterminate') => table.toggleAllPageRowsSelected(!!value),
          'aria-label': 'Select all',
        }),
        cell: ({ row }) => h(UCheckbox, {
          'modelValue': row.getIsSelected(),
          'onUpdate:modelValue': (value: boolean | 'indeterminate') => row.toggleSelected(!!value),
          'aria-label': `Select ${row.original.merchantReference}`,
        }),
        enableSorting: false,
      }]
    : []

  const tenantColumn: TableColumn<Merchant>[] = props.showTenantColumn
    ? [{
        id: 'tenant',
        header: t('merchants.tenant'),
        enableSorting: false,
        cell: () => h('span', { class: 'text-muted text-sm' }, '—'),
      }]
    : []

  return [
    ...selectColumn,
  {
    accessorKey: 'merchantReference',
    header: t('merchants.reference'),
    enableSorting: false,
  },
  {
    accessorKey: 'displayName',
    header: ({ column }) => sortableHeader(column, t('merchants.displayName')),
    cell: ({ row }) => {
      if (editingMerchantId.value === row.original.merchantId) {
        return h('div', { class: 'flex items-center gap-2' }, [
          h(UInput, {
            'modelValue': draftName.value,
            'onUpdate:modelValue': (value: string) => { draftName.value = value },
            'aria-label': `Display name for ${row.original.merchantReference}`,
            'data-testid': 'merchant-name-input',
          }),
          h(UButton, {
            size: 'xs',
            color: 'primary',
            label: 'Save',
            'aria-label': `Save name ${row.original.merchantReference}`,
            'data-testid': 'merchant-name-save',
            onClick: () => emit('saveName', row.original, draftName.value),
          }),
        ])
      }
      const nameLink = h(UButton, {
        variant: 'link',
        color: 'primary',
        size: 'sm',
        type: 'button',
        label: row.original.displayName,
        'aria-label': `Open ${row.original.merchantReference}`,
        onClick: () => emit('open360', row.original),
      })
      if (!canUpdateMerchantStatus.value) {
        return nameLink
      }
      return h('div', { class: 'flex items-center gap-1' }, [
        nameLink,
        h(UButton, {
          size: 'xs',
          color: 'neutral',
          variant: 'ghost',
          label: 'Edit',
          'aria-label': `Edit name ${row.original.merchantReference}`,
          'data-testid': 'merchant-name-edit',
          onClick: () => {
            editingMerchantId.value = row.original.merchantId
            draftName.value = row.original.displayName
          },
        }),
      ])
    }
  },
  {
    accessorKey: 'status',
    header: ({ column }) => sortableHeader(column, t('merchants.status')),
    cell: ({ row }) => {
      return h(UBadge, {
        class: 'capitalize',
        variant: 'subtle',
        color: statusColor(row.original.status)
      }, () => row.original.status)
    }
  },
  ...tenantColumn,
  {
    id: 'riskFlagged',
    header: t('merchants.risk'),
    enableSorting: false,
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
    accessorKey: 'updatedAt',
    header: ({ column }) => sortableHeader(column, t('merchants.updated')),
    cell: ({ row }) => {
      return h('span', { class: 'text-muted text-sm' }, new Date(row.original.updatedAt).toLocaleString())
    }
  },
  {
    accessorKey: 'createdAt',
    header: ({ column }) => sortableHeader(column, t('merchants.created')),
    cell: ({ row }) => {
      return h('span', { class: 'text-muted text-sm' }, new Date(row.original.createdAt).toLocaleString())
    }
  },
  {
    id: 'actions',
    enableSorting: false,
    cell: ({ row }) => {
      const buttons: ReturnType<typeof h>[] = []
      const status = row.original.status

      if (status === 'DRAFT' && canUpdateMerchantStatus.value) {
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

      if (canReadPayments.value) {
        buttons.push(
          h('span', { 'data-testid': 'action-view-payments' }, [
            h(UButton, {
              size: 'xs',
              color: 'neutral',
              variant: 'ghost',
              icon: 'i-lucide-receipt',
              label: 'Payments',
              'aria-label': `View payments for ${row.original.merchantReference}`,
              to: `/admin/merchants/${row.original.merchantId}/payments`,
            }),
          ]),
        )
      }

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
})
</script>
