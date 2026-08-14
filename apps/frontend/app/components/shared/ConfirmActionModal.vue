<template>
  <div data-testid="confirm-action-modal">
  <UModal
    :open="open"
    @update:open="emit('update:open', $event)"
  >
    <template #content>
      <UCard>
        <template #header>
          <div class="flex items-center justify-between">
            <h3 class="text-base font-semibold text-gray-900 dark:text-gray-100">
              {{ title }}
            </h3>
            <UButton
              color="neutral"
              variant="ghost"
              icon="i-lucide-x"
              size="sm"
              aria-label="Close"
              @click="handleCancel"
            />
          </div>
        </template>

        <p v-if="description" class="text-sm text-gray-600 dark:text-gray-400">
          {{ description }}
        </p>

        <template #footer>
          <div class="flex justify-end gap-3">
            <UButton
              data-testid="confirm-action-dismiss"
              color="neutral"
              variant="ghost"
              @click="handleCancel"
            >
              {{ cancelLabel ?? 'Cancel' }}
            </UButton>
            <UButton
              data-testid="confirm-action-confirm"
              color="error"
              @click="handleConfirm"
            >
              {{ confirmLabel ?? 'Confirm' }}
            </UButton>
          </div>
        </template>
      </UCard>
    </template>
  </UModal>
  </div>
</template>

<script setup lang="ts">
/**
 * Reusable confirmation modal for destructive actions.
 * Focus trap and focus restoration are handled natively by UModal.
 *
 * Requirements: 5.8, 8.11, 9.1 (accessibility)
 */

const props = defineProps<{
  open: boolean
  title: string
  description?: string
  confirmLabel?: string
  cancelLabel?: string
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
  'update:open': [value: boolean]
}>()

function handleConfirm() {
  emit('confirm')
  emit('update:open', false)
}

function handleCancel() {
  emit('cancel')
  emit('update:open', false)
}
</script>
