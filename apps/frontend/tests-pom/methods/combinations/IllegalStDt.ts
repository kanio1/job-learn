/**
 * Combination ST + DT — illegal edges with a fresh If-Match.
 *
 * Why together: ST names the forbidden from→action; DT keeps If-Match valid
 * so a 422 is the transition, not a 428/412 precondition miss.
 * SCN-ILL-02 refund as merchant.manager is 409 dual_control (gate before ST).
 * Layer: REST only. E2E already owns legal LIF-01/02/03 + ConfirmModal.
 */

import { paymentStatusEdges } from '../state/PaymentStatusMachine'

export const illegalStDtRest = paymentStatusEdges.filter(edge => edge.id.startsWith('SCN-ILL-'))

export const headerStDtRest = paymentStatusEdges.filter(edge =>
  edge.id === 'SCN-LIF-03' || edge.id === 'SCN-LIF-04' || edge.id === 'SCN-LIF-05',
)
