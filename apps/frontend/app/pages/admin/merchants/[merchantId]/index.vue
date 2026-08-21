<template>
  <UDashboardPanel id="merchant-detail" data-testid="merchant-detail-panel">
    <template #header>
      <UDashboardNavbar :title="merchant?.displayName ?? 'Merchant Details'">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
        <template #right>
          <UButton
            color="neutral"
            variant="ghost"
            icon="i-lucide-arrow-left"
            label="Back to merchants"
            data-testid="merchant-back-to-list"
            @click="leaveToList"
          />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <LoadingState v-if="loading" message="Loading merchant…" />

      <ErrorState
        v-else-if="loadError"
        :problem="loadProblem"
        :message="loadError"
        :on-retry="load"
      />

      <div v-else-if="merchant" class="space-y-4">
        <!-- Merchant Info -->
        <UCard>
          <template #header>
            <div class="flex items-center justify-between gap-2">
              <span class="text-sm font-semibold">Merchant Information</span>
              <MerchantStatusBadge :status="merchant.status" data-testid="merchant-status-badge" />
            </div>
          </template>

          <dl class="space-y-2 text-sm">
            <div class="flex gap-2">
              <dt class="w-36 shrink-0 font-medium text-gray-500 dark:text-gray-400">Name</dt>
              <dd data-testid="merchant-name" class="text-gray-900 dark:text-gray-100">{{ merchant.displayName }}</dd>
            </div>
            <div class="flex gap-2">
              <dt class="w-36 shrink-0 font-medium text-gray-500 dark:text-gray-400">Reference</dt>
              <dd data-testid="merchant-reference" class="font-mono text-xs text-gray-900 dark:text-gray-100">{{ merchant.merchantReference }}</dd>
            </div>
            <div class="flex gap-2">
              <dt class="w-36 shrink-0 font-medium text-gray-500 dark:text-gray-400">Created</dt>
              <dd class="text-gray-900 dark:text-gray-100">{{ new Date(merchant.createdAt).toLocaleString() }}</dd>
            </div>
            <div class="flex gap-2">
              <dt class="w-36 shrink-0 font-medium text-gray-500 dark:text-gray-400">Updated</dt>
              <dd class="text-gray-900 dark:text-gray-100">{{ new Date(merchant.updatedAt).toLocaleString() }}</dd>
            </div>
          </dl>
        </UCard>

        <UCard v-if="can.canUpdateMerchantStatus" data-testid="merchant-contact-form">
          <template #header>
            <span class="text-sm font-semibold">Contact{{ isDirty ? ' (unsaved changes)' : '' }}</span>
          </template>
          <form class="space-y-3" @submit.prevent="handleSaveContact">
            <UFormField label="Display name">
              <UInput
                v-model="form.displayName"
                data-testid="merchant-display-name-input"
                aria-label="Display name"
              />
            </UFormField>
            <UFormField label="Contact phone">
              <UInput
                v-model="form.contactPhone"
                data-testid="merchant-contact-phone-input"
                aria-label="Contact phone"
                maxlength="32"
              />
            </UFormField>
            <UFormField label="Contact address">
              <UInput
                v-model="form.contactAddress"
                data-testid="merchant-contact-address-input"
                aria-label="Contact address"
                maxlength="200"
              />
            </UFormField>
            <UButton
              type="submit"
              data-testid="merchant-save"
              color="primary"
              :loading="savingContact"
              :disabled="!isDirty"
            >
              Save
            </UButton>
          </form>
        </UCard>

        <UModal
          v-model:open="showUnsaved"
          title="You have unsaved changes."
          :ui="{ content: 'max-w-md' }"
        >
          <template #content>
            <div
              data-testid="unsaved-changes-dialog"
              class="p-4 space-y-3"
            >
              <p class="text-sm font-medium">You have unsaved changes.</p>
              <div class="flex justify-end gap-2">
                <UButton data-testid="unsaved-stay" color="neutral" variant="soft" @click="stayOnPage">Stay</UButton>
                <UButton data-testid="unsaved-discard" color="error" variant="soft" @click="discardAndLeave">Discard and leave</UButton>
              </div>
            </div>
          </template>
        </UModal>

        <UModal
          v-model:open="showConflict"
          title="Record changed by another user"
          :ui="{ content: 'max-w-lg' }"
        >
          <template #content>
            <div
              data-testid="merchant-conflict-dialog"
              class="p-4 space-y-3"
            >
              <UAlert
                color="warning"
                title="Record changed by another user."
                description="Your save was not applied."
              />
              <UTabs :items="conflictTabs" class="w-full">
                <template #yours>
                  <dl class="text-sm space-y-1" data-testid="conflict-yours">
                    <div>Phone YOUR: {{ yours.contactPhone || '—' }}</div>
                    <div>Address YOUR: {{ yours.contactAddress || '—' }}</div>
                    <div>Name YOUR: {{ yours.displayName }}</div>
                  </dl>
                </template>
                <template #latest>
                  <dl class="text-sm space-y-1" data-testid="conflict-latest">
                    <div>Phone SERVER: {{ latest?.contactPhone || '—' }}</div>
                    <div>Address SERVER: {{ latest?.contactAddress || '—' }}</div>
                    <div>Name SERVER: {{ latest?.displayName }}</div>
                  </dl>
                </template>
              </UTabs>
              <div class="flex justify-end gap-2">
                <UButton data-testid="conflict-discard" color="neutral" variant="soft" @click="discardMine">Discard mine</UButton>
                <UButton data-testid="conflict-reload" color="primary" variant="soft" @click="reloadLatest">Reload latest</UButton>
              </div>
            </div>
          </template>
        </UModal>

        <!-- Status Actions -->
        <UCard v-if="can.canUpdateMerchantStatus">
          <template #header>
            <span class="text-sm font-semibold">Status Actions</span>
          </template>
          <div class="flex flex-wrap gap-2">
            <UButton
              v-if="merchant.status === 'DRAFT'"
              data-testid="action-activate-merchant"
              color="success"
              variant="soft"
              icon="i-lucide-circle-check"
              :loading="activating"
              @click="handleActivate"
            >Activate</UButton>
            <UButton
              v-if="merchant.status === 'ACTIVE'"
              data-testid="action-suspend-merchant"
              color="warning"
              variant="soft"
              icon="i-lucide-pause-circle"
              :loading="suspending"
              @click="handleSuspend"
            >Suspend</UButton>
          </div>
        </UCard>

        <ErrorState
          v-if="actionProblem"
          :problem="actionProblem"
          retry-label="Reload"
          :on-retry="reloadAfterConflict"
        />

        <!-- Risk Review -->
        <UCard data-testid="merchant-risk-panel">
          <template #header>
            <span class="text-sm font-semibold">Risk Review</span>
          </template>
          <div class="flex items-center gap-4">
            <UBadge
              data-testid="merchant-risk-status"
              :color="merchant.riskFlagged ? 'error' : 'success'"
              variant="subtle"
              :icon="merchant.riskFlagged ? 'i-lucide-flag' : 'i-lucide-shield-check'"
            >
              {{ merchant.riskFlagged ? 'Risk flagged' : 'No risk flag' }}
            </UBadge>
            <UButton
              v-if="can.canUpdateMerchantRiskFlag"
              data-testid="merchant-risk-toggle"
              :color="merchant.riskFlagged ? 'neutral' : 'error'"
              variant="soft"
              :icon="merchant.riskFlagged ? 'i-lucide-x-circle' : 'i-lucide-flag'"
              :loading="updatingRiskFlag"
              @click="handleRiskFlagToggle"
            >
              {{ merchant.riskFlagged ? 'Clear risk flag' : 'Mark as risk flagged' }}
            </UButton>
          </div>
        </UCard>

        <!-- ETag + Correlation ID -->
        <UCard v-if="responseEtag || responseCorrelationId">
          <template #header>
            <span class="text-sm font-semibold">Response Metadata</span>
          </template>
          <div class="space-y-2 text-sm">
            <EtagDisplay v-if="responseEtag" :etag="responseEtag" />
            <div v-if="responseCorrelationId" class="flex gap-2">
              <span class="w-28 shrink-0 font-medium text-gray-500 dark:text-gray-400">Correlation ID</span>
              <span class="font-mono text-xs break-all text-gray-900 dark:text-gray-100">{{ responseCorrelationId }}</span>
            </div>
          </div>
        </UCard>

        <!-- Response Headers -->
        <UCard v-if="hasResponseHeaders">
          <template #header>
            <span class="text-sm font-semibold">Response Headers</span>
          </template>
          <HeaderKeyValuePanel :headers="responseHeaders" />
        </UCard>

        <!-- Link to Payment Orders -->
        <div class="flex">
          <UButton
            data-testid="merchant-payment-orders-link"
            color="primary"
            variant="soft"
            icon="i-lucide-credit-card"
            :to="`/admin/merchants/${merchantId}/payments`"
          >
            View Payment Orders
          </UButton>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'dashboard' })

