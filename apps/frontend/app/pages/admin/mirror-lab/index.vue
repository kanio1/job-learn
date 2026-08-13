<template>
  <UDashboardPanel id="mirror-lab-hub">
    <template #header>
      <UDashboardNavbar title="Mirror Lab">
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
          description="Dashboard Keycloak JWT never reaches the browser. Hosted checkout is a public PSP-like page (no nuxt-session). Lab OAuth Bearer is swapped only in the BFF. These labs are educational mirrors — not PayU and not a bank."
        />

        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <UCard data-testid="mirror-lab-card-session">
            <h2 class="font-semibold mb-2">Session Lab</h2>
            <p class="text-sm text-muted mb-3">HttpOnly cookies vs document.cookie, idle lock, storageState vs sessionStorage.</p>
            <UButton to="/admin/session-lab" data-testid="mirror-lab-open-session">Open Session Lab</UButton>
          </UCard>
          <UCard data-testid="mirror-lab-card-visual">
            <h2 class="font-semibold mb-2">Visual Lab</h2>
            <p class="text-sm text-muted mb-3">Stable tiles for toHaveScreenshot. Mask [data-dynamic]. No live clocks in tiles.</p>
            <UButton to="/admin/visual-lab" data-testid="mirror-lab-open-visual">Open Visual Lab</UButton>
          </UCard>
          <UCard data-testid="mirror-lab-card-network">
            <h2 class="font-semibold mb-2">Network Lab</h2>
            <p class="text-sm text-muted mb-3">503 then 200, lie JSON, abort, HAR replay. POM uses waitForResponse, never fulfill.</p>
            <UButton to="/admin/network-lab" data-testid="mirror-lab-open-network">Open Network Lab</UButton>
          </UCard>
          <UCard data-testid="mirror-lab-card-payu" :class="{ 'opacity-60': !checkoutLabEnabled }">
            <h2 class="font-semibold mb-2">PayU protocol mirrors</h2>
            <p class="text-sm text-muted mb-3">GET-with-body 403, lang on Location, refund notify, expiry, iframe widget, trusted_merchant.</p>
            <UButton
              v-if="checkoutLabEnabled"
              to="/admin/checkout-lab"
              data-testid="mirror-lab-open-payu"
            >
              Open Checkout Lab
            </UButton>
            <p v-else class="text-sm text-muted">Requires Checkout Lab flag.</p>
          </UCard>
          <UCard data-testid="mirror-lab-card-bank">
            <h2 class="font-semibold mb-2">Bank-like lab</h2>
            <p class="text-sm text-muted mb-3">Step-up, statements, disputes multipart, maker-checker, AIS-lite consent. JWT dashboard, not lab Bearer.</p>
            <UButton to="/admin/mirror-lab/bank" data-testid="mirror-lab-open-bank">Open bank lab</UButton>
          </UCard>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'dashboard' })

const checkoutLabEnabled = computed(() => useRuntimeConfig().public.checkoutLabEnabled === true)
</script>
