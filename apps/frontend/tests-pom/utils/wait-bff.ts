import type { Page, Request, Response } from '@playwright/test'

type BffMatch = {
  method: string
  pathExact: string
  queryExact?: Readonly<Record<string, string>>
}

function matchesBffUrl(urlString: string, match: BffMatch): boolean {
  try {
    const url = new URL(urlString)
    if (url.pathname !== match.pathExact) return false
    if (!match.queryExact) return true
    const actual = [...url.searchParams.entries()].sort()
    const expected = Object.entries(match.queryExact).sort()
    return actual.length === expected.length && actual.every(([key, value], index) => {
      const expectedEntry = expected[index]
      return expectedEntry !== undefined && key === expectedEntry[0] && value === expectedEntry[1]
    })
  }
  catch {
    return false
  }
}

/**
 * Wait for an exact BFF pathname (no prefix match on /history or /evidence).
 * What changes between calls: method + pathExact, not a regex over the whole URL.
 */
export function waitForBffRequest(
  page: Page,
  match: BffMatch,
): Promise<Request> {
  return page.waitForRequest((request) => {
    if (request.method() !== match.method) {
      return false
    }
    return matchesBffUrl(request.url(), match)
  })
}

export function waitForBffResponse(
  page: Page,
  match: BffMatch,
): Promise<Response> {
  return page.waitForResponse((response) => {
    if (response.request().method() !== match.method) {
      return false
    }
    return matchesBffUrl(response.url(), match)
  })
}
