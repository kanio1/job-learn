import type { PaymentView, PaymentViewFilters } from '~~/shared/types/payment-view'
import { PAYMENT_VIEW_FILTER_KEYS } from '~~/shared/types/payment-view'

export const PAYMENT_VIEWS_KEY_PREFIX = 'pq.payment-views.'
export const PAYMENT_VIEWS_QUOTA = 20

export const DEFAULT_PAYMENT_VIEW_COLUMNS = [
  'clientOrderReference',
  'amountMinor',
  'status',
  'createdAt',
  'createdBy',
  'actions',
] as const

export function paymentViewsStorageKey(subject: string): string {
  return `${PAYMENT_VIEWS_KEY_PREFIX}${subject}`
}

export function pickPaymentViewFilters(query: object): PaymentViewFilters {
  const bag: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(query)) {
    bag[key] = value
  }
  const filters: PaymentViewFilters = {}
  const status = asNonEmptyString(bag.status)
  if (status) filters.status = status
  const currency = asNonEmptyString(bag.currency)
  if (currency) filters.currency = currency
  const minAmount = asOptionalInt(bag.minAmount)
  if (minAmount !== undefined) filters.minAmount = minAmount
  const maxAmount = asOptionalInt(bag.maxAmount)
  if (maxAmount !== undefined) filters.maxAmount = maxAmount
  const fromDate = asNonEmptyString(bag.fromDate)
  if (fromDate) filters.fromDate = fromDate
  const toDate = asNonEmptyString(bag.toDate)
  if (toDate) filters.toDate = toDate
  const clientOrderReference = asNonEmptyString(bag.clientOrderReference)
  if (clientOrderReference) filters.clientOrderReference = clientOrderReference
  const sort = asNonEmptyString(bag.sort)
  if (sort) filters.sort = sort
  return filters
}

export function upsertLocalPaymentView(existing: PaymentView[], incoming: PaymentView): PaymentView[] {
  let next = existing.filter(view => view.id !== incoming.id && view.name !== incoming.name)
  if (incoming.isDefault) {
    next = next.map(view => (view.isDefault ? { ...view, isDefault: false } : view))
  }
  next = [...next, incoming]
  if (next.length <= PAYMENT_VIEWS_QUOTA) {
    return next
  }
  const droppable = next
    .filter(view => view.id !== incoming.id)
    .slice()
    .sort((left, right) => left.createdAt.localeCompare(right.createdAt) || left.id.localeCompare(right.id))
  const oldest = droppable[0]
  return oldest ? next.filter(view => view.id !== oldest.id) : next.slice(-PAYMENT_VIEWS_QUOTA)
}

export function readPaymentViews(subject: string): PaymentView[] {
  if (typeof localStorage === 'undefined' || !subject) {
    return []
  }
  try {
    const raw = localStorage.getItem(paymentViewsStorageKey(subject))
    if (!raw) {
      return []
    }
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.filter(isPaymentView)
  }
  catch {
    return []
  }
}

export function writePaymentViews(subject: string, views: PaymentView[]): void {
  if (typeof localStorage === 'undefined' || !subject) {
    return
  }
  localStorage.setItem(paymentViewsStorageKey(subject), JSON.stringify(views))
}

function isPaymentView(value: unknown): value is PaymentView {
  if (!value || typeof value !== 'object') {
    return false
  }
  const record = value as Record<string, unknown>
  if (typeof record.id !== 'string' || typeof record.name !== 'string') {
    return false
  }
  if (record.resource !== 'PAYMENT_ORDERS' || typeof record.isDefault !== 'boolean') {
    return false
  }
  if (!record.filters || typeof record.filters !== 'object' || Array.isArray(record.filters)) {
    return false
  }
  const filters = record.filters as Record<string, unknown>
  return Object.keys(filters).every(key => (PAYMENT_VIEW_FILTER_KEYS as readonly string[]).includes(key))
}

function asNonEmptyString(value: unknown): string | undefined {
  if (typeof value !== 'string') {
    return undefined
  }
  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : undefined
}

function asOptionalInt(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isInteger(value) && value >= 0) {
    return value
  }
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value)
    if (Number.isInteger(parsed) && parsed >= 0) {
      return parsed
    }
  }
  return undefined
}
