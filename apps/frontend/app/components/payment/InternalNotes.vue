<template>
  <UCard data-testid="payment-internal-notes">
    <template #header>
      <span class="text-sm font-semibold">Internal notes</span>
    </template>

    <div class="space-y-4">
      <!-- Notes list -->
      <div data-testid="payment-note-list">
        <p
          v-if="!loading && notes.length === 0"
          class="text-sm text-gray-400 italic"
          data-testid="payment-note-empty"
        >
          No internal notes yet.
        </p>
        <ul v-if="notes.length > 0" class="space-y-3">
          <li
            v-for="note in notes"
            :key="note.id"
            class="rounded-md border border-gray-200 dark:border-gray-700 p-3 text-sm"
            data-testid="payment-note-item"
          >
            <p class="whitespace-pre-wrap text-gray-900 dark:text-gray-100">{{ note.body }}</p>
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              {{ note.authorDisplay }} &middot; {{ new Date(note.createdAt).toLocaleString() }}
            </p>
          </li>
        </ul>
      </div>

      <!-- Add note form — only for allowed roles -->
      <div v-if="can.canCreatePaymentNote" class="space-y-2">
        <UFormField label="Note body" :error="validationError ?? undefined">
          <div data-testid="payment-note-body">
            <UEditor
              ref="noteEditor"
              v-model="noteBody"
              content-type="markdown"
              placeholder="Add an internal note…"
              :image="false"
              :mention="false"
              @update:model-value="validationError = null"
            />
          </div>
        </UFormField>
        <UButton
          color="primary"
          variant="soft"
          icon="i-lucide-plus"
          :loading="submitting"
          data-testid="payment-note-submit"
          @click="handleSubmit"
        >
          Add internal note
        </UButton>
      </div>
    </div>
  </UCard>
</template>

<script setup lang="ts">
const props = defineProps<{
  merchantId: string
  paymentOrderId: string
}>()

const { can } = useAuthorization()
const { listNotes, addNote } = usePaymentNotesApi()
const { success: toastSuccess, error: toastError } = useAppToast()

const notes = ref<PaymentOrderNote[]>([])
const loading = ref(false)
const submitting = ref(false)
const noteBody = ref('')
const validationError = ref<string | null>(null)
const noteEditor = ref<{ editor?: { getText: () => string } } | null>(null)

async function load() {
  loading.value = true
  const response = await listNotes(props.merchantId, props.paymentOrderId)
  loading.value = false
  if (response.data) {
    notes.value = response.data
  }
}

async function handleSubmit() {
  const trimmed = (noteEditor.value?.editor?.getText() ?? noteBody.value).trim()
  if (!trimmed) {
    validationError.value = 'Note body must not be blank'
    return
  }
  submitting.value = true
  const response = await addNote(props.merchantId, props.paymentOrderId, trimmed)
  submitting.value = false
  if (response.data) {
    notes.value = [...notes.value, response.data]
    noteBody.value = ''
    toastSuccess('Note added')
  } else {
    toastError(response.problem?.detail ?? 'Failed to add note')
  }
}

await load()
</script>
