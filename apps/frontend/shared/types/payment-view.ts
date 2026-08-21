export const PAYMENT_VIEW_RESOURCE = 'PAYMENT_ORDERS' as const
export type PaymentViewResource = typeof PAYMENT_VIEW_RESOURCE

export const PAYMENT_VIEW_FILTER_KEYS = [
  'status',
  'currency',
  'minAmount',
  'maxAmount',
  'fromDate',
  'toDate',
  'clientOrderReference',
  'sort',
] as const

export type PaymentViewFilterKey = (typeof PAYMENT_VIEW_FILTER_KEYS)[number]

export interface PaymentViewFilters {
  status?: string
  currency?: string
  minAmount?: number
  maxAmount?: number
  fromDate?: string
  toDate?: string
  clientOrderReference?: string
  sort?: string
}

export interface PaymentView {
  id: string
  name: string
  resource: PaymentViewResource
  filters: PaymentViewFilters
  columns: string[]
  isDefault: boolean
  createdAt: string
  updatedAt: string
}
