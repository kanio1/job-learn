<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between gap-3">
        <div>
          <h3 class="text-base font-semibold">Payment orders</h3>
          <p class="text-sm text-muted">{{ list.totalElements }} order(s) across {{ list.totalPages }} page(s)</p>
        </div>
        <UButton icon="i-lucide-plus" :to="`/admin/merchants/${merchantId}/payments/new`" label="New payment" />
      </div>
    </template>

    <UTable
      v-if="list.content.length"
      :data="list.content"
      :columns="columns"
      aria-label="Payment order list"
      class="shrink-0"
    />

    <UEmpty
      v-else
      icon="i-lucide-credit-card"
      title="No payment orders yet"
      description="This merchant has no payment orders matching the current scope. Create a payment order before expecting summary data."
    >
      <template #actions>
        <UButton icon="i-lucide-plus" :to="`/admin/merchants/${merchantId}/payments/new`">
          Create payment order
        </UButton>
      </template>
    </UEmpty>
  </UCard>
</template>

<script setup lang="ts">
import { h, resolveComponent } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import type { PaymentOrderListResponse, PaymentOrderResponse } from '~/schemas/payment-order.schema'

const PaymentStatusBadge = resolveComponent('PaymentStatusBadge')
const UButton = resolveComponent('UButton')

const props = defineProps<{
  merchantId: string
  list: PaymentOrderListResponse
}>()

const columns: TableColumn<PaymentOrderResponse>[] = [
  {
    accessorKey: 'clientOrderReference',
    header: 'Reference',
  },
  {
    accessorKey: 'amountMinor',
    header: 'Amount',
    cell: ({ row }) => `${row.original.amountMinor} ${row.original.currency}`,
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => h(PaymentStatusBadge, { status: row.original.status }),
  },
  {
    accessorKey: 'createdAt',
    header: 'Created',
    cell: ({ row }) => h('span', { class: 'text-muted text-sm' }, new Date(row.original.createdAt).toLocaleString()),
  },
  {
    id: 'actions',
    cell: ({ row }) => h(UButton, {
      size: 'xs',
      variant: 'ghost',
      icon: 'i-lucide-external-link',
      label: 'Details',
      'aria-label': `View payment order ${row.original.clientOrderReference}`,
      to: `/admin/merchants/${props.merchantId}/payments/${row.original.paymentOrderId}`,
    }),
  },
]
</script>
