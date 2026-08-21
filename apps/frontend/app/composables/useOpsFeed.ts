import { opsFeedFrameSchema, opsFeedRecentSchema, type OpsFeedFrame } from '~/schemas/ops-feed.schema'
import { mergeOpsFeedEvents, opsFeedSocketUrl, shouldReconnectOpsFeed } from '~/utils/opsFeed'

export function useOpsFeed() {
  const { request } = useApiClient()
  const toast = useAppToast()
  const events = ref<OpsFeedFrame[]>([])
  const connected = ref(false)
  let socket: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let stopped = false
  let handshakeDenied = false

  function applyIncoming(incoming: OpsFeedFrame[]) {
    events.value = mergeOpsFeedEvents(events.value, incoming)
  }

  function handlePayload(raw: string) {
    try {
      const parsed: unknown = JSON.parse(raw)
      const result = opsFeedFrameSchema.safeParse(parsed)
      if (!result.success) {
        toast.warning('Ignored invalid event')
        return
      }
      applyIncoming([result.data])
      if (result.data.type === 'PAYMENT_FAILED'
        || result.data.type === 'REFUND_APPROVAL_NEEDED'
        || result.data.type === 'SUPPORT_CASE_ASSIGNED') {
        void useNotificationStore().refresh()
      }
    }
    catch {
      toast.warning('Ignored invalid event')
    }
  }

  async function loadRecent() {
    const response = await request('/api/ops/feed/recent', opsFeedRecentSchema)
    if (response.data?.events) {
      applyIncoming(response.data.events)
    }
  }

  function clearReconnectTimer() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  function connect() {
    if (stopped || handshakeDenied || import.meta.server) {
      return
    }
    clearReconnectTimer()
    const url = opsFeedSocketUrl()
    socket = new WebSocket(url)
    socket.addEventListener('open', () => {
      connected.value = true
      void loadRecent()
    })
    socket.addEventListener('message', (message) => {
      handlePayload(String(message.data))
    })
    socket.addEventListener('close', (event) => {
      connected.value = false
      if (stopped) {
        return
      }
      if (!shouldReconnectOpsFeed(event.code)) {
        handshakeDenied = true
        clearReconnectTimer()
        return
      }
      reconnectTimer = setTimeout(() => connect(), 1000)
    })
    socket.addEventListener('error', () => {
      connected.value = false
    })
  }

  function onOffline() {
    connected.value = false
    socket?.close()
  }

  function onOnline() {
    if (!stopped && !handshakeDenied) {
      connect()
    }
  }

  onMounted(() => {
    stopped = false
    connect()
    window.addEventListener('offline', onOffline)
    window.addEventListener('online', onOnline)
  })

  onBeforeUnmount(() => {
    stopped = true
    window.removeEventListener('offline', onOffline)
    window.removeEventListener('online', onOnline)
    clearReconnectTimer()
    socket?.close()
  })

  return { events, connected }
}
