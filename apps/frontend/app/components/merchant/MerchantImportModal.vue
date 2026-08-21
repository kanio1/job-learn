<template>
  <UModal v-model:open="open" title="Import merchants">
    <template #body>
      <div data-testid="merchant-import-panel" class="space-y-4">
        <div>
          <label class="mb-1 block text-sm font-medium" for="merchant-import-csv">Upload CSV</label>
          <input
            id="merchant-import-csv"
            data-testid="merchant-import-csv"
            type="file"
            accept="text/csv,.csv"
            aria-label="Upload CSV"
            @change="onFile"
          >
        </div>

        <ErrorState
          v-if="problem"
          :problem="problem"
          :message="errorMessage ?? undefined"
        />

        <div v-if="preview" class="space-y-2 text-sm">
          <p data-testid="merchant-import-valid">Valid: {{ preview.validCount }}</p>
          <p data-testid="merchant-import-warning">Warning: {{ preview.warningCount }}</p>
          <p data-testid="merchant-import-rejected">Rejected: {{ preview.rejectedCount }}</p>
          <UButton
            data-testid="merchant-import-download-rejected"
            color="neutral"
            variant="ghost"
            size="sm"
            :disabled="preview.rejectedCount === 0"
            @click="downloadRejected"
          >
            Download rejected
          </UButton>
        </div>
      </div>
    </template>
    <template #footer>
      <div class="flex justify-end gap-2">
        <UButton color="neutral" variant="ghost" @click="open = false">Close</UButton>
        <UButton
          data-testid="merchant-import-commit"
          color="primary"
          :disabled="!canCommit"
          :loading="committing"
          @click="commit"
        >
          Commit import
        </UButton>
      </div>
    </template>
  </UModal>
</template>

<script setup lang="ts">
import type { MerchantImportPreview } from '~/schemas/merchant-import.schema'
import type { ProblemDetails } from '~/types/api'

const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{
  committed: []
}>()

const { previewMerchantImport, commitMerchantImport } = useMerchantsApi()
const toast = useToast()

const preview = ref<MerchantImportPreview | null>(null)
const problem = ref<ProblemDetails | null>(null)
const errorMessage = ref<string | null>(null)
const committing = ref(false)

const canCommit = computed(() => {
  if (!preview.value) {
    return false
  }
  return preview.value.validCount + preview.value.warningCount > 0
})

function downloadRejected() {
  if (!preview.value) {
    return
  }
  const rejected = preview.value.rows.filter(row => row.status === 'REJECTED')
  const header = 'line,merchantReference,displayName,tenantReference,reason'
  const body = rejected.map(row =>
    [row.line, row.merchantReference, row.displayName, row.tenantReference, row.reason ?? ''].join(','),
  ).join('\n')
  const blob = new Blob([`${header}\n${body}\n`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'rejected-merchants.csv'
  link.click()
  URL.revokeObjectURL(url)
}

watch(open, (isOpen) => {
  if (!isOpen) {
    preview.value = null
    problem.value = null
    errorMessage.value = null
  }
})

async function onFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  preview.value = null
  problem.value = null
  errorMessage.value = null
  if (!file) {
    return
  }
  const response = await previewMerchantImport(file)
  if (response.data) {
    preview.value = response.data
  } else {
    problem.value = response.problem
    errorMessage.value = response.problem?.detail || response.problem?.title || 'Import preview failed'
  }
}

async function commit() {
  if (!preview.value) {
    return
  }
  committing.value = true
  const response = await commitMerchantImport(preview.value.previewId)
  committing.value = false
  if (response.data) {
    toast.add({ title: `Imported ${response.data.createdCount} merchant(s)`, color: 'primary' })
    open.value = false
    emit('committed')
  } else {
    problem.value = response.problem
    errorMessage.value = response.problem?.detail || response.problem?.title || 'Import commit failed'
  }
}
</script>
