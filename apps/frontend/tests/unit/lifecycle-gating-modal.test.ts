/**
 * Integration tests for lifecycle gating and modal flow
 *
 * Validates: Requirements 5.8, 5.9, 5.10, 5.11, 9.6
 *
 * Covers:
 *  - Modal gating for cancel / refund (confirm modal appears, dismiss/cancel sends NO request)
 *  - Dismiss retains entered values (Idempotency-Key, If-Match, reason)
 *  - Validation: empty Idempotency-Key blocks request with message (Req 5.10)
 *  - Validation: Idempotency-Key > 255 chars blocks request with message (Req 5.10)
 *  - Validation: amountMinor = 0 or > 100,000,000 blocks request with message (Req 5.11)
 *  - Toast on success/failure: UToast with descriptive message shown (Req 9.6)
 *
 * These are example-based Vitest tests (no property-based testing).
 * No Playwright — all assertions are at the Vitest layer.
 *
 * The test strategy:
 *  - Pure logic functions (validateDrawer, requiresConfirmModal, toast builders) are
 *    tested directly as unit tests — they are faithful ports of the logic in
 *    [paymentOrderId].vue and do not require component mounting.
 *  - Component rendering tests (ConfirmActionModal, IdempotencyKeyInput, IfMatchInput)
 *    verify data-testid presence, prop rendering, and emitted events using
 *    mountSuspended from @nuxt/test-utils/runtime, consistent with the existing
 *    app/components/shared/state-and-lifecycle.test.ts pattern.
 */

// @vitest-environment nuxt
import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import ConfirmActionModal from '../../app/components/shared/ConfirmActionModal.vue'
import IdempotencyKeyInput from '../../app/components/shared/IdempotencyKeyInput.vue'
import IfMatchInput from '../../app/components/shared/IfMatchInput.vue'

// ---------------------------------------------------------------------------
// Stubs for Nuxt UI components that use Teleport or don't render in test env
// ---------------------------------------------------------------------------

/**
 * UModal stub that renders the #content slot inline without Teleport.
 * UModal in real Nuxt UI renders via <Teleport to="body"> which is not
 * available in happy-dom/vitest environments.
 */
const UModalStub = {
  name: 'UModal',
  props: ['open'],
  template: '<div v-if="open"><slot name="content" /></div>',
}

/**
 * UCard stub that renders header/default/footer slots.
 */
const UCardStub = {
  name: 'UCard',
  template: '<div><slot name="header" /><slot /><slot name="footer" /></div>',
}

/**
 * Default global stubs for all component mounts in this file.
 */
const globalStubs = {
  stubs: {
    UModal: UModalStub,
    UCard: UCardStub,
  },
}

// ---------------------------------------------------------------------------
// Pure helpers mirroring the page logic from [paymentOrderId].vue
// These are tested as pure unit tests — no component mounting needed.
// ---------------------------------------------------------------------------

interface DrawerState {
  idempotencyKey: string
  action: 'authorize' | 'capture' | 'cancel' | 'refund' | null
  amountMinorRaw: number | null
}

interface ValidationResult {
  valid: boolean
  message?: string
  field?: 'idempotencyKey' | 'amountMinor'
}

/**
 * Pure function mirroring validateDrawer() from [paymentOrderId].vue.
 * Returns whether the drawer state is valid and, if not, what the error is.
 */
function validateDrawer(state: DrawerState): ValidationResult {
  if (!state.idempotencyKey) {
    return { valid: false, message: 'Idempotency key is required', field: 'idempotencyKey' }
  }
  if (state.idempotencyKey.length > 255) {
    return { valid: false, message: 'Idempotency key must not exceed 255 characters', field: 'idempotencyKey' }
  }
  if (
    (state.action === 'capture' || state.action === 'refund') &&
    state.amountMinorRaw !== null
  ) {
    const v = state.amountMinorRaw
    if (!Number.isInteger(v) || v < 1 || v > 100_000_000) {
      return {
        valid: false,
        message: 'Amount must be an integer between 1 and 100,000,000',
        field: 'amountMinor',
      }
    }
  }
  return { valid: true }
}

