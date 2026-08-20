/**
 * Metamorphic — list ⊆ summary (MR-SUMMARY).
 *
 * GET list totalElements (or content length) ≤ GET summary.totalOrders
 * for the same merchant and no tighter filter. Summary is the reporting
 * oracle; the table is a page. Layer: REST + UI cards. Never seed-learning.
 */

export const mrSummary = {
  id: 'MR-SUMMARY',
} as const
