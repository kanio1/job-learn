export const usePaymentOrdersStore = defineStore('payment-orders', () => {
  const loading = ref(false)
  const error = ref<string | null>(null)
  const lastCreatedOrder = ref<any>(null)
  const currentOrder = ref<any>(null)

  function reset() {
    error.value = null
    lastCreatedOrder.value = null
    currentOrder.value = null
  }

  return { loading, error, lastCreatedOrder, currentOrder, reset }
})
