<template>
  <div data-testid="if-match-input">
  <UFormField label="If-Match">
    <UInput
      :model-value="modelValue"
      placeholder="ETag value for conditional update"
      @update:model-value="val => emit('update:modelValue', val)"
    />
  </UFormField>
  </div>
</template>

<script setup lang="ts">
/**
 * Editable If-Match header input, pre-filled with the latest ETag.
 * Caller is responsible for passing the current ETag as the initial modelValue.
 *
 * Requirements: 8.9, 5.3
 */

defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>
