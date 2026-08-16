<template>
  <UDashboardPanel id="session-lab">
    <template #header>
      <UDashboardNavbar title="Session Lab">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>
    <template #body>
      <div class="space-y-4 p-1 max-w-3xl">
        <UAlert
          color="info"
          variant="subtle"
          title="HttpOnly is invisible to JavaScript"
          description="document.cookie cannot list nuxt-session. The BFF cookie-policy contract and Playwright context.cookies() are the oracles. storageState serializes cookies for the next context; sessionStorage does not."
        />

        <UCard>
          <h2 class="font-semibold mb-2">JS-visible cookies</h2>
          <pre data-testid="session-lab-js-cookies" class="text-xs whitespace-pre-wrap">{{ jsCookies || '(empty — expected when session cookie is HttpOnly)' }}</pre>
        </UCard>

        <UCard>
          <h2 class="font-semibold mb-2">BFF cookie policy</h2>
          <pre data-testid="session-lab-cookie-policy" class="text-xs whitespace-pre-wrap">{{ policyText }}</pre>
        </UCard>

        <div class="flex flex-wrap gap-2">
          <UButton data-testid="session-lab-open-hosted" href="/psp/checkout/00000000-0000-4000-8000-000000000001" target="_blank">
            Open hosted checkout (new tab)
          </UButton>
          <UButton data-testid="session-lab-end-oidc" color="error" variant="outline" @click="endOidc">
            End OIDC session
          </UButton>
        </div>

        <UCard>
          <h2 class="font-semibold mb-2">Devices (same BFF session)</h2>
          <p class="text-sm text-muted mb-2">Two browser contexts with the same storageState share nuxt-session. Each page registers a device id in sessionStorage.</p>
          <ul data-testid="session-lab-device-list" class="text-sm space-y-1">
            <li v-for="device in devices" :key="device.id" class="flex items-center gap-2">
              <span>{{ device.label }}</span>
              <UButton size="xs" variant="outline" :data-testid="`session-lab-revoke-${device.id}`" @click="revoke(device.id)">
                Revoke
              </UButton>
            </li>
          </ul>
        </UCard>

        <UCard>
          <h2 class="font-semibold mb-2">CSRF demo (only this path)</h2>
          <p class="text-sm text-muted mb-2">GET token + cookie. POST without X-CSRF-Token returns 403 csrf_failed. Merchant payment BFF is unchanged.</p>
          <div class="flex gap-2">
            <UButton data-testid="session-lab-csrf-ok" @click="csrfDemo(true)">POST with token</UButton>
            <UButton data-testid="session-lab-csrf-fail" color="error" variant="outline" @click="csrfDemo(false)">POST without token</UButton>
          </div>
          <pre data-testid="session-lab-csrf-result" class="text-xs mt-2 whitespace-pre-wrap">{{ csrfResult }}</pre>
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'dashboard' })

const jsCookies = ref('')
const policyText = ref('')
const csrfResult = ref('')
const devices = ref<{ id: string, label: string }[]>([])
const deviceId = ref('')

async function loadPolicy() {
  jsCookies.value = document.cookie
  const policy = await $fetch('/api/session-lab/cookie-policy')
  policyText.value = JSON.stringify(policy, null, 2)
}

async function loadDevices() {
  if (!deviceId.value) {
    deviceId.value = sessionStorage.getItem('mrl-device-id') || crypto.randomUUID()
    sessionStorage.setItem('mrl-device-id', deviceId.value)
  }
  await $fetch('/api/session-lab/devices', {
    method: 'POST',
    body: { id: deviceId.value, label: navigator.userAgent.slice(0, 80) },
  })
  devices.value = await $fetch('/api/session-lab/devices')
}

async function revoke(id: string) {
  await $fetch(`/api/session-lab/devices/${id}/revoke`, { method: 'POST' })
  await loadDevices()
}

async function endOidc() {
  const result = await $fetch<{ ended: boolean, endSessionUrl: string }>('/api/session-lab/end-session', {
    method: 'POST',
  })
  window.location.href = result.endSessionUrl
}

async function csrfDemo(withToken: boolean) {
  const tokenResponse = await $fetch<{ token: string }>('/api/session-lab/csrf')
  try {
    const result = await $fetch.raw('/api/session-lab/csrf-demo', {
      method: 'POST',
      headers: withToken ? { 'X-CSRF-Token': tokenResponse.token } : {},
    })
    csrfResult.value = JSON.stringify({ status: result.status, body: result._data })
  }
  catch (error: any) {
    csrfResult.value = JSON.stringify({ status: error.statusCode, body: error.data })
  }
}

onMounted(async () => {
  await loadPolicy()
  await loadDevices()
})
</script>
