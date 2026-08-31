import type { Page, Request } from '@playwright/test'

/**
 * Observes one action window and always unregisters its listener, including
 * when the action's UI assertion fails. It is intentionally not an event bus.
 */
export async function observeRequests<T>(
  page: Page,
  matches: (request: Request) => boolean,
  action: () => Promise<T>,
): Promise<{ readonly requests: readonly Request[], readonly result: T }> {
  const requests: Request[] = []
  const listener = (request: Request) => {
    if (matches(request)) requests.push(request)
  }
  page.on('request', listener)
  try {
    const result = await action()
    return { requests, result }
  }
  finally {
    page.off('request', listener)
  }
}
