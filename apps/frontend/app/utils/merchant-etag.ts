/** Format merchant optimistic-lock If-Match from GET ETag or body version. */
export function merchantIfMatch(etag?: string | null, version?: number): string {
  if (etag && etag.length > 0) {
    return etag
  }
  const value = version ?? 0
  return `"v${value}"`
}
