<template>
  <div data-testid="idempotency-key-input">
  <UFormField label="Idempotency Key" :error="validationError">
    <UInput
      :model-value="modelValue"
      placeholder="UUID v4 idempotency key"
      :maxlength="255"
      @update:model-value="onUpdate"
    />
  </UFormField>
  </div>
</template>

<script setup lang="ts">
/**
 * Editable idempotency key input.
 * - On mount: auto-generates a UUID v4 if no value is provided.
 * - Validates: non-empty, max 255 characters.
 * - Emits update:modelValue for v-model usage.
 *
 * Requirements: 8.8, 5.2, 5.10
 */

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const validationError = computed<string | undefined>(() => {
  if (!props.modelValue) return 'Idempotency key is required'
  if (props.modelValue.length > 255) return 'Idempotency key must not exceed 255 characters'
  return undefined
})

function onUpdate(val: string) {
  emit('update:modelValue', val)
}

onMounted(() => {
  if (!props.modelValue) {
    emit('update:modelValue', crypto.randomUUID())
  }
})
</script>
