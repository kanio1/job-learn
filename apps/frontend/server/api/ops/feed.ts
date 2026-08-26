import { createEvent } from 'h3'
// `ws` publishes no bundled declaration file; Nitro still needs this static import
// to include the runtime client in the standalone server output.
// @ts-expect-error TS7016
import WebSocket from 'ws'



type WsClient = {
  on(event: 'open', listener: () => void): void
  on(event: 'message', listener: (data: string | Uint8Array) => void): void
  on(event: 'close', listener: (code: number, reason: Uint8Array) => void): void
  on(event: 'error', listener: () => void): void
  on(event: 'unexpected-response', listener: (request: { url?: string }, response: { statusCode?: number }) => void): void
  send(data: string): void
  close(): void
  readyState: number
}

type FeedPeer = {
  request: {
    url: string
    headers: {
      get(name: string): string | null
      forEach(fn: (value: string, key: string) => void): void
    }
  }
  context: { upstream?: WsClient }
  send(data: string): void
  close(code?: number, reason?: string): void
}

function messageText(data: string | Uint8Array): string {
  return data instanceof Uint8Array ? new TextDecoder().decode(data) : data
}

function createUpstream(url: string, accessToken: string): WsClient {
  return new WebSocket(url, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
}

function sessionEvent(peer: FeedPeer) {
  const headers: Record<string, string> = {}
  peer.request.headers.forEach((value, key) => {
    headers[key.toLowerCase()] = value
  })
  const req = {
    method: 'GET',
    url: peer.request.url || '/api/ops/feed',
    headers,
  }
  const res = {
    statusCode: 200,
    setHeader() {},
    getHeader() {
      return undefined
    },
    end() {},
  }
  // SAFETY: Cookie header is enough for nuxt-auth-utils; the dummy res is unused.
  const nodeReq: Parameters<typeof createEvent>[0] = req as Parameters<typeof createEvent>[0]
  // SAFETY: requireUserSession never writes the upgrade response.
  const nodeRes: Parameters<typeof createEvent>[1] = res as never
  return createEvent(nodeReq, nodeRes)
}

/**
 * Same-origin browser WS → Spring Bearer from the sealed session.
 * Token is never placed on the URL, query string, or browser JS.
 */
export default defineWebSocketHandler({
  async open(peer) {
    // SAFETY: crossws Peer exposes upgrade request headers and a mutable context bag.
    const bag = peer as FeedPeer
    let accessToken: string | undefined
    try {
      const session = await requireUserSession(sessionEvent(bag))
      accessToken = session.secure?.accessToken
    }
    catch {
      bag.close(4401, 'Unauthorized')
      return
    }
    if (!accessToken) {
      bag.close(4401, 'Unauthorized')
      return
    }
    const backendUrl = useRuntimeConfig().public.apiBaseUrl || 'http://localhost:8080'
    const wsUrl = `${backendUrl.replace(/^http/i, 'ws')}/api/ops/feed`
    const upstream = createUpstream(wsUrl, accessToken)
    bag.context.upstream = upstream
    upstream.on('message', (data) => {
      bag.send(messageText(data))
    })
    upstream.on('close', (code, reason) => {
      bag.close(code, new TextDecoder().decode(reason))
    })
    upstream.on('error', () => {
      bag.close(1011, 'upstream')
    })
    upstream.on('unexpected-response', (_request, response) => {
      const status = response.statusCode ?? 500
      bag.close(status === 401 ? 4401 : 1011, status === 401 ? 'Unauthorized' : 'upstream')
    })
  },
  message(peer, message) {
    // SAFETY: crossws Peer exposes upgrade request headers and a mutable context bag.
    const upstream = (peer as FeedPeer).context.upstream as WsClient | undefined
    if (upstream && upstream.readyState === 1) {
      upstream.send(message.text())
    }
  },
  close(peer) {
    // SAFETY: crossws Peer exposes upgrade request headers and a mutable context bag.
    const upstream = (peer as FeedPeer).context.upstream as WsClient | undefined
    if (upstream && upstream.readyState === 1) {
      upstream.close()
    }
  },
})
