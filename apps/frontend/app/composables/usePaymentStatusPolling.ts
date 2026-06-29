type RefreshStatusResult = {
  status?: string | null
}

export function usePaymentStatusPolling(refreshStatus: () => Promise<RefreshStatusResult | null | undefined>) {
  const isRefreshing = ref(false)
  const autoRefreshEnabled = ref(false)
  const lastCheckedAt = ref<string | null>(null)
  const error = ref<string | null>(null)

  let intervalId: ReturnType<typeof setInterval> | null = null

  async function refresh() {
    if (isRefreshing.value) return null
    isRefreshing.value = true
    error.value = null

    try {
      const result = await refreshStatus()
      lastCheckedAt.value = new Date().toISOString()
      return result ?? null
    } catch (e: unknown) {
      const err = e as { message?: string; statusMessage?: string }
      error.value = err?.statusMessage || err?.message || 'Status refresh failed.'
      stop()
      return null
    } finally {
      isRefreshing.value = false
    }
  }

  function start(intervalMs = 1000) {
    if (intervalId) return
    autoRefreshEnabled.value = true
    intervalId = setInterval(() => {
      void refresh()
    }, intervalMs)
  }

  function stop() {
    autoRefreshEnabled.value = false
    if (intervalId) {
      clearInterval(intervalId)
      intervalId = null
    }
  }

  function setAutoRefresh(enabled: boolean, intervalMs = 1000) {
    if (enabled) {
      start(intervalMs)
    } else {
      stop()
    }
  }

  onBeforeUnmount(stop)

  return {
    isRefreshing,
    autoRefreshEnabled,
    lastCheckedAt,
    error,
    refresh,
    setAutoRefresh,
    stop,
  }
}
