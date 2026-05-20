<template>
  <form @submit.prevent="onSubmit">
    <UFormField label="Merchant Reference" name="merchantReference">
      <UInput
        v-model="formState.merchantReference"
        placeholder="e.g. MERCH-001"
        aria-label="Merchant reference"
      />
      <p v-if="fieldErrors.merchantReference" class="mt-1 text-sm text-error" role="alert">
        {{ fieldErrors.merchantReference }}
      </p>
    </UFormField>
    <UFormField label="Display Name" name="displayName" class="mt-4">
      <UInput
        v-model="formState.displayName"
        placeholder="e.g. Acme Payments Inc."
        aria-label="Display name"
      />
      <p v-if="fieldErrors.displayName" class="mt-1 text-sm text-error" role="alert">
        {{ fieldErrors.displayName }}
      </p>
    </UFormField>
    <p v-if="error" class="mt-2 text-sm text-error" role="alert">
      {{ error }}
    </p>
    <div class="mt-4 flex justify-end gap-2">
      <UButton variant="outline" type="button" @click="$emit('cancel')">
        Cancel
      </UButton>
      <UButton type="submit" color="primary" :loading="submitting">
        Create
      </UButton>
    </div>
  </form>
</template>

<script setup lang="ts">
import { createMerchantSchema } from '~/schemas/merchant.schema'
import type { CreateMerchantForm } from '~/schemas/merchant.schema'

defineProps<{
  error?: string | null
  submitting?: boolean
}>()

const emit = defineEmits<{
  submit: [value: CreateMerchantForm]
  cancel: []
}>()

const formState = reactive<CreateMerchantForm>({
  merchantReference: '',
  displayName: ''
})

const fieldErrors = reactive<Partial<Record<keyof CreateMerchantForm, string>>>({})

function onSubmit() {
  fieldErrors.merchantReference = undefined
  fieldErrors.displayName = undefined

  const result = createMerchantSchema.safeParse(formState)
  if (!result.success) {
    const flattened = result.error.flatten().fieldErrors
    fieldErrors.merchantReference = flattened.merchantReference?.[0]
    fieldErrors.displayName = flattened.displayName?.[0]
    return
  }

  emit('submit', result.data)
}
</script>
