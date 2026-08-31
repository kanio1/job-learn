/**
 * EP/BVA — amountMinor / currency on create payment-order.
 *
 * What changes: numeric/currency partition. Same ALPHA_001 + unique key.
 * Layer: REST only. Do not turn each row into an E2E.
 * Seed: unique factory. Never seed-learning.
 */

export type AmountPartition = {
  id: string
  amountMinor: number
  currency: string
  expectStatus: 201 | 400
}

/** Domain/Bean Validation: 1 .. 100_000_000 inclusive (`PaymentAmount`, `@Min`/`@Max`). */
export const MAX_AMOUNT_MINOR = 100_000_000

export const amountPartitions = [
  { id: 'SCN-PAY-06', amountMinor: 0, currency: 'PLN', expectStatus: 400 },
  { id: 'SCN-PAY-07', amountMinor: 1, currency: 'PLN', expectStatus: 201 },
  { id: 'SCN-PAY-10', amountMinor: 2, currency: 'PLN', expectStatus: 201 },
  { id: 'SCN-PAY-11', amountMinor: MAX_AMOUNT_MINOR, currency: 'PLN', expectStatus: 201 },
  { id: 'SCN-PAY-08', amountMinor: MAX_AMOUNT_MINOR + 1, currency: 'PLN', expectStatus: 400 },
  { id: 'SCN-PAY-09a', amountMinor: 1999, currency: 'PL', expectStatus: 400 },
  { id: 'SCN-PAY-09b', amountMinor: 1999, currency: 'XXX', expectStatus: 400 },
  { id: 'SCN-PAY-09c', amountMinor: 1999, currency: 'PLNX', expectStatus: 400 },
] as const satisfies readonly AmountPartition[]
