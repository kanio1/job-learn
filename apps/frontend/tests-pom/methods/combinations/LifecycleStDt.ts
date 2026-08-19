/**
 * Combination ST + DT If-Match (UC-W2-09).
 *
 * Why together: ST names the legal edge; DT names the HTTP precondition.
 * ST without DT = happy-only. DT without ST = 412 in the wrong state.
 * E2E: SCN-LIF-01/02/03. REST/RA: absent / malformed / illegal edge.
 */

import { paymentStatusEdges } from '../state/PaymentStatusMachine'

export const lifecycleStDtE2e = paymentStatusEdges.filter(edge =>
  edge.id === 'SCN-LIF-01' || edge.id === 'SCN-LIF-02' || edge.id === 'SCN-LIF-03',
)
