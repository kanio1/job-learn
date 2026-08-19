/**
 * BVA — headers on authorize (on/off around If-Match and Idempotency-Key).
 *
 * What changes: header presence/shape, not the order amount.
 * Layer: REST. "v99" is a stale version (412). "stale-etag" is malformed (400).
 * Seed: unique CREATED order on ALPHA_001. Never seed-learning.
 */

export type HeaderBoundary = {
  id: string
  ifMatch: 'fresh' | 'absent' | 'v99' | 'malformed'
  omitIdempotencyKey?: boolean
  expectStatus: 200 | 400 | 412 | 428
}

export const authorizeHeaderBoundaries: readonly HeaderBoundary[] = [
  { id: 'SCN-LIF-01-ON', ifMatch: 'fresh', expectStatus: 200 },
  { id: 'SCN-LIF-04', ifMatch: 'absent', expectStatus: 428 },
  { id: 'SCN-LIF-03', ifMatch: 'v99', expectStatus: 412 },
  { id: 'SCN-LIF-05', ifMatch: 'malformed', expectStatus: 400 },
]