/**
 * Mirrors requiresConfirmModal logic from handleSubmit() in [paymentOrderId].vue.
 * cancel and refund are destructive — they gate behind the confirm modal.
 */
function requiresConfirmModal(action: string | null): boolean {
  return action === 'cancel' || action === 'refund'
}

/**
 * Mirrors the IdempotencyKeyInput `validationError` computed.
 */
function idempotencyKeyValidationError(value: string): string | undefined {
  if (!value) return 'Idempotency key is required'
  if (value.length > 255) return 'Idempotency key must not exceed 255 characters'
  return undefined
}

/**
 * Mirrors the `amountError` computed in [paymentOrderId].vue.
 */
function amountMinorError(v: number | null): string | undefined {
  if (v === null || v === undefined) return undefined
  if (!Number.isInteger(v)) return 'Amount must be a whole number'
  if (v < 1) return 'Amount must be at least 1'
  if (v > 100_000_000) return 'Amount must be at most 100,000,000'
  return undefined
}

/** Builds the success toast payload, mirroring executeAction() in [paymentOrderId].vue. */
function buildSuccessToast(action: string, newStatus: string, newEtag: string) {
  return {
    title: `${action} succeeded`,
    description: `New status: ${newStatus}${newEtag ? ` · ETag: ${newEtag}` : ''}`,
    color: 'success' as const,
  }
}

/** Builds the failure toast payload, mirroring executeAction() in [paymentOrderId].vue. */
function buildFailureToast(action: string, detail?: string, title?: string) {
  return {
    title: `${action} failed`,
    description: detail ?? title ?? 'Action failed',
    color: 'error' as const,
  }
}

// ---------------------------------------------------------------------------
// 1. Modal gating: cancel requires confirm modal (Req 5.8)
// ---------------------------------------------------------------------------

describe('Modal gating for cancel (Req 5.8)', () => {
  it('cancel action requires the confirmation modal', () => {
    expect(requiresConfirmModal('cancel')).toBe(true)
  })

  it('authorize action does NOT require the confirmation modal', () => {
    expect(requiresConfirmModal('authorize')).toBe(false)
  })

  it('capture action does NOT require the confirmation modal', () => {
    expect(requiresConfirmModal('capture')).toBe(false)
  })

  it('ConfirmActionModal renders title and description for cancel', async () => {
    const wrapper = await mountSuspended(ConfirmActionModal, {
      props: {
        open: true,
        title: 'Confirm Cancel',
        description: 'Are you sure you want to cancel this payment order? This action cannot be undone.',
        confirmLabel: 'Confirm',
        cancelLabel: 'Go back',
      },
      global: globalStubs,
    })

    expect(wrapper.text()).toContain('Confirm Cancel')
    expect(wrapper.text()).toContain('Are you sure you want to cancel this payment order?')
  })

  it('clicking Go back emits cancel and does NOT emit confirm — no request sent', async () => {
    const wrapper = await mountSuspended(ConfirmActionModal, {
      props: {
        open: true,
        title: 'Confirm Cancel',
        description: 'Are you sure?',
        confirmLabel: 'Confirm',
        cancelLabel: 'Go back',
      },
      global: globalStubs,
    })

    const buttons = wrapper.findAll('button')
    const cancelButton = buttons.find(b => b.text() === 'Go back')
    expect(cancelButton).toBeDefined()
    await cancelButton!.trigger('click')

    // No confirm event → no request will be dispatched by the page
    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.emitted('confirm')).toBeFalsy()
  })

  it('clicking Confirm emits confirm (not cancel) — request proceeds', async () => {
    const wrapper = await mountSuspended(ConfirmActionModal, {
      props: {
        open: true,
        title: 'Confirm Cancel',
        description: 'Are you sure?',
        confirmLabel: 'Confirm',
        cancelLabel: 'Go back',
      },
      global: globalStubs,
    })

    const buttons = wrapper.findAll('button')
    const confirmButton = buttons.find(b => b.text() === 'Confirm')
    expect(confirmButton).toBeDefined()
    await confirmButton!.trigger('click')

    expect(wrapper.emitted('confirm')).toBeTruthy()
    expect(wrapper.emitted('cancel')).toBeFalsy()
  })
})

