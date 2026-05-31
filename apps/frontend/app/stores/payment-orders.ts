import {
  paymentOrderListResponseSchema,
  paymentOrderResponseSchema,
  paymentOrderSummaryResponseSchema,
  type PaymentOrderListResponse,
  type PaymentOrderResponse,
  type PaymentOrderSummaryResponse,
} from '~/schemas/payment-order.schema'

export const usePaymentOrdersStore = defineStore('payment-orders', () => {
  const loading = ref(false)
  const error = ref<string | null>(null)
  const insufficientAuthority = ref(false)
  const lastCreatedOrder = ref<PaymentOrderResponse | null>(null)
  const currentOrder = ref<PaymentOrderResponse | null>(null)
  const list = ref<PaymentOrderListResponse | null>(null)
  const summary = ref<PaymentOrderSummaryResponse | null>(null)

  async function loadList(merchantId: string, query: Record<string, string | number | undefined> = {}) {
    return loadResource(async () => {
      const response = await $fetch(`/api/merchants/${merchantId}/payment-orders`, { query })
      list.value = paymentOrderListResponseSchema.parse(response)
      return list.value
    })
  }

  async function loadSummary(merchantId: string, query: Record<string, string | number | undefined> = {}) {
    return loadResource(async () => {
      const response = await $fetch(`/api/merchants/${merchantId}/payment-orders/summary`, { query })
      summary.value = paymentOrderSummaryResponseSchema.parse(response)
      return summary.value
    })
  }

  function setLastCreatedOrder(order: unknown) {
    lastCreatedOrder.value = paymentOrderResponseSchema.parse(order)
  }

  async function loadResource<T>(loader: () => Promise<T>) {
    loading.value = true
    clearError()

    try {
      return await loader()
    } catch (e) {
      handleError(e)
      throw e
    } finally {
      loading.value = false
    }
  }

  function handleError(e: unknown) {
    const errorLike = e as { statusCode?: number; data?: { error?: string; message?: string }; statusMessage?: string; message?: string }

    if (errorLike?.statusCode === 403) {
      insufficientAuthority.value = true
      list.value = null
      summary.value = null
      currentOrder.value = null
      error.value = 'You do not have permission to view payment orders for this merchant.'
      return
    }

    if (errorLike?.statusCode === 503 || errorLike?.data?.error === 'backend_unavailable') {
      error.value = 'Backend service is unavailable. Start the backend on http://localhost:8080 and retry.'
      return
    }

    error.value = errorLike?.data?.message || errorLike?.statusMessage || errorLike?.message || 'Failed to load payment orders.'
  }

  function clearError() {
    error.value = null
    insufficientAuthority.value = false
  }

  function reset() {
    clearError()
    lastCreatedOrder.value = null
    currentOrder.value = null
    list.value = null
    summary.value = null
  }

  return {
    loading,
    error,
    insufficientAuthority,
    lastCreatedOrder,
    currentOrder,
    list,
    summary,
    loadList,
    loadSummary,
    setLastCreatedOrder,
    clearError,
    reset,
  }
})
