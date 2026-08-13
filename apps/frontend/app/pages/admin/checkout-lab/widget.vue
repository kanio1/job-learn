<template>
  <UDashboardPanel id="checkout-lab-widget">
    <template #header>
      <UDashboardNavbar title="Hosted widget (iframe)">
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
          title="Same-origin iframe"
          description="New-tab hosted checkout remains the default. This iframe is same-origin for frameLocator learning only."
        />
        <UFormField label="Session id">
          <div class="flex gap-2">
            <UInput v-model="sessionId" data-testid="widget-session-id" class="flex-1" />
            <UButton data-testid="widget-load" @click="src = sessionId ? `/psp/checkout/${sessionId}` : ''">Load</UButton>
          </div>
        </UFormField>
        <iframe
          v-if="src"
          data-testid="checkout-lab-widget-frame"
          class="h-[640px] w-full rounded border"
          :src="src"
        />
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'dashboard' })

const sessionId = ref('')
const src = ref('')
</script>
