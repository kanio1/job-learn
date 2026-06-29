<template>
  <div class="max-w-2xl mx-auto space-y-6 p-4">
    <div>
      <h1 class="text-xl font-semibold">Tenant settings</h1>
      <p class="text-sm text-gray-500 mt-1">
        Update contact information and integration settings for this tenant.
      </p>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="flex items-center gap-2 text-sm text-gray-400">
      <UIcon name="i-lucide-loader-circle" class="animate-spin" />
      Loading settings…
    </div>

    <!-- Load error -->
    <ProblemDetailsCard
      v-else-if="loadError"
      :problem="loadError"
      data-testid="tenant-settings-error"
    />

    <!-- Settings form -->
    <UCard v-else data-testid="tenant-settings-form">
      <template #header>
        <span class="text-sm font-semibold">Settings</span>
      </template>

      <div class="space-y-4">
        <UFormField label="Contact email">
          <UInput
            v-model="form.contactEmail"
            type="email"
            placeholder="ops@example.com"
            data-testid="tenant-settings-contact-email"
          />
        </UFormField>

        <UFormField label="Timezone">
          <UInput
            v-model="form.timezone"
            placeholder="Europe/Warsaw"
            data-testid="tenant-settings-timezone"
          />
        </UFormField>

        <UFormField label="Webhook base URL" hint="Optional — must start with https://">
          <UInput
            v-model="form.webhookBaseUrl"
            type="url"
            placeholder="https://example.com/webhooks"
            data-testid="tenant-settings-webhook-base-url"
          />
        </UFormField>

        <!-- Stale ETag error -->
        <ProblemDetailsCard
          v-if="saveError"
          :problem="saveError"
          data-testid="tenant-settings-error"
        />

        <div class="flex justify-end">
          <UButton
            color="primary"
            :loading="saving"
            data-testid="tenant-settings-save"
            @click="handleSave"
          >
            Save tenant settings
          </UButton>
        </div>
      </div>
    </UCard>
  </div>
</template>

<script setup lang="ts">
import type { ProblemDetails } from '~/types/api'

definePageMeta({ layout: 'dashboard' })

const { getSettings, updateSettings } = useTenantSettingsApi()
const { can } = useAuthorization()
const { success: toastSuccess, error: toastError } = useAppToast()

if (!can.value.canManageTenantSettings) {
  await navigateTo('/')
}

const loading = ref(true)
const saving = ref(false)
const loadError = ref<ProblemDetails | null>(null)
const saveError = ref<ProblemDetails | null>(null)

let currentEtag = ''

const form = reactive({
  contactEmail: '',
  timezone: 'UTC',
  webhookBaseUrl: '',
})

async function load() {
  loading.value = true
  loadError.value = null
  const response = await getSettings()
  loading.value = false
  if (response.data) {
    form.contactEmail = response.data.contactEmail ?? ''
    form.timezone = response.data.timezone
    form.webhookBaseUrl = response.data.webhookBaseUrl ?? ''
    currentEtag = response.headers?.etag ?? ''
  } else {
    loadError.value = response.problem ?? null
  }
}

async function handleSave() {
  saving.value = true
  saveError.value = null
  const response = await updateSettings(
    {
      contactEmail: form.contactEmail || undefined,
      timezone: form.timezone,
      webhookBaseUrl: form.webhookBaseUrl || undefined,
    },
    currentEtag,
  )
  saving.value = false
  if (response.data) {
    currentEtag = response.headers?.etag ?? currentEtag
    form.contactEmail = response.data.contactEmail ?? ''
    form.timezone = response.data.timezone
    form.webhookBaseUrl = response.data.webhookBaseUrl ?? ''
    toastSuccess('Settings saved')
  } else {
    saveError.value = response.problem ?? null
    if (!response.problem) {
      toastError('Failed to save settings')
    }
  }
}

await load()
</script>
