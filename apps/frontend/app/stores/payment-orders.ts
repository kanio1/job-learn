import {
  paymentOrderListResponseSchema,
  paymentOrderResponseSchema,
  paymentOrderSummaryResponseSchema,
  backendErrorSchema,
  type PaymentOrderListResponse,
  type PaymentOrderResponse,
  type PaymentOrderSummaryResponse,
  type StatusHistoryEntry,
  type LifecycleErrorCategory,
} from '~/schemas/payment-order.schema'
import { ZodError } from 'zod'
import { usePaymentOrdersApi } from '~/composables/usePaymentOrdersApi'
import { usePaymentLifecycleApi, mapStatusToCategory } from '~/composables/usePaymentLifecycleApi'

export const usePaymentOrdersStore = defineStore('payment-orders', () => {
  const loading = ref(false)
  const error = ref<string | null>(null)
  const insufficientAuthority = ref(false)
  const lastCreatedOrder = ref<PaymentOrderResponse | null>(null)
  const currentOrder = ref<PaymentOrderResponse | null>(null)
  const list = ref<PaymentOrderListResponse | null>(null)
  const summary = ref<PaymentOrderSummaryResponse | null>(null)

  // Feature 010 lifecycle console state
  const history = ref<StatusHistoryEntry[]>([])
  const historyLoading = ref(false)
  const historyError = ref<string | null>(null)
  const versionMarker = ref<string | null>(null)
  const lifecycleFeedback = ref<LifecycleErrorCategory | null>(null)
  const actionSubmitting = ref(false)
  const metadataSaving = ref(false)

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
      const { getOrder } = usePaymentOrdersApi()
      const response = await getOrder(merchantId, paymentOrderId)

      if (response.problem && !response.data) {
        const status = response.status
        const errorLike = { statusCode: status, statusMessage: response.problem.title, message: response.problem.detail }
        handleError(errorLike)
        throw new Error(response.problem.detail ?? response.problem.title ?? 'Failed to load payment order')
      }

      if (response.data) {
        currentOrder.value = response.data
        // Update versionMarker from ETag (preferred) or response field fallback
        versionMarker.value = response.headers.etag || response.data.versionMarker || null
      } else {
        throw new Error('Received malformed payment order data from server')
      }

      return currentOrder.value
    })
  }

  async function loadHistory(merchantId: string, paymentOrderId: string) {
    historyLoading.value = true
    historyError.value = null
    try {
      const { getHistory } = usePaymentLifecycleApi()
      const response = await getHistory(merchantId, paymentOrderId)

      if (response.data) {
        history.value = response.data.content || []
        return history.value
      } else {
        historyError.value = 'Failed to load lifecycle history.'
        history.value = []
        throw new Error(response.problem?.detail ?? 'Failed to load lifecycle history.')
      }
    } catch (e) {
      historyError.value = 'Failed to load lifecycle history.'
      history.value = []
      throw e
    } finally {
      historyLoading.value = false
    }
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
    history.value = []
    historyError.value = null
    versionMarker.value = null
    lifecycleFeedback.value = null
  }

  // Feature 010: derive available actions from current status (conservative role handling in UI)
  function getAvailableActions(status: string | undefined) {
    if (!status) return []
    switch (status) {
      case 'CREATED': return ['authorize', 'cancel']
      case 'AUTHORIZED': return ['capture', 'cancel']
      case 'CAPTURED': return ['refund']
      default: return []
    }
  }

  // Feature 010: lifecycle action submission (uses current versionMarker)
  async function submitLifecycleAction(
    merchantId: string,
    paymentOrderId: string,
    action: 'authorize' | 'capture' | 'cancel' | 'refund',
    payload: { amountMinor?: number; reason?: string } = {}
  ) {
    if (!versionMarker.value) {
      throw new Error('Missing version marker for conditional update')
    }

    actionSubmitting.value = true
    lifecycleFeedback.value = null

    const idempotencyKey = `feat010-${action}-${Date.now()}-${Math.random().toString(36).slice(2)}`
    const { authorizeOrder, captureOrder, cancelOrder, refundOrder } = usePaymentLifecycleApi()

    try {
      let response
      const ifMatch = versionMarker.value

      if (action === 'authorize') {
        response = await authorizeOrder(merchantId, paymentOrderId, ifMatch, idempotencyKey)
      } else if (action === 'capture') {
        response = await captureOrder(merchantId, paymentOrderId, ifMatch, idempotencyKey, payload)
      } else if (action === 'cancel') {
        response = await cancelOrder(merchantId, paymentOrderId, ifMatch, idempotencyKey, payload)
      } else {
        response = await refundOrder(merchantId, paymentOrderId, ifMatch, idempotencyKey, payload)
      }

      if (response.problem && !response.data) {
        lifecycleFeedback.value = mapStatusToCategory(response.status)
        throw new Error(response.problem.detail ?? response.problem.title ?? `Lifecycle action ${action} failed`)
      }

      // Update versionMarker from ETag on success
      versionMarker.value = response.headers.etag || null

      // Refresh detail + history after success
      await Promise.all([
        loadDetail(merchantId, paymentOrderId),
        loadHistory(merchantId, paymentOrderId),
      ])
      return true
    } catch (e: any) {
      // Only set lifecycleFeedback if not already set by problem handling above
      if (!lifecycleFeedback.value) {
        const status = e?.statusCode || e?.response?.status || e?.status
        lifecycleFeedback.value = mapStatusToCategory(status ?? 0)
      }
      throw e
    } finally {
      actionSubmitting.value = false
    }
  }

  function clearLifecycleFeedback() {
    lifecycleFeedback.value = null
  }

  async function saveMetadata(
    merchantId: string,
    paymentOrderId: string,
    metadata: Record<string, unknown>
  ) {
    if (!versionMarker.value) {
      throw new Error('Missing version marker for conditional metadata update')
    }

    metadataSaving.value = true
    lifecycleFeedback.value = null

    try {
      const { patchMetadata } = usePaymentLifecycleApi()
      const response = await patchMetadata(merchantId, paymentOrderId, versionMarker.value, metadata)

      if (response.problem && !response.data) {
        lifecycleFeedback.value = mapStatusToCategory(response.status)
        throw new Error(response.problem.detail ?? response.problem.title ?? 'Metadata update failed')
      }

      // Update versionMarker from ETag on success
      versionMarker.value = response.headers.etag || null

      await loadDetail(merchantId, paymentOrderId)
      return true
    } catch (e: any) {
      // Only set lifecycleFeedback if not already set by problem handling above
      if (!lifecycleFeedback.value) {
        const status = e?.statusCode || e?.response?.status || e?.status
        lifecycleFeedback.value = mapStatusToCategory(status ?? 0)
      }
      throw e
    } finally {
      metadataSaving.value = false
    }
  }

  return {
    loading,
    error,
    insufficientAuthority,
    lastCreatedOrder,
    currentOrder,
    list,
    summary,
    history,
    historyLoading,
    historyError,
    versionMarker,
    lifecycleFeedback,
    actionSubmitting,
    metadataSaving,
    loadList,
    loadSummary,
    loadDetail,
    loadHistory,
    createOrder,
    setLastCreatedOrder,
    clearError,
    reset,
    getAvailableActions,
    submitLifecycleAction,
    clearLifecycleFeedback,
    saveMetadata,
  }
})
