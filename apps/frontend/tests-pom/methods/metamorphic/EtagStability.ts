/**
 * Metamorphic — ETag stability then change (MR-ETAG).
 *
 * GET, no write, GET again ⇒ ETag unchanged.
 * Authorize with that ETag ⇒ next GET ETag differs.
 * Layer: REST. Never seed-learning.
 */

export const mrEtag = { id: 'MR-ETAG' } as const
