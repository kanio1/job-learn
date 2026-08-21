/**
 * UC-M360-20 — open merchant 360 from the registry, inspect sections, Escape.
 *
 * What changes: unique merchant vs activated (suspend confirm).
 * Layer: E2E (dialog, focus, nested ConfirmActionModal). GET detail is waitForBff.
 * Seed: unique factory. Never Alpha ~104. Audit has no entityId filter — History is a link.
 */

export const merchant360Journey = {
  openFromList: {
    id: 'UC-M360-20',
    actor: 'PLATFORM_ADMIN' as const,
    routeStays: '/admin/merchants',
    sections: ['Information', 'Risk', 'Payments', 'History'] as const,
  },
  escapeReturnsFocus: {
    id: 'UC-M360-20',
    dismiss: 'Escape' as const,
  },
  nestedSuspendDismiss: {
    id: 'EG-M360-06',
    confirmDismissMustNotPost: '/suspend' as const,
  },
} as const
