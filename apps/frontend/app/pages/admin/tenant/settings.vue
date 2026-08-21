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

        <UAccordion
          :items="policyAccordionItems"
          type="single"
          default-value="policy"
          :unmount-on-hide="false"
          data-testid="policy-accordion"
        >
          <template #body>
            <RuleConfigurator
              v-model:auto-capture="form.autoCapture"
              v-model:max-auto-capture-minor="form.maxAutoCaptureMinor"
              v-model:risk-threshold="form.riskThreshold"
              v-model:refund-policy="form.refundPolicy"
              :max-error="policyMaxError"
            />
          </template>
        </UAccordion>

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
import { paymentPolicySchema, DEFAULT_PAYMENT_POLICY } from '~/schemas/tenant-settings.schema'

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
const policyMaxError = ref<string | null>(null)

let currentEtag = ''

const policyAccordionItems = [
  { label: 'Payment rules', value: 'policy', icon: 'i-lucide-sliders-horizontal' },
]

const form = reactive({
  contactEmail: '',
  timezone: 'UTC',
  webhookBaseUrl: '',
  autoCapture: DEFAULT_PAYMENT_POLICY.autoCapture,
  maxAutoCaptureMinor: DEFAULT_PAYMENT_POLICY.maxAutoCaptureMinor as number | null,
  riskThreshold: DEFAULT_PAYMENT_POLICY.riskThreshold,
  refundPolicy: DEFAULT_PAYMENT_POLICY.refundPolicy,
})

function applyPolicy(policy: typeof DEFAULT_PAYMENT_POLICY) {
  form.autoCapture = policy.autoCapture
  form.maxAutoCaptureMinor = policy.autoCapture ? policy.maxAutoCaptureMinor : 0
  form.riskThreshold = policy.riskThreshold
  form.refundPolicy = policy.refundPolicy
}

async function load() {
  loading.value = true
  loadError.value = null
  const response = await getSettings()
  loading.value = false
  if (response.data) {
    form.contactEmail = response.data.contactEmail ?? ''
    form.timezone = response.data.timezone
    form.webhookBaseUrl = response.data.webhookBaseUrl ?? ''
    applyPolicy(response.data.paymentPolicy)
    currentEtag = response.headers?.etag ?? ''
  } else {
    loadError.value = response.problem ?? null
  }
}

async function handleSave() {
  saving.value = true
  saveError.value = null
  policyMaxError.value = null
  const max = form.autoCapture ? form.maxAutoCaptureMinor : 0
  const parsedPolicy = paymentPolicySchema.safeParse({
    autoCapture: form.autoCapture,
    maxAutoCaptureMinor: max ?? 0,
    riskThreshold: form.riskThreshold,
    refundPolicy: form.refundPolicy,
  })
  if (!parsedPolicy.success) {
    saving.value = false
    const maxIssue = parsedPolicy.error.issues.find(issue => issue.path[0] === 'maxAutoCaptureMinor')
    policyMaxError.value = maxIssue?.message ?? parsedPolicy.error.issues[0]?.message ?? 'Invalid payment policy'
    return
  }
  const response = await updateSettings(
    {
      contactEmail: form.contactEmail || undefined,
      timezone: form.timezone,
      webhookBaseUrl: form.webhookBaseUrl || undefined,
      paymentPolicy: parsedPolicy.data,
    },
    currentEtag,
  )
  saving.value = false
  if (response.data) {
    currentEtag = response.headers?.etag ?? currentEtag
    form.contactEmail = response.data.contactEmail ?? ''
    form.timezone = response.data.timezone
    form.webhookBaseUrl = response.data.webhookBaseUrl ?? ''
    applyPolicy(response.data.paymentPolicy)
    toastSuccess('Settings saved')
  } else {
    saveError.value = response.problem ?? null
    if (response.status === 400) {
      policyMaxError.value = response.problem?.detail ?? 'Invalid payment policy'
    }
    if (!response.problem) {
      toastError('Failed to save settings')
    }
  }
}

await load()
</script>
