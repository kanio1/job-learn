<template>
  <div data-testid="rule-configurator" class="space-y-5">
    <UFormField label="Auto capture" name="autoCapture">
      <USwitch
        v-model="autoCapture"
        label="Auto capture"
        data-testid="policy-auto-capture"
      />
    </UFormField>

    <UFormField
      label="Max auto-capture amount (minor units)"
      name="maxAutoCaptureMinor"
      :error="maxError || undefined"
    >
      <div
        data-testid="policy-max-auto-capture"
        :aria-disabled="autoCapture ? undefined : 'true'"
      >
        <UInputNumber
          v-model="maxAutoCaptureMinor"
          :disabled="!autoCapture"
          :min="autoCapture ? 1 : 0"
          :required="autoCapture"
          aria-label="Max auto-capture amount"
          placeholder="Required when auto capture is on"
        />
      </div>
      <p v-if="maxError" data-testid="policy-max-error" class="text-sm text-error mt-1">
        {{ maxError }}
      </p>
    </UFormField>

    <UFormField :label="`Risk threshold (${riskThreshold})`" name="riskThreshold">
      <div data-testid="policy-risk-threshold">
        <USlider
          v-model="riskThreshold"
          :min="0"
          :max="100"
          :step="1"
          aria-label="Risk threshold"
        />
      </div>
    </UFormField>

    <URadioGroup
      v-model="refundPolicy"
      legend="Refund policy"
      :items="refundItems"
      orientation="horizontal"
      loop
      data-testid="policy-refund-policy"
    />
  </div>
</template>

<script setup lang="ts">
const autoCapture = defineModel<boolean>('autoCapture', { required: true })
const maxAutoCaptureMinor = defineModel<number | null>('maxAutoCaptureMinor', { required: true })
const riskThreshold = defineModel<number>('riskThreshold', { required: true })
const refundPolicy = defineModel<'MANUAL' | 'AUTOMATIC'>('refundPolicy', { required: true })

defineProps<{
  maxError?: string | null
}>()

const refundItems: Array<{ label: string, value: 'MANUAL' | 'AUTOMATIC' }> = [
  { label: 'Manual', value: 'MANUAL' },
  { label: 'Automatic', value: 'AUTOMATIC' },
]

watch(autoCapture, (enabled) => {
  if (!enabled) {
    maxAutoCaptureMinor.value = 0
    return
  }
  if (maxAutoCaptureMinor.value == null || maxAutoCaptureMinor.value < 1) {
    maxAutoCaptureMinor.value = null
  }
})
</script>