import type { MerchantResponse } from '~/composables/useMerchantsApi'
import type { ProblemDetails } from '~/types/api'
import { merchantIfMatch } from '~/utils/merchant-etag'

const route = useRoute()
const merchantId = route.params.merchantId as string

const { getMerchant, activateMerchant, suspendMerchant, updateMerchantRiskFlag, patchMerchant } = useMerchantsApi()
const { can } = useAuthorization()
const { success: toastSuccess, error: toastError, warning: toastWarning } = useAppToast()

const merchant = ref<MerchantResponse | null>(null)
const loading = ref(true)
const loadError = ref<string | null>(null)
const loadProblem = ref<ProblemDetails | null>(null)
const activating = ref(false)
const suspending = ref(false)
const updatingRiskFlag = ref(false)
const savingContact = ref(false)
const actionProblem = ref<ProblemDetails | null>(null)
const showUnsaved = ref(false)
const showConflict = ref(false)
const pendingLeaveTo = ref<string | null>(null)
const bypassGuard = ref(false)

const form = reactive({
  displayName: '',
  contactPhone: '',
  contactAddress: '',
})
const lastLoaded = reactive({
  displayName: '',
  contactPhone: '',
  contactAddress: '',
})
const yours = reactive({
  displayName: '',
  contactPhone: '',
  contactAddress: '',
})
const latest = ref<MerchantResponse | null>(null)