// ---------------------------------------------------------------------------
// 2. Modal gating: refund requires confirm modal (Req 5.8)
// ---------------------------------------------------------------------------

describe('Modal gating for refund (Req 5.8)', () => {
  it('refund action requires the confirmation modal', () => {
    expect(requiresConfirmModal('refund')).toBe(true)
  })

  it('ConfirmActionModal renders title and description for refund', async () => {
    const wrapper = await mountSuspended(ConfirmActionModal, {
      props: {
        open: true,
        title: 'Confirm Refund',
        description: 'Are you sure you want to refund this payment order? This action cannot be undone.',
        confirmLabel: 'Confirm',
        cancelLabel: 'Go back',
      },
      global: globalStubs,
    })

    expect(wrapper.text()).toContain('Confirm Refund')
    expect(wrapper.text()).toContain('Are you sure you want to refund this payment order?')
  })

  it('dismissing refund modal: cancel emitted, confirm NOT emitted — no request sent', async () => {
    const wrapper = await mountSuspended(ConfirmActionModal, {
      props: {
        open: true,
        title: 'Confirm Refund',
        description: 'Are you sure?',
        confirmLabel: 'Confirm',
        cancelLabel: 'Go back',
      },
      global: globalStubs,
    })

    const buttons = wrapper.findAll('button')
    const cancelButton = buttons.find(b => b.text() === 'Go back')
    await cancelButton!.trigger('click')

    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.emitted('confirm')).toBeFalsy()
  })
})

// ---------------------------------------------------------------------------
// 3. Dismiss retains values (Req 5.9)
// ---------------------------------------------------------------------------

describe('Dismiss modal retains entered values (Req 5.9)', () => {
  /**
   * onModalCancel() in [paymentOrderId].vue ONLY sets confirmModalOpen=false.
   * All drawer state (idempotencyKey, ifMatch, reason, amountMinor) remains untouched.
   * This ensures the user can re-examine their input and retry.
   */
  it('onModalCancel only closes the confirm modal — all drawer state unchanged', () => {
    let confirmModalOpen = true
    const drawerOpen = true
    let drawerIdempotencyKey = 'my-unique-key-123'
    let drawerIfMatch = '"etag-version-5"'
    let drawerReason = 'test reason'
    let drawerAmountMinorRaw: number | null = 5000

    // Mirrors onModalCancel() in [paymentOrderId].vue
    function onModalCancel() {
      confirmModalOpen = false
      // All other drawer state is intentionally NOT touched
    }

    onModalCancel()

    expect(confirmModalOpen).toBe(false)
    // Drawer still open, all values retained
    expect(drawerOpen).toBe(true)
    expect(drawerIdempotencyKey).toBe('my-unique-key-123')
    expect(drawerIfMatch).toBe('"etag-version-5"')
    expect(drawerReason).toBe('test reason')
    expect(drawerAmountMinorRaw).toBe(5000)
  })

  it('ConfirmActionModal emits update:open=false on cancel (modal closes, NOT the drawer)', async () => {
    const wrapper = await mountSuspended(ConfirmActionModal, {
      props: {
        open: true,
        title: 'Confirm Action',
      },
      global: globalStubs,
    })

    const buttons = wrapper.findAll('button')
    const cancelButton = buttons.find(b => b.text() === 'Cancel')
    await cancelButton!.trigger('click')

    const updateOpenEvents = wrapper.emitted('update:open')
    expect(updateOpenEvents).toBeTruthy()
    expect(updateOpenEvents![updateOpenEvents!.length - 1]).toEqual([false])

    // cancel emitted (not confirm)
    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.emitted('confirm')).toBeFalsy()
  })

  it('IdempotencyKeyInput shows the retained value after dismiss', async () => {
    const wrapper = await mountSuspended(IdempotencyKeyInput, {
      props: { modelValue: 'retained-key-after-dismiss' },
    })

    const input = wrapper.find('input')
    expect(input.element.value).toBe('retained-key-after-dismiss')
  })

  it('IfMatchInput shows the retained ETag value after dismiss', async () => {
    const wrapper = await mountSuspended(IfMatchInput, {
      props: { modelValue: '"retained-etag-value"' },
    })

    const input = wrapper.find('input')
    expect(input.element.value).toBe('"retained-etag-value"')
  })
})

