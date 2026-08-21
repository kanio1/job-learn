import type { PaymentView, PaymentViewFilters } from '~~/shared/types/payment-view'
import { paymentViewFiltersSchema } from '~/schemas/payment-view.schema'
import {
  DEFAULT_PAYMENT_VIEW_COLUMNS,
  pickPaymentViewFilters,
  readPaymentViews,
  upsertLocalPaymentView,
  writePaymentViews,
} from '~/utils/paymentViewsStorage'

export function usePaymentViews() {
  const { user } = useUserSession()
  const {
    listPaymentViews,
    createPaymentView,
    updatePaymentView,
    setDefaultPaymentView,
  } = usePaymentViewsApi()
  const views = ref<PaymentView[]>([])

  const subject = computed(() => {
    const id = (user.value as { id?: string } | null | undefined)?.id
    return typeof id === 'string' && id.length > 0 ? id : ''
  })

  const defaultView = computed(() => views.value.find(view => view.isDefault) ?? views.value[0])

  function persist(): void {
    if (!subject.value) {
      return
    }
    writePaymentViews(subject.value, views.value)
  }

  async function hydrate(): Promise<void> {
    views.value = subject.value ? readPaymentViews(subject.value) : []
    const response = await listPaymentViews()
    if (response.status === 200 && response.data) {
      views.value = response.data.content
      persist()
    }
  }

  async function saveAs(input: {
    name: string
    filters: PaymentViewFilters | Record<string, unknown>
    columns?: string[]
    isDefault?: boolean
  }): Promise<PaymentView> {
    const name = input.name.trim()
    const filters = paymentViewFiltersSchema.parse(pickPaymentViewFilters(input.filters))
    const columns = input.columns && input.columns.length > 0
      ? [...input.columns]
      : [...DEFAULT_PAYMENT_VIEW_COLUMNS]
    const existing = views.value.find(view => view.name === name)
    const payload = {
      name,
      filters,
      columns,
      isDefault: input.isDefault ?? existing?.isDefault ?? views.value.every(view => !view.isDefault),
    }
    const saved = existing
      ? await updatePaymentView(existing.id, payload)
      : await createPaymentView(payload)
    if (saved.data) {
      views.value = upsertLocalPaymentView(views.value, saved.data)
      persist()
      return saved.data
    }
    const now = new Date().toISOString()
    const local: PaymentView = {
      id: existing?.id ?? crypto.randomUUID(),
      name,
      resource: 'PAYMENT_ORDERS',
      filters,
      columns,
      isDefault: existing?.isDefault ?? views.value.every(view => !view.isDefault),
      createdAt: existing?.createdAt ?? now,
      updatedAt: now,
    }
    views.value = upsertLocalPaymentView(views.value, local)
    persist()
    return local
  }

  async function setDefault(id: string): Promise<void> {
    const response = await setDefaultPaymentView(id)
    if (response.data) {
      views.value = views.value.map(view => ({
        ...view,
        isDefault: view.id === id,
      }))
      persist()
      return
    }
    const target = views.value.find(view => view.id === id)
    if (!target) {
      return
    }
    views.value = upsertLocalPaymentView(views.value, {
      ...target,
      isDefault: true,
      updatedAt: new Date().toISOString(),
    })
    persist()
  }

  return { subject, views, defaultView, hydrate, saveAs, setDefault }
}
