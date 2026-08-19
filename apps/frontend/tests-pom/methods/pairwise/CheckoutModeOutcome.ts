/**
 * Pairwise checkout mode × outcome (other product world — CPL).
 *
 * Do not mix these rows into payment_orders create/lifecycle specs.
 * Oracle = fulfillment-status, never query status= alone.
 */

export const checkoutModeOutcomes = [
  { id: 'SCN-CPL-01', mode: 'ONLINE', action: 'approve', fulfillment: 'CONFIRMED' },
  { id: 'SCN-CPL-02', mode: 'ONLINE', action: 'lie', fulfillmentNot: 'CONFIRMED' },
  { id: 'SCN-CPL-03', mode: 'CASH', action: 'none', fulfillment: 'CONFIRMED' },
  { id: 'SCN-CPL-04', mode: 'ONLINE', action: 'decline', fulfillment: 'CANCELLED' },
  { id: 'SCN-CPL-06', mode: 'EXPIRED_LINK', action: 'open', testId: 'psp-link-expired' },
] as const