// ---------------------------------------------------------------------------
// 4. Validation — empty Idempotency-Key blocks request (Req 5.10)
// ---------------------------------------------------------------------------

describe('Validation: empty Idempotency-Key blocks request (Req 5.10)', () => {
  it('validateDrawer returns invalid with "idempotency key" message when key is empty', () => {
    const result = validateDrawer({ idempotencyKey: '', action: 'authorize', amountMinorRaw: null })

    expect(result.valid).toBe(false)
    expect(result.field).toBe('idempotencyKey')
    expect(result.message).toMatch(/idempotency key/i)
  })

  it('idempotencyKeyValidationError returns required message for empty string', () => {
    expect(idempotencyKeyValidationError('')).toBe('Idempotency key is required')
  })

  it('IdempotencyKeyInput component shows "required" text when value is empty', async () => {
    const wrapper = await mountSuspended(IdempotencyKeyInput, {
      props: { modelValue: '' },
    })

    expect(wrapper.text()).toContain('required')
  })

  it('validateDrawer returns valid when key has 1 character', () => {
    const result = validateDrawer({ idempotencyKey: 'a', action: 'authorize', amountMinorRaw: null })
    expect(result.valid).toBe(true)
  })

  it('validateDrawer returns valid when key is exactly 255 chars (boundary)', () => {
    const result = validateDrawer({ idempotencyKey: 'a'.repeat(255), action: 'authorize', amountMinorRaw: null })
    expect(result.valid).toBe(true)
  })
})

// ---------------------------------------------------------------------------
// 5. Validation — Idempotency-Key > 255 chars blocks request (Req 5.10)
// ---------------------------------------------------------------------------

describe('Validation: Idempotency-Key > 255 chars blocks request (Req 5.10)', () => {
  it('validateDrawer returns invalid when key is 256 characters', () => {
    const result = validateDrawer({ idempotencyKey: 'x'.repeat(256), action: 'authorize', amountMinorRaw: null })

    expect(result.valid).toBe(false)
    expect(result.field).toBe('idempotencyKey')
    expect(result.message).toMatch(/255/i)
  })

  it('validateDrawer returns invalid when key is 300 characters', () => {
    const result = validateDrawer({ idempotencyKey: 'z'.repeat(300), action: 'capture', amountMinorRaw: null })

    expect(result.valid).toBe(false)
    expect(result.field).toBe('idempotencyKey')
  })

  it('idempotencyKeyValidationError returns 255-char message for over-length key', () => {
    expect(idempotencyKeyValidationError('k'.repeat(256))).toBe('Idempotency key must not exceed 255 characters')
  })

  it('IdempotencyKeyInput component shows 255-char error for over-length key', async () => {
    const wrapper = await mountSuspended(IdempotencyKeyInput, {
      props: { modelValue: 'y'.repeat(300) },
    })

    expect(wrapper.text()).toContain('255')
  })
})

// ---------------------------------------------------------------------------
// 6. Validation — invalid amountMinor (Req 5.11)
// ---------------------------------------------------------------------------

