<template>
  <UDashboardPanel id="network-lab">
    <template #header>
      <UDashboardNavbar title="Network Lab">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>
    <template #body>
      <div class="space-y-4 p-1 max-w-3xl">
        <UAlert
          color="warning"
          variant="subtle"
          title="JSON success is not an oracle"
          description="Lie fulfillment always returns status=success. Assert persistence or waitForResponse. POM must not use route.fulfill."
        />
        <div class="flex flex-wrap gap-2">
          <UButton data-testid="network-lab-trigger-503" @click="trigger503">Retry 503</UButton>
          <UButton data-testid="network-lab-lie" variant="outline" @click="triggerLie">Lie body</UButton>
          <UButton data-testid="network-lab-slow" variant="outline" @click="triggerSlow">Slow (abort demo)</UButton>
          <UButton data-testid="network-lab-cors" variant="outline" @click="triggerCors">CORS cookie demo</UButton>
          <UButton data-testid="network-lab-har" variant="outline" @click="triggerHar">HAR replay path</UButton>
        </div>
        <ErrorState v-if="failure" :message="failure" />
        <pre v-else data-testid="network-lab-result" class="text-xs whitespace-pre-wrap">{{ result }}</pre>
        <p class="text-sm text-muted">
          Hosted checkout must not send credentialed cross-origin cookies. This demo allows credentials only for http://localhost:3000.
        </p>
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
import ErrorState from '~/components/shared/ErrorState.vue'

definePageMeta({ layout: 'dashboard' })

const result = ref('')
const failure = ref('')

async function capture(path: string, method: 'GET' | 'POST' = 'GET') {
  failure.value = ''
  try {
    const response = await $fetch.raw(path, { method })
    result.value = JSON.stringify({ status: response.status, retryAfter: response.headers.get('retry-after'), body: response._data })
  }
  catch (error: any) {
    if (!error?.statusCode) {
      failure.value = error?.message || 'Network request failed'
      result.value = ''
      return
    }
    result.value = JSON.stringify({
      status: error.statusCode,
      retryAfter: error.response?.headers?.get?.('retry-after'),
      body: error.data,
    })
  }
}

function trigger503() {
  return capture('/api/network-lab/trigger-503-retry', 'POST')
}
function triggerLie() {
  return capture('/api/network-lab/lie-fulfillment')
}
function triggerSlow() {
  return capture('/api/network-lab/slow')
}
function triggerCors() {
  return capture('/api/network-lab/cors-cookie')
}
function triggerHar() {
  return capture('/api/network-lab/har-replay')
}
</script>
