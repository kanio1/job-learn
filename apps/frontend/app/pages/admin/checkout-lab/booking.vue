<template>
  <UDashboardPanel id="checkout-lab-booking">
    <template #header>
      <UDashboardNavbar title="Booking Lab">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>
    <template #body>
      <div class="space-y-4 p-1">
        <UAlert
          color="info"
          variant="subtle"
          title="What to observe"
          description="POST /bookings returns 200 JSON with redirectUri for the dashboard form. Merchant hop POST /sessions returns 302 Location (see ApiDebugPanel on direct session create). Cash confirms fulfillment with no checkout_session row."
        />

        <UForm :schema="formSchema" :state="form" class="space-y-4 max-w-xl" data-testid="checkout-booking-form" @submit="submit">
          <UFormField label="External order id" name="extOrderId">
            <UInput v-model="form.extOrderId" data-testid="checkout-booking-ext-order" />
          </UFormField>
          <UFormField label="Amount (minor units)" name="amountMinor">
            <UInput v-model.number="form.amountMinor" type="number" data-testid="checkout-booking-amount" />
          </UFormField>
          <UFormField label="Currency" name="currency">
            <USelect v-model="form.currency" :items="['PLN', 'EUR', 'USD']" data-testid="checkout-booking-currency" />
          </UFormField>
          <UFormField label="Mode" name="mode">
            <USelect v-model="form.mode" :items="['ONLINE', 'CASH']" data-testid="checkout-booking-mode" />
          </UFormField>
          <UButton type="submit" data-testid="checkout-booking-submit" :loading="loading">
            Create booking
          </UButton>
        </UForm>

        <ApiDebugPanel :request="debugRequest" :response="debugResponse" />
        <ProblemDetailsCard v-if="problem" :problem="problem" />

        <div v-if="booking" class="space-y-2">
          <p class="text-sm">
            Fulfillment:
            <span data-testid="fulfillment-status">{{ booking.fulfillmentStatus }}</span>
          </p>
          <p v-if="booking.validityUntil" class="text-sm">
            Link validity:
            <ExpirationCountdown :expires-at="booking.validityUntil" />
          </p>
          <UButton
            v-if="booking.redirectUri"
            :href="booking.redirectUri"
            target="_blank"
            data-testid="checkout-open-hosted"
            icon="i-lucide-external-link"
          >
            Open hosted checkout
          </UButton>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
import { z } from 'zod'
import type { ProblemDetails } from '~/types/api'
import type { BookingResult } from '~/schemas/checkout-lab.schema'

definePageMeta({ layout: 'dashboard' })

const formSchema = z.object({
  extOrderId: z.string().min(3),
  amountMinor: z.number().min(1).max(100_000_000),
  currency: z.enum(['PLN', 'EUR', 'USD']),
  mode: z.enum(['ONLINE', 'CASH']),
})

const form = reactive({
  extOrderId: `BOOK-${Date.now()}`,
  amountMinor: 1999,
  currency: 'PLN' as 'PLN' | 'EUR' | 'USD',
  mode: 'ONLINE' as 'ONLINE' | 'CASH',
})

const loading = ref(false)
const booking = ref<BookingResult | null>(null)
const problem = ref<ProblemDetails | null>(null)
const debugRequest = ref<{ method: string, path: string, headers: Record<string, string> } | null>(null)
const debugResponse = ref<{ status: number, headers: Record<string, string>, body: string } | null>(null)
const { createBooking } = useCheckoutLabApi()
const toast = useAppToast()

async function submit() {
  loading.value = true
  problem.value = null
  debugRequest.value = {
    method: 'POST',
    path: '/api/checkout-lab/bookings',
    headers: { Authorization: 'Bearer ••••••••', 'Content-Type': 'application/json' },
  }
  const response = await createBooking({
    mode: form.mode,
    extOrderId: form.extOrderId,
    amountMinor: form.amountMinor,
    currency: form.currency,
    continueUrl: `${window.location.origin}/checkout-lab/return?status=success`,
    notifyUrl: `${useRuntimeConfig().public.apiBaseUrl}/api/checkout-lab/notify`,
    validitySeconds: 900,
  })
  debugResponse.value = {
    status: response.status,
    headers: {
      'X-Correlation-ID': response.headers.correlationId || '',
    },
    body: response.raw,
  }
  loading.value = false
  if (response.problem) {
    problem.value = response.problem
    toast.error('Booking failed', response.problem.detail)
    return
  }
  booking.value = response.data
  toast.success(form.mode === 'CASH' ? 'Cash booking confirmed' : 'Online session created')
}
</script>
