<template>
  <div class="space-y-4">
    <!-- Filter panel -->
    <UCard>
      <template #header>
        <div class="flex items-center justify-between gap-3">
          <h3 class="text-base font-semibold">Filters</h3>
          <UButton
            v-if="hasActiveFilters"
            data-testid="payment-filter-clear"
            size="xs"
            color="neutral"
            variant="ghost"
            icon="i-lucide-x"
            label="Clear filters"
            @click="clearFilters"
          />
        </div>
      </template>

      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <!-- Status filter -->
        <UFormField label="Status">
          <USelect
            v-model="filters.status"
            :items="statusOptions"
            placeholder="Any status"
            value-key="value"
            label-key="label"
            clearable
          />
        </UFormField>

        <!-- Currency filter -->
        <UFormField label="Currency">
          <USelect
            v-model="filters.currency"
            :items="currencyOptions"
            placeholder="Any currency"
            value-key="value"
            label-key="label"
            clearable
          />
        </UFormField>

        <!-- Created from date -->
        <UFormField label="Created from">
          <UInput
            v-model="filters.fromDate"
            data-testid="payment-filter-created-from"
            type="date"
          />
        </UFormField>

        <!-- Created to date -->
        <UFormField label="Created to">
          <UInput
            v-model="filters.toDate"
            data-testid="payment-filter-created-to"
            type="date"
          />
        </UFormField>

        <!-- Min amount -->
        <UFormField label="Min amount">
          <UInput
            v-model="filters.minAmountStr"
            type="number"
            min="0"
            placeholder="Min (minor units)"
          />
        </UFormField>

        <!-- Max amount -->
        <UFormField label="Max amount">
          <UInput
            v-model="filters.maxAmountStr"
            type="number"
            min="0"
            placeholder="Max (minor units)"
          />
        </UFormField>

        <!-- Client order reference -->
        <UFormField label="Client order reference" class="sm:col-span-2">
          <UInput
            v-model="filters.clientOrderReference"
            placeholder="Search by reference…"
            clearable
          />
        </UFormField>
      </div>

      <template #footer>
        <div class="flex justify-end">
          <UButton
            data-testid="payment-filter-apply"
            icon="i-lucide-search"
            label="Apply filters"
            @click="applyFilters"
          />
        </div>
      </template>
    </UCard>

    <!-- Table / state area -->
    <div data-testid="payment-order-table">
    <div data-testid="payment-orders-table">
    <UCard>
      <template #header>
        <div class="flex items-center justify-between gap-3">
          <div>
            <h3 class="text-base font-semibold">Payment orders</h3>
            <p v-if="list" class="text-sm text-muted">
              {{ list.totalElements }} order(s) across {{ list.totalPages }} page(s)
            </p>
          </div>
          <UButton icon="i-lucide-plus" :to="`/admin/merchants/${merchantId}/payments/new`" label="New payment" />
        </div>
      </template>

      <!-- Loading state -->
      <LoadingState v-if="loading" message="Loading payment orders…" />

      <!-- Error state -->
      <ErrorState
        v-else-if="error"
        :message="error"
        :problem="problem"
        :on-retry="onRetry"
      />

      <!-- Empty state -->
      <div
        v-else-if="list && list.content.length === 0"
        data-testid="payment-orders-empty-state"
      >
        <EmptyStateCard
          :description="hasActiveFilters ? 'No payment orders match the active filters.' : 'This merchant has no payment orders yet.'"
          :action-label="hasActiveFilters ? 'Clear filters' : 'Create payment order'"
          :action-to="hasActiveFilters ? undefined : `/admin/merchants/${merchantId}/payments/new`"
          @action="hasActiveFilters ? clearFilters() : undefined"
        />
      </div>

      <!-- Data table -->
      <template v-else-if="list && list.content.length > 0">
        <UTable
          :data="list.content"
          :columns="columns"
          aria-label="Payment order list"
          class="shrink-0"
        />
      </template>

      <!-- Pagination -->
      <template v-if="list && list.totalPages > 1" #footer>
        <div class="flex justify-center">
          <UPagination
            :default-page="(list.page ?? 0) + 1"
            :total="list.totalElements"
            :page-count="list.size"
            @update:page="onPageChange"
          />
        </div>
      </template>
    </UCard>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { h, resolveComponent, computed, reactive } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import type { ProblemDetails } from '~/types/api'