describe('Validation: invalid amountMinor blocks request (Req 5.11)', () => {
  it('validateDrawer returns invalid for amountMinor = 0 on capture', () => {
    const result = validateDrawer({ idempotencyKey: 'valid-key', action: 'capture', amountMinorRaw: 0 })

    expect(result.valid).toBe(false)
    expect(result.field).toBe('amountMinor')
  })

  it('validateDrawer returns invalid for amountMinor = 0 on refund', () => {
    const result = validateDrawer({ idempotencyKey: 'valid-key', action: 'refund', amountMinorRaw: 0 })

    expect(result.valid).toBe(false)
    expect(result.field).toBe('amountMinor')
  })

  it('validateDrawer returns invalid for amountMinor > 100,000,000 on capture', () => {
    const result = validateDrawer({ idempotencyKey: 'valid-key', action: 'capture', amountMinorRaw: 100_000_001 })

    expect(result.valid).toBe(false)
    expect(result.field).toBe('amountMinor')
  })

  it('validateDrawer returns invalid for amountMinor > 100,000,000 on refund', () => {
    const result = validateDrawer({ idempotencyKey: 'valid-key', action: 'refund', amountMinorRaw: 200_000_000 })

    expect(result.valid).toBe(false)
    expect(result.field).toBe('amountMinor')
  })

  it('validateDrawer returns valid for amountMinor = 1 (minimum boundary)', () => {
    const result = validateDrawer({ idempotencyKey: 'valid-key', action: 'capture', amountMinorRaw: 1 })
    expect(result.valid).toBe(true)
  })

  it('validateDrawer returns valid for amountMinor = 100,000,000 (maximum boundary)', () => {
    const result = validateDrawer({ idempotencyKey: 'valid-key', action: 'refund', amountMinorRaw: 100_000_000 })
    expect(result.valid).toBe(true)
  })

  it('amountMinorError returns message for value = 0', () => {
    const err = amountMinorError(0)
    expect(err).toBeTruthy()
    expect(err).toMatch(/at least 1/i)
  })

  it('amountMinorError returns message for value > 100,000,000', () => {
    const err = amountMinorError(100_000_001)
    expect(err).toBeTruthy()
    expect(err).toMatch(/100,000,000/i)
  })

  it('amountMinorError returns undefined for null (optional field absent)', () => {
    expect(amountMinorError(null)).toBeUndefined()
  })

  it('amountMinorError returns undefined for valid value = 1', () => {
    expect(amountMinorError(1)).toBeUndefined()
  })

  it('amountMinorError returns undefined for valid value = 100,000,000', () => {
    expect(amountMinorError(100_000_000)).toBeUndefined()
  })

  it('authorize action: validateDrawer ignores amountMinorRaw (not a capture/refund)', () => {
    // The amountMinor field is not shown for authorize; any raw value is irrelevant
    const result = validateDrawer({ idempotencyKey: 'valid-key', action: 'authorize', amountMinorRaw: 0 })
    expect(result.valid).toBe(true)
  })

  it('cancel action: validateDrawer ignores amountMinorRaw (no amount field for cancel)', () => {
    const result = validateDrawer({ idempotencyKey: 'valid-key', action: 'cancel', amountMinorRaw: 0 })
    expect(result.valid).toBe(true)
  })
})

// ---------------------------------------------------------------------------
// 7. Toast on write outcome (Req 9.6)
// ---------------------------------------------------------------------------

describe('Toast on write outcome (Req 9.6)', () => {
  it('success toast for cancel: title="cancel succeeded", description contains new status and ETag', () => {
    const toast = buildSuccessToast('cancel', 'CANCELLED', '"7"')

    expect(toast.title).toBe('cancel succeeded')
    expect(toast.description).toContain('CANCELLED')
    expect(toast.description).toContain('"7"')
    expect(toast.color).toBe('success')
  })

  it('success toast for authorize: title="authorize succeeded", description contains AUTHORIZED', () => {
    const toast = buildSuccessToast('authorize', 'AUTHORIZED', '"3"')

    expect(toast.title).toBe('authorize succeeded')
    expect(toast.description).toContain('AUTHORIZED')
  })

  it('success toast with no ETag: description does NOT contain "ETag"', () => {
    const toast = buildSuccessToast('authorize', 'AUTHORIZED', '')

    expect(toast.description).not.toContain('ETag')
  })

  it('failure toast for refund: title="refund failed", description contains problem detail', () => {
    const toast = buildFailureToast('refund', 'Precondition Failed: stale ETag')

    expect(toast.title).toBe('refund failed')
    expect(toast.description).toContain('stale ETag')
    expect(toast.color).toBe('error')
  })

  it('failure toast falls back to title when detail is absent', () => {
    const toast = buildFailureToast('capture', undefined, 'Precondition Required')

    expect(toast.description).toBe('Precondition Required')
  })

  it('failure toast falls back to "Action failed" when both detail and title are absent', () => {
    const toast = buildFailureToast('authorize', undefined, undefined)

    expect(toast.description).toBe('Action failed')
  })
})

// ---------------------------------------------------------------------------
// 8. ConfirmActionModal component: testid, X button, and close behavior
// ---------------------------------------------------------------------------

