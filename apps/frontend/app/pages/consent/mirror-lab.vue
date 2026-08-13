<template>
  <div class="mx-auto max-w-lg p-6 space-y-4" data-testid="consent-mirror-lab">
    <h1 class="text-xl font-semibold">AIS-lite consent (lab)</h1>
    <p class="text-sm text-muted">Not Berlin Group production. Grant issues a token for TPP-lab GET. Revoke yields 403.</p>
    <UButton data-testid="consent-grant" @click="grant">Grant</UButton>
    <p data-testid="consent-token" class="font-mono text-xs break-all">{{ token }}</p>
    <UButton data-testid="consent-tpp" variant="outline" @click="readTpp">TPP list accounts</UButton>
    <UButton data-testid="consent-revoke" color="error" variant="outline" @click="revoke">Revoke</UButton>
    <pre data-testid="consent-tpp-result" class="text-xs whitespace-pre-wrap">{{ tppResult }}</pre>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'dashboard' })

const token = ref('')
const consentId = ref('')
const tppResult = ref('')

async function grant() {
  const body = await $fetch<{ consentId: string, accessToken: string }>('/api/mirror-lab/consents', { method: 'POST' })
  consentId.value = body.consentId
  token.value = body.accessToken
}

async function readTpp() {
  try {
    const response = await $fetch.raw('/api/mirror-lab/tpp/accounts', {
      headers: { 'X-Lab-Consent-Token': token.value },
    })
    tppResult.value = JSON.stringify({ status: response.status, body: response._data })
  }
  catch (error: any) {
    tppResult.value = JSON.stringify({ status: error.statusCode, body: error.data })
  }
}

async function revoke() {
  await $fetch(`/api/mirror-lab/consents/${consentId.value}/revoke`, { method: 'POST' })
}
</script>
