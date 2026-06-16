<template>
  <UForm
    ref="formRef"
    :schema="createMerchantSchema"
    :state="formState"
    data-testid="create-merchant-form"
    class="space-y-4"
    @submit="onSubmit"
  >
    <UFormField label="Merchant Reference" name="merchantReference">
      <UInput
        v-model="formState.merchantReference"
        placeholder="e.g. MERCH-001"
        aria-label="Merchant reference"
      />
    </UFormField>

    <UFormField label="Display Name" name="displayName">
      <UInput
        v-model="formState.displayName"
        placeholder="e.g. Acme Payments Inc."
        aria-label="Display name"
      />
    </UFormField>

    <UAlert
      v-if="error"
      color="error"
      variant="subtle"
      icon="i-lucide-circle-alert"
      :description="error"
      role="alert"
    />

    <div class="flex justify-end gap-2">
      <UButton variant="outline" type="button" @click="$emit('cancel')">
        Cancel
      </UButton>
      <UButton type="submit" color="primary" :loading="submitting">
        Create
      </UButton>
    </div>
  </UForm>
</template>

<script setup lang="ts">
import { createMerchantSchema } from '~/schemas/merchant.schema'
import type { CreateMerchantForm } from '~/schemas/merchant.schema'

/**
 * Create merchant form with Zod field-level validation via UForm.
 *
 * - `data-testid="create-merchant-form"` on the form element (Req 12.1)
 * - Field-level messages rendered by UFormField from the Zod schema (Req 2.5, 10.2)
 * - User input is retained on server-side error (`error` prop; Req 2.9)
 * - Does NOT send request on invalid input (Req 2.5, 10.1)
 */

defineProps<{
  error?: string | null
  submitting?: boolean
}>()

const emit = defineEmits<{
  submit: [value: CreateMerchantForm]
  cancel: []
}>()

const formState = reactive<Partial<CreateMerchantForm>>({
  merchantReference: '',
  displayName: '',
})

function onSubmit() {
  // UForm validates against `createMerchantSchema` before calling this handler.
  // If validation fails, UForm shows field-level messages and does not call onSubmit.
  emit('submit', formState as CreateMerchantForm)
}
</script>
