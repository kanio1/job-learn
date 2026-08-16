<template>
  <UDashboardPanel id="mirror-lab-bank">
    <template #header>
      <UDashboardNavbar title="Bank-like lab">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>
    <template #body>
      <div class="space-y-6 p-1 max-w-3xl">
        <UAlert
          color="info"
          variant="subtle"
          title="Not a bank"
          description="Step-up is a lab header, not Keycloak ACR. Maker-checker does not bind payment_orders refund."
        />

        <UCard>
          <h2 class="font-semibold mb-2">High-value refund (step-up)</h2>
          <div class="flex gap-2 mb-2">
            <UInput v-model.number="amountMinor" type="number" data-testid="step-up-amount" />
            <UInput v-model="merchantId" data-testid="step-up-merchant-id" />
            <UButton data-testid="step-up-submit" @click="submitRefund(false)">Submit</UButton>
          </div>
          <div v-if="stepUpOpen" data-testid="step-up-challenge" class="rounded border p-3 space-y-2">
            <p>Confirm amount {{ amountMinor }} for merchant {{ merchantId }}</p>
            <UButton data-testid="step-up-confirm" @click="submitRefund(true)">Confirm step-up</UButton>
          </div>
          <pre data-testid="step-up-result" class="text-xs mt-2">{{ stepUpResult }}</pre>
        </UCard>

        <UCard>
          <h2 class="font-semibold mb-2">Statements</h2>
          <div class="flex gap-2">
            <UButton data-testid="statement-download-csv" @click="downloadStatement('csv')">CSV</UButton>
            <UButton data-testid="statement-download-pdf" variant="outline" @click="downloadStatement('pdf')">PDF</UButton>
          </div>
        </UCard>

        <UCard>
          <h2 class="font-semibold mb-2">Disputes</h2>
          <UButton data-testid="dispute-open" @click="openDispute">Open dispute</UButton>
          <p data-testid="dispute-id" class="text-sm mt-2">{{ disputeId }}</p>
          <input data-testid="dispute-evidence-file" type="file" class="mt-2" @change="onEvidence">
        </UCard>

        <UCard>
          <h2 class="font-semibold mb-2">Maker-checker</h2>
          <p class="text-sm text-muted mb-2">Lab refund approvals (not payment_orders). Create as maker, approve as a second role.</p>
          <div class="flex flex-wrap gap-2">
            <UButton data-testid="approval-create" @click="createApproval">Create as maker</UButton>
            <UInput v-model="approvalId" class="mt-2" data-testid="approval-id" />
            <UButton data-testid="approval-approve" variant="outline" @click="confirmApproveOpen = true">
              Approve as checker
            </UButton>
          </div>
          <pre data-testid="approval-result" class="text-xs mt-2">{{ approvalResult }}</pre>
        </UCard>

        <ConfirmActionModal
          :open="confirmApproveOpen"
          title="Approve refund"
          description="Checker confirms this lab approval. Maker self-approve is rejected."
          confirm-label="Approve"
          @confirm="approve"
          @update:open="confirmApproveOpen = $event"
        />

        <UButton to="/consent/mirror-lab" data-testid="mirror-lab-open-consent">AIS-lite consent</UButton>
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'dashboard' })

const amountMinor = ref(10000)
const merchantId = ref('00000000-0000-0000-0000-0000000000b1')
const stepUpOpen = ref(false)
const stepUpResult = ref('')
const disputeId = ref('')
const approvalId = ref('')
const approvalResult = ref('')
const confirmApproveOpen = ref(false)

async function submitRefund(stepUp: boolean) {
  try {
    const response = await $fetch.raw('/api/mirror-lab/high-value-refunds', {
      method: 'POST',
      body: { amountMinor: amountMinor.value, merchantId: merchantId.value },
      headers: stepUp ? { 'X-Lab-Step-Up': 'confirmed' } : {},
    })
    stepUpOpen.value = false
    stepUpResult.value = JSON.stringify({ status: response.status, body: response._data })
  }
  catch (error: any) {
    if (error.statusCode === 403 && error.data?.error === 'step_up_required') {
      stepUpOpen.value = true
    }
    stepUpResult.value = JSON.stringify({ status: error.statusCode, body: error.data })
  }
}

async function downloadStatement(format: 'csv' | 'pdf') {
  const response = await fetch(`/api/mirror-lab/statements?format=${format}`)
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = format === 'csv' ? 'statement.csv' : 'statement.pdf'
  anchor.click()
  URL.revokeObjectURL(url)
}

async function openDispute() {
  const body = await $fetch<{ disputeId: string }>('/api/mirror-lab/disputes', {
    method: 'POST',
    body: { merchantId: merchantId.value },
  })
  disputeId.value = body.disputeId
}

async function onEvidence(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !disputeId.value) {
    return
  }
  const form = new FormData()
  form.append('file', file)
  await $fetch(`/api/mirror-lab/disputes/${disputeId.value}/evidence`, { method: 'POST', body: form })
}

async function createApproval() {
  const body = await $fetch<{ approvalId: string }>('/api/mirror-lab/refund-approvals', {
    method: 'POST',
    body: { merchantId: merchantId.value, amountMinor: amountMinor.value },
  })
  approvalId.value = body.approvalId
}

async function approve() {
  confirmApproveOpen.value = false
  try {
    const response = await $fetch.raw(`/api/mirror-lab/refund-approvals/${approvalId.value}/approve`, { method: 'POST' })
    approvalResult.value = JSON.stringify({ status: response.status, body: response._data })
  }
  catch (error: any) {
    approvalResult.value = JSON.stringify({ status: error.statusCode, body: error.data })
  }
}
</script>
