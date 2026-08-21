<template>
  <section data-testid="payment-expiry-calendar" class="space-y-4">
    <h2 class="text-sm font-medium text-highlighted">Authorization expiry and dual-control</h2>
    <UCalendar
      :is-date-disabled="isPast"
      @update:model-value="onSelect"
    />
    <ul data-testid="payment-expiry-day-list" class="space-y-1 text-sm">
      <li v-if="itemsForDay.length === 0" class="text-muted">No expiries or dual-control dues on this day</li>
      <li
        v-for="item in itemsForDay"
        :key="item.key"
        data-testid="payment-expiry-day-item"
      >
        {{ item.label }}
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { z } from 'zod'
import type { PaymentOrderResponse } from '~/composables/usePaymentOrdersApi'

export type DualControlDue = {
  approvalId: string
  paymentOrderId: string
  clientOrderReference: string
  dueAt: string
}

const calendarDaySchema = z.object({
  year: z.number().int(),
  month: z.number().int().min(1).max(12),
  day: z.number().int().min(1).max(31),
})

const props = defineProps<{
  orders: PaymentOrderResponse[]
  dualControlDues?: DualControlDue[]
}>()

const selectedKey = ref(localDateKey(new Date()))

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function localDateKey(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function expiresKey(iso: string | null | undefined) {
  if (!iso) {
    return null
  }
  return localDateKey(new Date(iso))
}

function isPast(date: { year: number, month: number, day: number }) {
  const today = new Date()
  const cell = new Date(date.year, date.month - 1, date.day)
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  return cell < start
}

function dayKey(value: z.infer<typeof calendarDaySchema>) {
  return `${value.year}-${pad(value.month)}-${pad(value.day)}`
}

function onSelect(value: unknown) {
  const single = calendarDaySchema.safeParse(value)
  if (single.success) {
    selectedKey.value = dayKey(single.data)
    return
  }
  const range = z.object({ start: calendarDaySchema }).safeParse(value)
  if (range.success) {
    selectedKey.value = dayKey(range.data.start)
    return
  }
  const iso = String(value)
  if (/^\d{4}-\d{2}-\d{2}/.test(iso)) {
    selectedKey.value = iso.slice(0, 10)
  }
}

const itemsForDay = computed(() => {
  const expiries = props.orders
    .filter(order => expiresKey(order.expiresAt) === selectedKey.value)
    .map(order => ({
      key: `exp-${order.paymentOrderId}`,
      label: `${order.clientOrderReference} · ${order.status}`,
    }))
  const dues = (props.dualControlDues ?? [])
    .filter(due => expiresKey(due.dueAt) === selectedKey.value)
    .map(due => ({
      key: `due-${due.approvalId}`,
      label: `Dual-control · ${due.clientOrderReference}`,
    }))
  return [...expiries, ...dues]
})
</script>