const isDirty = computed(() => {
  return form.displayName !== lastLoaded.displayName
    || form.contactPhone !== lastLoaded.contactPhone
    || form.contactAddress !== lastLoaded.contactAddress
})

function applyLoaded(data: MerchantResponse) {
  form.displayName = data.displayName
  form.contactPhone = data.contactPhone ?? ''
  form.contactAddress = data.contactAddress ?? ''
  lastLoaded.displayName = form.displayName
  lastLoaded.contactPhone = form.contactPhone
  lastLoaded.contactAddress = form.contactAddress
}

const responseEtag = ref<string | undefined>()
const responseCorrelationId = ref<string | undefined>()
const responseHeaders = ref<Record<string, string>>({})

const hasResponseHeaders = computed(() => Object.keys(responseHeaders.value).length > 0)

const conflictTabs = [
  { label: 'Your changes', slot: 'yours' as const },
  { label: 'Latest version', slot: 'latest' as const },
]

async function load() {
  loading.value = true
  loadError.value = null
  loadProblem.value = null

  const response = await getMerchant(merchantId)
  loading.value = false

  if (response.data) {
    merchant.value = response.data
    applyLoaded(response.data)
    responseEtag.value = response.headers.etag
    responseCorrelationId.value = response.headers.correlationId
    const h = response.headers
    const headers: Record<string, string> = {}
    if (h.etag) headers['ETag'] = h.etag
    if (h.cacheControl) headers['Cache-Control'] = h.cacheControl
    if (h.vary) headers['Vary'] = h.vary
    if (h.correlationId) headers['X-Correlation-ID'] = h.correlationId
    responseHeaders.value = headers
    actionProblem.value = null
  } else {
    loadError.value = response.problem?.detail ?? 'Failed to load merchant'
    loadProblem.value = response.problem
  }
}

function currentIfMatch(): string {
  return merchantIfMatch(responseEtag.value, merchant.value?.version)
}

function applyMerchantWrite(response: Awaited<ReturnType<typeof activateMerchant>>): boolean {
  if (response.data) {
    merchant.value = response.data
    if (response.headers.etag) {
      responseEtag.value = response.headers.etag
    }
    actionProblem.value = null
    return true
  }
  if (response.status === 412) {
    actionProblem.value = response.problem
    return false
  }
  return false
}

async function reloadAfterConflict() {
  actionProblem.value = null
  await load()
}

async function handleActivate() {
  activating.value = true
  const response = await activateMerchant(merchantId, currentIfMatch())
  activating.value = false
  if (applyMerchantWrite(response)) {
    toastSuccess('Merchant activated')
  } else if (response.status !== 412) {
    toastError(response.problem?.detail ?? 'Activation failed')
  }
}

