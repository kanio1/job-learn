<template>
  <UCard>
    <template #header>
      <h3 class="text-base font-semibold">Create Payment Order</h3>
    </template>

    <div class="space-y-6">
      <UAlert
        v-if="!canCreatePaymentOrder"
        color="warning"
        variant="subtle"
        icon="i-lucide-shield-alert"
        description="You do not have permission to create payment orders."
        role="alert"
      />

      <IdempotencyKeyInput v-model="idempotencyKey" />

      <UStepper
        v-model="step"
        :items="[...stepItems]"
        class="w-full"
        data-testid="create-payment-order-stepper"
      />

      <UForm
        :schema="createPaymentOrderSchema"
        :state="formState"
        data-testid="create-payment-order-form"
        class="space-y-4"
        @submit="onSubmit"
      >
        <UFormField
          v-if="step === 'amount'"
          label="Amount (minor units)"
          name="amountMinor"
          :error="stepError ?? undefined"
        >
          <UInput v-model.number="formState.amountMinor" type="number" placeholder="12500" />
        </UFormField>

        <UFormField
          v-else-if="step === 'currency'"
          label="Currency"
          name="currency"
          :error="stepError ?? undefined"
        >
          <USelect v-model="formState.currency" :items="currencies" placeholder="Select currency" />
        </UFormField>

        <UFormField
          v-else-if="step === 'reference'"
          label="Client Order Reference"
          name="clientOrderReference"
          :error="stepError ?? undefined"
        >
          <UInput v-model="formState.clientOrderReference" placeholder="PAY-001" />
        </UFormField>

        <dl
          v-else
          data-testid="create-payment-order-review"
          class="space-y-2 text-sm"
        >
          <div class="flex justify-between gap-4">
            <dt class="text-muted">Amount (minor units)</dt>
            <dd>{{ formState.amountMinor }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-muted">Currency</dt>
            <dd>{{ formState.currency }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-muted">Client Order Reference</dt>
            <dd>{{ formState.clientOrderReference }}</dd>
          </div>
        </dl>

        <ErrorState
          v-if="errorProblem || errorMessage"
          :problem="errorProblem ?? undefined"
          :message="errorMessage ?? undefined"
        />

        <div class="flex items-center gap-2">
          <UButton
            v-if="step !== 'amount'"
            type="button"
            color="neutral"
            variant="ghost"
            label="Back"
            data-testid="create-payment-order-back"
            @click="goBack"
          />
          <UButton
            v-if="step !== 'review'"
            type="button"
            label="Next"
            data-testid="create-payment-order-next"
            :disabled="!canCreatePaymentOrder"
            @click="goNext"
          />
          <UButton
            v-else
            type="submit"
            :disabled="!canCreatePaymentOrder"
            label="Create Payment Order"
            data-testid="action-create-payment-order"
            :aria-label="canCreatePaymentOrder ? 'Create payment order' : 'Create payment order unavailable: missing payment create authority'"
          />
        </div>
      </UForm>
    </div>
  </UCard>
</template>

<script setup lang="ts">
/**
 * Create Payment Order Form — stepper: amount → currency → reference → review.
 *
 * Idempotency-Key reuse: on failure the key is retained; changing any field
 * after a failure generates a new key. Draft lives in sessionStorage.
 */
import { createPaymentOrderSchema } from '~/schemas/payment-order.schema'
import type { ProblemDetails } from '~/types/api'

const props = defineProps<{
  merchantId: string
}>()

const emit = defineEmits<{
  (e: 'created', paymentOrderId: string): void
  (e: 'debugRequest', info: { method: string; path: string; headers: Record<string, string> }): void
  (e: 'debugResponse', info: { status: number; headers: Record<string, string>; body: string }): void
}>()

const { createOrder } = usePaymentOrdersApi()
const { success: toastSuccess } = useAppToast()
const { can } = useAuthorization()

const errorMessage = ref<string | null>(null)
const errorProblem = ref<ProblemDetails | null>(null)
const stepError = ref<string | null>(null)

const failedSubmitSnapshot = ref<{ amountMinor: number | undefined; currency: string | undefined; clientOrderReference: string } | null>(null)

const currencies = ['PLN', 'EUR', 'USD']
const stepItems = [
  { value: 'amount', title: 'Amount', icon: 'i-lucide-coins' },
  { value: 'currency', title: 'Currency', icon: 'i-lucide-banknote' },
  { value: 'reference', title: 'Reference', icon: 'i-lucide-hash' },
  { value: 'review', title: 'Review', icon: 'i-lucide-check' },
] as const

type Step = (typeof stepItems)[number]['value']
const step = ref<Step>('amount')

const formState = reactive({
  amountMinor: undefined as number | undefined,
  currency: undefined as 'PLN' | 'EUR' | 'USD' | undefined,
  clientOrderReference: '',
})

const canCreatePaymentOrder = computed(() => can.value.canCreatePaymentOrder)
const draftKey = computed(() => `payment-order-draft:${props.merchantId}`)
const idempotencyKey = ref('')

function formMatchesFailedSnapshot(): boolean {
  if (!failedSubmitSnapshot.value) return false
  const snap = failedSubmitSnapshot.value
  return (
    snap.amountMinor === formState.amountMinor &&
    snap.currency === formState.currency &&
    snap.clientOrderReference === formState.clientOrderReference
  )
}

watch(
  () => [formState.amountMinor, formState.currency, formState.clientOrderReference],
  () => {
    persistDraft()
    if (failedSubmitSnapshot.value && !formMatchesFailedSnapshot()) {
      idempotencyKey.value = crypto.randomUUID()
      failedSubmitSnapshot.value = null
    }
  },
)

function persistDraft() {
  if (!import.meta.client) {
    return
  }
  sessionStorage.setItem(draftKey.value, JSON.stringify({
    amountMinor: formState.amountMinor,
    currency: formState.currency,
    clientOrderReference: formState.clientOrderReference,
    step: step.value,
  }))
}

function restoreDraft() {
  if (!import.meta.client) {
    return
  }
  const raw = sessionStorage.getItem(draftKey.value)
  if (!raw) {
    return
  }
  try {
    const parsed = JSON.parse(raw) as {
      amountMinor?: number
      currency?: 'PLN' | 'EUR' | 'USD'
      clientOrderReference?: string
      step?: Step
    }
    formState.amountMinor = parsed.amountMinor
    formState.currency = parsed.currency
    formState.clientOrderReference = parsed.clientOrderReference ?? ''
    if (parsed.step && stepItems.some(item => item.value === parsed.step)) {
      step.value = parsed.step
    }
  }
  catch {
    sessionStorage.removeItem(draftKey.value)
  }
}

function clearDraft() {
  if (import.meta.client) {
    sessionStorage.removeItem(draftKey.value)
  }
}

function goBack() {
  stepError.value = null
  if (step.value === 'currency') step.value = 'amount'
  else if (step.value === 'reference') step.value = 'currency'
  else if (step.value === 'review') step.value = 'reference'
  persistDraft()
}

function goNext() {
  stepError.value = null
  if (step.value === 'amount') {
    const amountParsed = createPaymentOrderSchema.pick({ amountMinor: true }).safeParse({
      amountMinor: formState.amountMinor,
    })
    if (!amountParsed.success) {
      stepError.value = formState.amountMinor == null || Number(formState.amountMinor) < 1
        ? 'Amount must be at least 1'
        : (amountParsed.error.issues[0]?.message ?? 'Amount must be at least 1')
      return
    }
    step.value = 'currency'
  }
  else if (step.value === 'currency') {
    const currencyParsed = createPaymentOrderSchema.pick({ currency: true }).safeParse({
      currency: formState.currency,
    })
    if (!currencyParsed.success) {
      stepError.value = currencyParsed.error.issues[0]?.message ?? 'Currency is required'
      return
    }
    step.value = 'reference'
  }
  else if (step.value === 'reference') {
    const referenceParsed = createPaymentOrderSchema.pick({ clientOrderReference: true }).safeParse({
      clientOrderReference: formState.clientOrderReference,
    })
    if (!referenceParsed.success) {
      stepError.value = referenceParsed.error.issues[0]?.message ?? 'Client order reference is required'
      return
    }
    step.value = 'review'
  }
  persistDraft()
}

onMounted(() => {
  restoreDraft()
})

async function onSubmit() {
  if (!canCreatePaymentOrder.value) return
  if (step.value !== 'review') {
    goNext()
    return
  }

  errorMessage.value = null
  errorProblem.value = null

  const path = `/api/merchants/${props.merchantId}/payment-orders`
  const requestHeaders: Record<string, string> = {
    'Idempotency-Key': idempotencyKey.value,
    'Content-Type': 'application/json',
  }

  emit('debugRequest', {
    method: 'POST',
    path,
    headers: requestHeaders,
  })

  try {
    const response = await createOrder(
      props.merchantId,
      {
        amountMinor: formState.amountMinor!,
        currency: formState.currency!,
        clientOrderReference: formState.clientOrderReference,
      },
      idempotencyKey.value
    )

    emit('debugResponse', {
      status: response.status,
      headers: {
        ...(response.headers.etag ? { 'ETag': response.headers.etag } : {}),
        ...(response.headers.location ? { 'Location': response.headers.location } : {}),
        ...(response.headers.vary ? { 'Vary': response.headers.vary } : {}),
        ...(response.headers.cacheControl ? { 'Cache-Control': response.headers.cacheControl } : {}),
        ...(response.headers.correlationId ? { 'X-Correlation-ID': response.headers.correlationId } : {}),
      },
      body: response.raw,
    })

    if (response.problem || !response.data) {
      errorProblem.value = response.problem
      errorMessage.value = response.problem
        ? null
        : 'Failed to create payment order. Please try again.'

      failedSubmitSnapshot.value = {
        amountMinor: formState.amountMinor,
        currency: formState.currency,
        clientOrderReference: formState.clientOrderReference,
      }
      return
    }

    formState.amountMinor = undefined
    formState.currency = undefined
    formState.clientOrderReference = ''
    step.value = 'amount'
    idempotencyKey.value = crypto.randomUUID()
    failedSubmitSnapshot.value = null
    clearDraft()

    toastSuccess(`Payment order ${response.data.paymentOrderId} created successfully`)

    emit('created', response.data.paymentOrderId)
  } catch {
    errorMessage.value = 'An unexpected error occurred. Please try again.'

    failedSubmitSnapshot.value = {
      amountMinor: formState.amountMinor,
      currency: formState.currency,
      clientOrderReference: formState.clientOrderReference,
    }
  }
}
</script>