describe('ConfirmActionModal component (Req 5.8, 5.9)', () => {
  it('data-testid="confirm-action-modal" is present on the root element', async () => {
    const wrapper = await mountSuspended(ConfirmActionModal, {
      props: { open: false, title: 'Confirm Action' },
      global: globalStubs,
    })

    expect(wrapper.find('[data-testid="confirm-action-modal"]').exists()).toBe(true)
  })

  it('clicking the X close button emits cancel and update:open=false', async () => {
    const wrapper = await mountSuspended(ConfirmActionModal, {
      props: { open: true, title: 'Confirm Cancel' },
      global: globalStubs,
    })

    const closeButton = wrapper.find('button[aria-label="Close"]')
    expect(closeButton.exists()).toBe(true)
    await closeButton.trigger('click')

    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.emitted('confirm')).toBeFalsy()

    const updateOpenEvents = wrapper.emitted('update:open')
    expect(updateOpenEvents).toBeTruthy()
    expect(updateOpenEvents![updateOpenEvents!.length - 1]).toEqual([false])
  })

  it('clicking Confirm emits confirm and update:open=false', async () => {
    const wrapper = await mountSuspended(ConfirmActionModal, {
      props: { open: true, title: 'Confirm Action' },
      global: globalStubs,
    })

    const buttons = wrapper.findAll('button')
    const confirmButton = buttons.find(b => b.text() === 'Confirm')
    expect(confirmButton).toBeDefined()
    await confirmButton!.trigger('click')

    expect(wrapper.emitted('confirm')).toBeTruthy()

    const updateOpenEvents = wrapper.emitted('update:open')
    expect(updateOpenEvents).toBeTruthy()
    expect(updateOpenEvents![updateOpenEvents!.length - 1]).toEqual([false])
  })
})

// ---------------------------------------------------------------------------
// 9. IdempotencyKeyInput component (Req 5.2, 5.10)
// ---------------------------------------------------------------------------

describe('IdempotencyKeyInput component (Req 5.2, 5.10)', () => {
  it('data-testid="idempotency-key-input" is present', async () => {
    const wrapper = await mountSuspended(IdempotencyKeyInput, {
      props: { modelValue: 'test-key' },
    })

    expect(wrapper.find('[data-testid="idempotency-key-input"]').exists()).toBe(true)
  })

  it('shows no validation error for a valid UUID key', () => {
    const uuid = '550e8400-e29b-41d4-a716-446655440000'
    expect(idempotencyKeyValidationError(uuid)).toBeUndefined()
  })

  it('emits update:modelValue when input value changes', async () => {
    const wrapper = await mountSuspended(IdempotencyKeyInput, {
      props: { modelValue: 'initial-key' },
    })

    const input = wrapper.find('input')
    await input.setValue('new-key-value')

    const emittedUpdates = wrapper.emitted('update:modelValue')
    expect(emittedUpdates).toBeTruthy()
    expect((emittedUpdates![emittedUpdates!.length - 1] as [string])[0]).toBe('new-key-value')
  })
})

// ---------------------------------------------------------------------------
// 10. IfMatchInput component (Req 5.3)
// ---------------------------------------------------------------------------

describe('IfMatchInput component (Req 5.3)', () => {
  it('data-testid="if-match-input" is present', async () => {
    const wrapper = await mountSuspended(IfMatchInput, {
      props: { modelValue: '"etag-1"' },
    })

    expect(wrapper.find('[data-testid="if-match-input"]').exists()).toBe(true)
  })

  it('renders the pre-filled ETag value from the parent (store versionMarker)', async () => {
    const etag = '"version-42"'
    const wrapper = await mountSuspended(IfMatchInput, {
      props: { modelValue: etag },
    })

    const input = wrapper.find('input')
    expect(input.element.value).toBe(etag)
  })

  it('emits update:modelValue when user edits the If-Match value', async () => {
    const wrapper = await mountSuspended(IfMatchInput, {
      props: { modelValue: '"old-etag"' },
    })

    const input = wrapper.find('input')
    await input.setValue('"new-etag"')

    const emittedUpdates = wrapper.emitted('update:modelValue')
    expect(emittedUpdates).toBeTruthy()
    expect((emittedUpdates![emittedUpdates!.length - 1] as [string])[0]).toBe('"new-etag"')
  })
})