import {
  paymentOrderListQuerySchema,
  type PaymentOrderListQuery,
  type PaymentOrderListResponse,
  type PaymentOrderResponse,
} from '~/schemas/payment-order.schema'

const PaymentStatusBadge = resolveComponent('PaymentStatusBadge')
const UButton = resolveComponent('UButton')

const props = defineProps<{
  merchantId: string
  list?: PaymentOrderListResponse | null
  loading?: boolean
  error?: string | null
  problem?: ProblemDetails | null
  onRetry?: () => void
  currentQuery?: PaymentOrderListQuery
}>()

const emit = defineEmits<{
  (e: 'query-change', query: PaymentOrderListQuery): void
}>()

// ---------------------------------------------------------------------------
// Filter state — initialised from currentQuery if provided
// ---------------------------------------------------------------------------

const filters = reactive({
  status: props.currentQuery?.status ?? '',
  currency: props.currentQuery?.currency ?? '',
  fromDate: props.currentQuery?.fromDate ?? '',
  toDate: props.currentQuery?.toDate ?? '',
  minAmountStr: props.currentQuery?.minAmount != null ? String(props.currentQuery.minAmount) : '',
  maxAmountStr: props.currentQuery?.maxAmount != null ? String(props.currentQuery.maxAmount) : '',
  clientOrderReference: props.currentQuery?.clientOrderReference ?? '',
})

const currentPage = ref((props.currentQuery?.page ?? 0))
const pageSize = ref(props.currentQuery?.size ?? 20)

// ---------------------------------------------------------------------------
// Derived state
// ---------------------------------------------------------------------------

const hasActiveFilters = computed(() =>
  !!(filters.status || filters.currency || filters.fromDate || filters.toDate ||
     filters.minAmountStr || filters.maxAmountStr || filters.clientOrderReference)
)

// ---------------------------------------------------------------------------
// Options for selects
// ---------------------------------------------------------------------------

const statusOptions = [
  { value: 'CREATED', label: 'Created' },
  { value: 'AUTHORIZED', label: 'Authorized' },
  { value: 'CAPTURED', label: 'Captured' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'EXPIRED', label: 'Expired' },
  { value: 'REFUNDED', label: 'Refunded' },
]

const currencyOptions = [
  { value: 'PLN', label: 'PLN' },
  { value: 'EUR', label: 'EUR' },
  { value: 'USD', label: 'USD' },
]

// ---------------------------------------------------------------------------
// Actions
// ---------------------------------------------------------------------------

function buildQuery(page: number): PaymentOrderListQuery {
  const raw = {
    status: filters.status || undefined,
    currency: filters.currency || undefined,
    fromDate: filters.fromDate || undefined,
    toDate: filters.toDate || undefined,
    minAmount: filters.minAmountStr ? parseInt(filters.minAmountStr, 10) : undefined,
    maxAmount: filters.maxAmountStr ? parseInt(filters.maxAmountStr, 10) : undefined,
    clientOrderReference: filters.clientOrderReference || undefined,
    page,
    size: pageSize.value,
    sort: 'createdAt,desc',
  }
  // Validate and strip via schema (sets defaults, enforces max size=100)
  return paymentOrderListQuerySchema.parse(raw)
}

function applyFilters() {
  currentPage.value = 0
  emit('query-change', buildQuery(0))
}

function clearFilters() {
  filters.status = ''
  filters.currency = ''
  filters.fromDate = ''
  filters.toDate = ''
  filters.minAmountStr = ''
  filters.maxAmountStr = ''
  filters.clientOrderReference = ''
  currentPage.value = 0
  emit('query-change', buildQuery(0))
}

function onPageChange(page: number) {
  // UPagination is 1-based; backend is 0-based
  currentPage.value = page - 1
  emit('query-change', buildQuery(page - 1))
}

// ---------------------------------------------------------------------------
// Table columns
// ---------------------------------------------------------------------------

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
