<template>
  <UDashboardPanel id="visual-lab">
    <template #header>
      <UDashboardNavbar title="Visual Lab">
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
          title="Stable tiles"
          description="No UUIDs or live clocks in tile text. prefers-reduced-motion is respected. Mask CSS hides [data-dynamic]. Goldens are mocked Chromium only."
        />
        <div class="flex gap-2">
          <UButton data-testid="visual-lab-break" variant="outline" @click="broken = !broken">
            Toggle break
          </UButton>
        </div>
        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3" :class="broken ? 'visual-lab-broken' : ''">
          <div data-testid="visual-tile-merchant-badge" class="rounded-lg border p-4">
            <p class="text-sm mb-2">Merchant</p>
            <BusinessStatusBadge status="ACTIVE" type="merchant" />
            <span data-dynamic class="sr-only">dynamic</span>
          </div>
          <div data-testid="visual-tile-payment-badge" class="rounded-lg border p-4">
            <p class="text-sm mb-2">Payment</p>
            <BusinessStatusBadge status="CAPTURED" type="payment" />
          </div>
          <div data-testid="visual-tile-problem-details" class="rounded-lg border p-4">
            <ProblemDetailsCard :problem="sampleProblem" />
          </div>
          <div data-testid="visual-tile-hosted-cta" class="rounded-lg border p-4">
            <p class="text-sm mb-2">Hosted CTA</p>
            <UButton>Approve</UButton>
          </div>
          <div data-testid="visual-tile-idle-lock" class="rounded-lg border p-4">
            <p class="text-sm font-semibold">Session locked</p>
            <p class="text-sm text-muted">Unlock copy (static tile)</p>
          </div>
          <div data-testid="visual-tile-dark" class="rounded-lg border p-4 bg-neutral-900 text-neutral-50">
            <p class="text-sm mb-2">Dark tile</p>
            <BusinessStatusBadge status="SUSPENDED" type="merchant" />
          </div>
          <div data-testid="visual-tile-expired" class="rounded-lg border p-4">
            <UAlert color="error" title="Payment link expired" description="Static expiry copy, not a live timer." />
          </div>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>

<style scoped>
@media (prefers-reduced-motion: reduce) {
  * {
    animation: none !important;
    transition: none !important;
  }
}
.visual-lab-broken [data-testid='visual-tile-hosted-cta'] {
  padding-top: 48px;
}
</style>

<script setup lang="ts">
import type { ProblemDetails } from '~/types/api'

definePageMeta({ layout: 'dashboard' })

const broken = ref(false)
const sampleProblem: ProblemDetails = {
  type: 'https://api.payment-quality.local/problems/validation',
  title: 'Bad Request',
  status: 400,
  detail: 'Amount is invalid',
  error: 'validation',
}
</script>
