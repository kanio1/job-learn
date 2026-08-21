<template>
  <div data-testid="refund-pin-challenge" class="space-y-3">
    <p class="text-sm">Confirm high-value refund. Enter the 6-digit PIN.</p>
    <p v-if="expiresLabel" class="text-xs text-muted" data-testid="refund-pin-timer">
      Expires in {{ expiresLabel }}
    </p>
    <UProgress v-if="progress != null" :model-value="progress" :max="100" />
    <UPinInput
      v-model="pin"
      :length="6"
      type="number"
      otp
      aria-label="Refund approval PIN"
      data-testid="refund-pin-input"
      @complete="onComplete"
    />
    <p v-if="errorMessage" class="text-sm text-red-600" data-testid="refund-pin-error">
      {{ errorMessage }}
    </p>
    <UAlert v-if="locked" color="error" title="Too many attempts" data-testid="refund-pin-locked" />
    <UButton
      data-testid="refund-pin-verify"
      :disabled="pinValue.length < 6 || verifying || locked"
      @click="verify"
    >
      Verify
    </UButton>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  merchantId: string
  paymentOrderId: string
  challengeId: string
  expiresAt?: string | null
}>()

const emit = defineEmits<{ verified: [] }>()
const pin = ref<number[]>([])
const verifying = ref(false)
const locked = ref(false)
const errorMessage = ref<string | null>(null)
const now = ref(Date.now())

const pinValue = computed(() => pin.value.map(String).join(''))

const expiresMs = computed(() => {
  if (!props.expiresAt) {
    return null
  }
  return new Date(props.expiresAt).getTime()
})

const progress = computed(() => {
  if (expiresMs.value == null) {
    return null
  }
  const remaining = expiresMs.value - now.value
  return Math.max(0, Math.min(100, (remaining / 90_000) * 100))
})

const expiresLabel = computed(() => {
  if (expiresMs.value == null) {
    return ''
  }
  const remaining = Math.max(0, Math.floor((expiresMs.value - now.value) / 1000))
  const minutes = Math.floor(remaining / 60)
  const seconds = remaining % 60
  return `${minutes}:${String(seconds).padStart(2, '0')}`
})

let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})
onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
})

function onComplete() {
  void verify()
}

async function verify() {
  if (pinValue.value.length !== 6 || verifying.value || locked.value) {
    return
  }
  if (expiresMs.value != null && Date.now() >= expiresMs.value) {
    errorMessage.value = 'expired'
    return
  }
  verifying.value = true
  errorMessage.value = null
  try {
    await $fetch(
      `/api/merchants/${props.merchantId}/payment-orders/${props.paymentOrderId}/refund-challenges/${props.challengeId}/verify`,
      { method: 'POST', body: { pin: pinValue.value } },
    )
    emit('verified')
  } catch (error: unknown) {
    const err = error as { statusCode?: number, data?: { error?: string, detail?: string } }
    if (err.statusCode === 429 || err.data?.error === 'rate_limited') {
      locked.value = true
      errorMessage.value = 'Too many attempts'
    } else {
      errorMessage.value = err.data?.detail || err.data?.error || 'Invalid PIN'
      pin.value = []
    }
  } finally {
    verifying.value = false
  }
}
</script>
