/**
 * Metamorphic — filter inclusion (MR-FILTER).
 *
 * Narrow minAmount=A then wider minAmount=A'<A: every ref from the narrow
 * list is in the wide list. EP gives the cut; MR is the inclusion.
 * Layer: REST list (UI already has a separate filter E2E). Never seed-learning.
 */

export const mrFilter = {
  id: 'MR-FILTER',
  highAmount: 8800,
  lowAmount: 1100,
  narrowMin: 5000,
  wideMin: 1000,
} as const
