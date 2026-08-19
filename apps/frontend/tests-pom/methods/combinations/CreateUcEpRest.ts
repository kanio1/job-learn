/**
 * Combination UC happy + EP on REST (UC-W2-08).
 *
 * Why together: one E2E journey; amount/currency partitions stay on BffClient.
 * 15 E2E on 400 is waste. Not CPL POST /sessions.
 */

import { createOrderJourney } from '../use-case/CreateOrderJourney'
import { amountPartitions } from '../ep-bva/AmountPartitions'
import { orderReferencePartitions } from '../ep-bva/OrderReferencePartitions'
import { idempotencyKeyBoundaries, idempotencyMatrix } from '../decision-table/IdempotencyMatrix'

export const createUcEpRest = {
  journey: createOrderJourney,
  idempotency: idempotencyMatrix,
  amountPartitions,
  orderReferencePartitions,
  idempotencyKeyBoundaries,
}
