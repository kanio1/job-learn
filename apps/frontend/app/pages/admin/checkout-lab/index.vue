<template>
  <UDashboardPanel id="checkout-lab-hub">
    <template #header>
      <UDashboardNavbar title="Checkout Lab">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>
    <template #body>
      <div class="space-y-4 p-1">
        <UAlert
          icon="i-lucide-graduation-cap"
          color="info"
          variant="subtle"
          title="Three identity worlds"
          description="Keycloak JWT authenticates this dashboard. Lab OAuth Bearer creates merchant sessions. HMAC Lab-Signature authenticates notify. Hosted checkout is public — like a real PSP page."
        />

        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <UCard>
            <h2 class="font-semibold mb-2">Keycloak JWT</h2>
            <p class="text-sm text-muted">Dashboard session via nuxt-auth-utils. Token never reaches the browser. Used for this hub, Booking, and Inspector.</p>
          </UCard>
          <UCard>
            <h2 class="font-semibold mb-2">Lab OAuth Bearer</h2>
            <p class="text-sm text-muted">client_credentials form post. BFF swaps it in server-side. Masked as Bearer •••••••• in debug panels.</p>
          </UCard>
          <UCard>
            <h2 class="font-semibold mb-2">HMAC notify</h2>
            <p class="text-sm text-muted">Lab-Signature over raw body bytes. 400 = do not retry. 503 = retry. 202 = queued, not fulfilled.</p>
          </UCard>
          <UCard>
            <h2 class="font-semibold mb-2">Hosted capability</h2>
            <p class="text-sm text-muted">GET hosted issues short-lived Lab-Simulate-Token (HMAC). POST simulate requires it — public PSP page without Keycloak, not open UUID abuse.</p>
          </UCard>
        </div>

        <div class="flex flex-wrap gap-2">
          <UButton to="/admin/checkout-lab/booking" icon="i-lucide-calendar" data-testid="checkout-lab-open-booking">
            Booking
          </UButton>
          <UButton to="/admin/checkout-lab/inspector" variant="outline" icon="i-lucide-search" data-testid="checkout-lab-open-inspector">
            Inspector
          </UButton>
        </div>

        <UAlert
          color="warning"
          variant="subtle"
          title="continueUrl is not proof of payment"
          description="Return URL query status=success is a UX hint. Oracle is fulfillment CONFIRMED after a verified notify event."
        />
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'dashboard' })
</script>
