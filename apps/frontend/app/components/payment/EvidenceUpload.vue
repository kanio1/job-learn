<template>
  <UCard data-testid="evidence-upload-section">
    <template #header>
      <div class="flex items-center justify-between gap-3">
        <div>
          <h3 class="text-base font-semibold">Evidence</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400">
            Accepted: PDF, PNG, JPEG, TXT, CSV. Max size: 2 MB.
          </p>
        </div>
        <UButton
          color="neutral"
          variant="ghost"
          icon="i-lucide-refresh-cw"
          aria-label="Refresh evidence"
          :loading="loading"
          @click="loadEvidence"
        />
      </div>
    </template>

    <div class="space-y-4">
      <UAlert
        v-if="errorMessage"
        color="error"
        variant="subtle"
        title="Evidence upload failed"
        :description="errorMessage"
      />

      <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
        <input
          ref="fileInput"
          type="file"
          class="block w-full text-sm file:mr-3 file:rounded-md file:border-0 file:bg-gray-100 file:px-3 file:py-2 file:text-sm file:font-medium hover:file:bg-gray-200 dark:file:bg-gray-800 dark:hover:file:bg-gray-700"
          accept="application/pdf,image/png,image/jpeg,text/plain,text/csv"
          data-testid="evidence-upload-input"
          @change="onFileChange"
        >
        <UButton
          color="primary"
          icon="i-lucide-upload"
          :loading="uploading"
          :disabled="!selectedFile || uploading"
          data-testid="evidence-upload-submit"
          @click="handleUpload"
        >
          Upload evidence
        </UButton>
      </div>

      <p v-if="selectedFile" class="text-xs text-gray-500">
        Selected: {{ selectedFile.name }} ({{ formatBytes(selectedFile.size) }})
      </p>

      <div data-testid="evidence-list" class="space-y-2">
        <p v-if="!loading && evidence.length === 0" class="text-sm text-gray-400 italic">
          No evidence uploaded.
        </p>
        <div
          v-for="item in evidence"
          :key="item.evidenceId"
          class="grid gap-1 rounded-md border border-gray-200 p-3 text-sm dark:border-gray-800 sm:grid-cols-[1fr_auto]"
        >
          <div>
            <p class="font-medium" data-testid="evidence-file-name">{{ item.originalFilename }}</p>
            <p class="text-xs text-gray-500">{{ item.contentType }} · {{ formatBytes(item.sizeBytes) }}</p>
          </div>
          <p class="text-xs text-gray-500 sm:text-right">{{ new Date(item.uploadedAt).toLocaleString() }}</p>
        </div>
      </div>
    </div>
  </UCard>
</template>

<script setup lang="ts">
import type { PaymentEvidence } from '~/schemas/payment-order.schema'

const props = defineProps<{
  merchantId: string
  paymentOrderId: string
}>()

const toast = useAppToast()
const { listEvidence, uploadEvidence } = usePaymentEvidenceApi()

const evidence = ref<PaymentEvidence[]>([])
const selectedFile = ref<File | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const loading = ref(false)
const uploading = ref(false)
const errorMessage = ref<string | null>(null)

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
  errorMessage.value = null
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

async function loadEvidence() {
  loading.value = true
  errorMessage.value = null
  try {
    const response = await listEvidence(props.merchantId, props.paymentOrderId)
    if (response.data) {
      evidence.value = response.data.content
    } else if (response.problem) {
      errorMessage.value = response.problem.detail ?? response.problem.title ?? 'Could not load evidence.'
    }
  } finally {
    loading.value = false
  }
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  errorMessage.value = null
  try {
    const response = await uploadEvidence(props.merchantId, props.paymentOrderId, selectedFile.value)
    if (response.data) {
      evidence.value = [response.data, ...evidence.value]
      selectedFile.value = null
      if (fileInput.value) fileInput.value.value = ''
      toast.success('Evidence uploaded', 'The file metadata is now attached to this payment order.')
    } else if (response.problem) {
      errorMessage.value = response.problem.detail ?? response.problem.title ?? 'Upload failed.'
      toast.error('Evidence upload failed', errorMessage.value)
    }
  } finally {
    uploading.value = false
  }
}

onMounted(loadEvidence)
</script>
