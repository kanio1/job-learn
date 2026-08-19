import type { Page, Request, Response } from '@playwright/test'

/**
 * Wait for an exact BFF pathname (no prefix match on /history or /evidence).
 * What changes between calls: method + pathExact, not a regex over the whole URL.
 */
export function waitForBffRequest(
  page: Page,
  match: { method: string, pathExact: string },
): Promise<Request> {
  return page.waitForRequest((request) => {
    if (request.method() !== match.method) {
      return false
    }
    try {
      return new URL(request.url()).pathname === match.pathExact
    }
    catch {
      return false
    }
  })
}

export function waitForBffResponse(
  page: Page,
  match: { method: string, pathExact: string },
): Promise<Response> {
  return page.waitForResponse((response) => {
    if (response.request().method() !== match.method) {
      return false
    }
    try {
      return new URL(response.url()).pathname === match.pathExact
    }
    catch {
      return false
    }
  })
}
