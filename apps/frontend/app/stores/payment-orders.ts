import {
  paymentOrderListResponseSchema,
  paymentOrderResponseSchema,
  paymentOrderSummaryResponseSchema,
  backendErrorSchema,
  type PaymentOrderListResponse,
  type PaymentOrderResponse,
  type PaymentOrderSummaryResponse,
} from '~/schemas/payment-order.schema'
import { ZodError } from 'zod'

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

  async function loadDetail(merchantId: string, paymentOrderId: string) {
    return loadResource(async () => {
      const response = await $fetch(`/api/merchants/${merchantId}/payment-orders/${paymentOrderId}`)
      try {
        currentOrder.value = paymentOrderResponseSchema.parse(response)
      } catch (e) {
        if (e instanceof ZodError) {
          throw new Error('Received malformed payment order data from server')
        }
        throw e
      }
      return currentOrder.value
    })
  }

  async function createOrder(merchantId: string, payload: { amountMinor: number; currency: 'PLN' | 'EUR' | 'USD'; clientOrderReference: string }, idempotencyKey: string) {
    return loadResource(async () => {
      const response = await $fetch(`/api/merchants/${merchantId}/payment-orders`, {
        method: 'POST',
        headers: {
          'Idempotency-Key': idempotencyKey,
        },
        body: payload,
      })
      try {
        const parsed = paymentOrderResponseSchema.parse(response)
        lastCreatedOrder.value = parsed
        return parsed
      } catch (e) {
        if (e instanceof ZodError) {
          throw new Error('Received malformed payment order data from server')
        }
        throw e
      }
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
    const errorLike = e as { statusCode?: number; data?: unknown; statusMessage?: string; message?: string }

    if (errorLike?.statusCode === 403) {
      insufficientAuthority.value = true
      list.value = null
      summary.value = null
      currentOrder.value = null
      lastCreatedOrder.value = null
      error.value = 'You do not have permission to view payment orders for this merchant.'
      return
    }

    if (errorLike?.statusCode === 404) {
      currentOrder.value = null
      error.value = 'Payment order not found.'
      return
    }

    if (errorLike?.statusCode === 503) {
      const parsedError = backendErrorSchema.safeParse(errorLike?.data)
      if (parsedError.success && parsedError.data.error === 'backend_unavailable') {
        error.value = parsedError.data.message || 'Backend service is unavailable. Start the backend on http://localhost:8080 and retry.'
      } else {
        error.value = 'Backend service is unavailable. Start the backend on http://localhost:8080 and retry.'
      }
      return
    }

    const parsedError = backendErrorSchema.safeParse(errorLike?.data)
    if (parsedError.success && parsedError.data.message) {
      error.value = parsedError.data.message
    } else {
      error.value = errorLike?.statusMessage || errorLike?.message || 'Failed to load payment orders.'
    }
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
    loadDetail,
    createOrder,
    setLastCreatedOrder,
    clearError,
    reset,
  }
})
