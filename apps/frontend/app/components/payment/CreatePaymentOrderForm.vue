<template>
  <UCard>
    <template #header>
      <h3 class="text-base font-semibold">Create Payment Order</h3>
    </template>

    <UForm :schema="createPaymentOrderSchema" :state="formState" @submit="onSubmit" class="space-y-4">
      <UFormField label="Amount (minor units)" name="amountMinor">
        <UInput v-model.number="formState.amountMinor" type="number" placeholder="12500" />
      </UFormField>

      <UFormField label="Currency" name="currency">
        <USelect v-model="formState.currency" :items="currencies" placeholder="Select currency" />
      </UFormField>

      <UFormField label="Client Order Reference" name="clientOrderReference">
        <UInput v-model="formState.clientOrderReference" placeholder="PAY-001" />
      </UFormField>

      <UAlert v-if="store.error" color="error" variant="subtle" :title="store.error" />

      <div v-if="successMessage" class="text-sm text-green-600">
        {{ successMessage }}
      </div>

      <UButton type="submit" :loading="store.loading" label="Create Payment Order" />
    </UForm>
  </UCard>
</template>

<script setup lang="ts">
import { createPaymentOrderSchema } from '~/schemas/payment-order.schema'

const props = defineProps<{
  merchantId: string
}>()

const emit = defineEmits<{
  (e: 'created', paymentOrderId: string): void
}>()

const store = usePaymentOrdersStore()
const successMessage = ref<string | null>(null)
const idempotencyKey = ref(generateIdempotencyKey())

const currencies = ['PLN', 'EUR', 'USD']

const formState = reactive({
  amountMinor: undefined as number | undefined,
  currency: undefined as 'PLN' | 'EUR' | 'USD' | undefined,
  clientOrderReference: '',
})

watch(
  () => [formState.amountMinor, formState.currency, formState.clientOrderReference],
  () => {
    if (!store.loading) {
      idempotencyKey.value = generateIdempotencyKey()
    }
  }
)

function generateIdempotencyKey() {
  return `idem-${crypto.randomUUID()}`
}

async function onSubmit() {
  store.clearError()
  successMessage.value = null

  try {
    const result = await store.createOrder(
      props.merchantId,
      {
        amountMinor: formState.amountMinor!,
        currency: formState.currency!,
        clientOrderReference: formState.clientOrderReference,
      },
      idempotencyKey.value
    )
    successMessage.value = `Payment order ${result.paymentOrderId} created successfully`

    formState.amountMinor = undefined
    formState.currency = undefined
    formState.clientOrderReference = ''

    idempotencyKey.value = generateIdempotencyKey()
    emit('created', result.paymentOrderId)
  } catch {
    // Error is handled by the store
  }
}
</script>
