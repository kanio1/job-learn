/**
 * Kanban legal vs illegal edges (existing payment status machine).
 *
 * Layer: E2E menu is P0; dragTo is P1. HTTP 4xx uses current lifecycle codes.
 */

export const paymentKanbanEdges = {
  createdToAuthorized: { from: 'CREATED', to: 'AUTHORIZED', action: 'authorize' },
  createdToCapturedIllegal: { from: 'CREATED', to: 'CAPTURED', action: 'capture' },
} as const