async function handleSuspend() {
  suspending.value = true
  const response = await suspendMerchant(merchantId, currentIfMatch())
  suspending.value = false
  if (applyMerchantWrite(response)) {
    toastWarning('Merchant suspended')
  } else if (response.status !== 412) {
    toastError(response.problem?.detail ?? 'Suspend failed')
  }
}

async function handleRiskFlagToggle() {
  if (!merchant.value) return
  updatingRiskFlag.value = true
  const newValue = !merchant.value.riskFlagged
  const response = await updateMerchantRiskFlag(merchantId, newValue, currentIfMatch())
  updatingRiskFlag.value = false
  if (applyMerchantWrite(response)) {
    toastSuccess(newValue ? 'Risk flag set' : 'Risk flag cleared')
  } else if (response.status !== 412) {
    toastError(response.problem?.detail ?? 'Risk flag update failed')
  }
}

async function handleSaveContact() {
  if (!merchant.value) return
  savingContact.value = true
  yours.displayName = form.displayName
  yours.contactPhone = form.contactPhone
  yours.contactAddress = form.contactAddress
  const response = await patchMerchant(
    merchantId,
    {
      displayName: form.displayName,
      contactPhone: form.contactPhone === '' ? null : form.contactPhone,
      contactAddress: form.contactAddress === '' ? null : form.contactAddress,
    },
    currentIfMatch(),
  )
  savingContact.value = false
  if (response.data) {
    merchant.value = response.data
    applyLoaded(response.data)
    if (response.headers.etag) {
      responseEtag.value = response.headers.etag
    }
    actionProblem.value = null
    toastSuccess('Merchant contact saved')
    return
  }
  if (response.status === 412) {
    actionProblem.value = response.problem
    const fresh = await getMerchant(merchantId)
    if (fresh.data) {
      latest.value = fresh.data
      if (fresh.headers.etag) {
        responseEtag.value = fresh.headers.etag
      }
      showConflict.value = true
    }
    return
  }
  toastError(response.problem?.detail ?? 'Save failed')
}

function discardMine() {
  if (!latest.value) return
  merchant.value = latest.value
  applyLoaded(latest.value)
  showConflict.value = false
}

function reloadLatest() {
  discardMine()
  toastSuccess('Reloaded latest version')
}

function stayOnPage() {
  showUnsaved.value = false
  pendingLeaveTo.value = null
}

async function discardAndLeave() {
  showUnsaved.value = false
  bypassGuard.value = true
  applyLoaded(merchant.value ?? {
    merchantId,
    merchantReference: '',
    displayName: lastLoaded.displayName,
    status: 'DRAFT',
    createdAt: '',
    updatedAt: '',
    riskFlagged: false,
    version: 0,
    contactPhone: lastLoaded.contactPhone,
    contactAddress: lastLoaded.contactAddress,
  })
  const target = pendingLeaveTo.value ?? '/admin/merchants'
  pendingLeaveTo.value = null
  await navigateTo(target)
}

async function leaveToList(): Promise<void> {
  await navigateTo('/admin/merchants')
}

const unsavedHistoryPushed = ref(false)

watch(isDirty, (dirty) => {
  if (dirty && !unsavedHistoryPushed.value) {
    history.pushState({ merchantUnsavedGuard: true }, '', window.location.href)
    unsavedHistoryPushed.value = true
  }
  if (!dirty) {
    unsavedHistoryPushed.value = false
  }
})

onBeforeRouteLeave((to) => {
  if (bypassGuard.value || !isDirty.value) {
    return true
  }
  pendingLeaveTo.value = to.fullPath
  showUnsaved.value = true
  return false
})

function onPopState() {
  if (bypassGuard.value || !isDirty.value) {
    return
  }
  history.pushState({ merchantUnsavedGuard: true }, '', window.location.href)
  pendingLeaveTo.value = '/admin/merchants'
  showUnsaved.value = true
}

function onBeforeUnload(event: BeforeUnloadEvent) {
  if (!isDirty.value) {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

onMounted(() => {
  window.addEventListener('beforeunload', onBeforeUnload)
  window.addEventListener('popstate', onPopState)
})
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', onBeforeUnload)
  window.removeEventListener('popstate', onPopState)
})

await load()
</script>
