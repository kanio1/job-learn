<template>
  <UCard data-testid="refund-approvals">
    <template #header>
      <h3 class="text-base font-semibold">Dual-control refund</h3>
    </template>
    <div class="space-y-3">
      <div class="flex flex-wrap gap-2">
        <UButton
          v-if="canRequest"
          data-testid="refund-approval-create"
          @click="createRequest"
        >
          Request refund
        </UButton>
      </div>
      <p v-if="!etag" class="text-sm text-amber-700">
        Reload the payment order to obtain an ETag before a checker can approve.
      </p>
      <p v-if="errorMessage" class="text-sm text-red-600">{{ errorMessage }}</p>
      <ul class="space-y-2 text-sm">
        <li
          v-for="item in approvals"
          :key="item.approvalId"
          class="flex items-center justify-between gap-2 rounded border p-2"
        >
          <span>{{ item.status }} · {{ item.makerSubject }}</span>
          <UButton
            v-if="item.status === 'PENDING'"
            size="xs"
            data-testid="refund-approval-approve"
            :disabled="!canApprove"
            @click="approve(item.approvalId)"
          >
            Approve
          </UButton>
        </li>
      </ul>
      <PinChallengeComponent
        v-if="pinChallenge"
        :merchant-id="merchantId"
        :payment-order-id="paymentOrderId"
        :challenge-id="pinChallenge.challengeId"
        :expires-at="pinChallenge.expiresAt"
        @verified="onPinVerified"
      />
    </div>
  </UCard>
</template>

<script setup lang="ts">
const props = defineProps<{
  merchantId: string
  paymentOrderId: string
  etag?: string | null
  orderStatus?: string | null
  amountMinor?: number | null
}>()

const { can } = useAuthorization()
const toast = useAppToast()
const canRequest = computed(() => can.value.canRunLifecycle && props.orderStatus === 'CAPTURED')
const canApprove = computed(() => can.value.canRunLifecycle && Boolean(props.etag))
const approvals = ref<Array<{
  approvalId: string
  status: string
  makerSubject: string
}>>([])
const errorMessage = ref<string | null>(null)
const pinChallenge = ref<{ challengeId: string, expiresAt?: string, approvalId: string } | null>(null)
const pinVerified = ref(false)

const emit = defineEmits<{ approved: [] }>()
const needsPin = computed(() => (props.amountMinor ?? 0) > 100_000)

async function load() {
  try {
    const response = await $fetch<{ content: typeof approvals.value }>(
      `/api/merchants/${props.merchantId}/payment-orders/${props.paymentOrderId}/refund-approvals`,
    )
    approvals.value = response.content ?? []
  } catch {
    approvals.value = []
  }
}

async function createRequest() {
  errorMessage.value = null
  try {
    await $fetch(
      `/api/merchants/${props.merchantId}/payment-orders/${props.paymentOrderId}/refund-approvals`,
      { method: 'POST', body: props.amountMinor != null ? { amountMinor: props.amountMinor } : {} },
    )
    toast.success('Refund requested', 'A checker must approve before money moves.')
    await load()
  } catch (error: any) {
    errorMessage.value = error?.data?.detail || error?.data?.error || 'Request failed'
  }
}

async function onPinVerified() {
  pinVerified.value = true
  const approvalId = pinChallenge.value?.approvalId
  if (approvalId) {
    await approve(approvalId)
  }
}

async function approve(approvalId: string) {
  errorMessage.value = null
  if (!props.etag) {
    errorMessage.value = 'Reload the payment order to obtain an ETag before approving.'
    return
  }
  if (needsPin.value && !pinVerified.value) {
    try {
      const created = await $fetch<{ challengeId: string, expiresAt?: string }>(
        `/api/merchants/${props.merchantId}/payment-orders/${props.paymentOrderId}/refund-challenges`,
        { method: 'POST', body: {} },
      )
      pinChallenge.value = { ...created, approvalId }
    } catch (error: any) {
      errorMessage.value = error?.data?.detail || error?.data?.error || 'PIN challenge failed'
    }
    return
  }
  try {
    await $fetch(
      `/api/merchants/${props.merchantId}/payment-orders/${props.paymentOrderId}/refund-approvals/${approvalId}/approve`,
      {
        method: 'POST',
        headers: {
          'If-Match': props.etag,
          'Idempotency-Key': crypto.randomUUID(),
        },
      },
    )
    toast.success('Refund approved', 'The payment order is now refunded.')
    await load()
    emit('approved')
  } catch (error: any) {
    errorMessage.value = error?.data?.detail || error?.data?.error || 'Approve failed'
  }
}

onMounted(load)
</script>
