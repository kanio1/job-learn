<template>
  <div data-testid="support-kanban" class="space-y-3">
    <div class="flex flex-wrap items-center gap-2">
      <UButton
        v-if="can.canOperateSupport"
        data-testid="support-bulk-assign"
        size="sm"
        color="primary"
        variant="soft"
        :disabled="selectedIds.length === 0 || bulkBusy"
        @click="openAssignModal"
      >
        Assign owner ({{ selectedIds.length }})
      </UButton>
    </div>

    <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
      <section
        v-for="status in columns"
        :key="status"
        role="region"
        :aria-label="`${status} column`"
        :data-testid="`kanban-column-${status}`"
        class="flex min-h-48 flex-col rounded-lg border border-default bg-elevated/40 p-2"
        @dragover.prevent="onDragOver"
        @drop.prevent="onDrop($event, status)"
      >
        <h3
          :data-testid="`kanban-drop-${status}`"
          class="mb-2 flex min-h-10 shrink-0 items-center text-xs font-semibold uppercase text-muted"
          @dragover.prevent="onDragOver"
          @drop.prevent="onDrop($event, status)"
        >
          {{ status }}
        </h3>
        <UEmpty
          v-if="casesByStatus[status].length === 0"
          :data-testid="`kanban-empty-${status}`"
          icon="i-lucide-inbox"
          description="No cases"
        />
        <article
          v-for="supportCase in casesByStatus[status]"
          :key="supportCase.caseId"
          :aria-label="supportCase.caseReference"
          :data-testid="`support-card-${supportCase.caseId}`"
          draggable="true"
          class="mb-2 rounded-md border border-default bg-default p-2 text-sm"
          @dragstart="onDragStart($event, supportCase.caseId)"
          @dragover.prevent="onDragOver"
          @drop.prevent="onDrop($event, status)"
        >
          <div class="mb-1 flex items-start justify-between gap-2">
            <label class="flex items-center gap-2">
              <UCheckbox
                :model-value="selectedIds.includes(supportCase.caseId)"
                :data-testid="`support-card-select-${supportCase.caseId}`"
                :aria-label="`Select ${supportCase.caseReference}`"
                @update:model-value="toggleSelected(supportCase.caseId, $event)"
              />
              <span class="font-mono text-xs">{{ supportCase.caseReference }}</span>
            </label>
            <UBadge size="xs" variant="subtle">{{ supportCase.priority }}</UBadge>
          </div>
          <p class="text-xs text-muted">{{ supportCase.title }}</p>
          <p class="text-xs text-muted">
            Payment: {{ supportCase.paymentOrderId ?? '—' }}
          </p>
          <div class="mt-2 flex items-center justify-between gap-2">
            <span class="text-xs" :data-testid="`support-card-assignee-${supportCase.caseId}`">
              {{ supportCase.assigneeSubject || 'Unassigned' }}
            </span>
            <UDropdownMenu
              v-if="moveItems(supportCase).length > 0"
              :items="moveItems(supportCase)"
            >
              <UButton
                size="xs"
                color="neutral"
                variant="ghost"
                :aria-label="`Move ${supportCase.caseReference}`"
              >
                Move
              </UButton>
            </UDropdownMenu>
          </div>
        </article>
      </section>
    </div>

    <UModal v-model:open="assignOpen" title="Assign owner">
      <template #body>
        <UProgress v-if="bulkBusy" data-testid="bulk-assign-progress" class="mb-3" />
        <UFormField label="Assignee subject">
          <UInput
            v-model="assigneeSubject"
            data-testid="bulk-assign-assignee"
            aria-label="Assignee subject"
          />
        </UFormField>
      </template>
      <template #footer>
        <UButton data-testid="bulk-assign-submit" :disabled="bulkBusy" @click="runBulkAssign">
          Assign
        </UButton>
      </template>
    </UModal>

    <UModal v-model:open="resultOpen" title="Bulk assign result">
      <template #body>
        <div data-testid="bulk-assign-result" class="space-y-3">
          <p data-testid="bulk-success-count">Succeeded: {{ lastResult?.succeeded ?? 0 }}</p>
          <UAlert
            v-if="(lastResult?.failed.length ?? 0) > 0"
            color="warning"
            title="Some cases could not be assigned"
          />
          <table v-if="(lastResult?.failed.length ?? 0) > 0" class="w-full text-sm">
            <thead>
              <tr>
                <th class="text-left">Case</th>
                <th class="text-left">Reason</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in lastResult?.failed ?? []"
                :key="row.caseId"
                :data-testid="`bulk-failure-row-${row.caseId}`"
              >
                <td>{{ row.caseReference || row.caseId }}</td>
                <td>{{ humanError(row.error) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
      <template #footer>
        <UButton
          v-if="(lastResult?.failed.length ?? 0) > 0"
          data-testid="bulk-retry-failed"
          color="primary"
          @click="retryFailed"
        >
          Retry failed
        </UButton>
      </template>
    </UModal>
  </div>
</template>

<script setup lang="ts">
import type { DropdownMenuItem } from '@nuxt/ui'
import type { BulkAssignResult, SupportCase, SupportCaseStatus } from '~/schemas/support-case.schema'

const columns: SupportCaseStatus[] = ['NEW', 'IN_PROGRESS', 'WAITING', 'RESOLVED']

const { can } = useAuthorization()
const { listCases, patchCase, bulkAssign } = useSupportCasesApi()
const toast = useAppToast()

const cases = ref<SupportCase[]>([])
const selectedIds = ref<string[]>([])
const assignOpen = ref(false)
const resultOpen = ref(false)
const bulkBusy = ref(false)
const assigneeSubject = ref('support.agent')
const lastResult = ref<BulkAssignResult | null>(null)
const lastFailedIds = ref<string[]>([])

const casesByStatus = computed(() => {
  const grouped: Record<SupportCaseStatus, SupportCase[]> = {
    NEW: [],
    IN_PROGRESS: [],
    WAITING: [],
    RESOLVED: [],
  }
  for (const item of cases.value) {
    grouped[item.status].push(item)
  }
  return grouped
})

function etagOf(supportCase: SupportCase): string {
  return `"v${supportCase.version}"`
}

function legalTargets(status: SupportCaseStatus): SupportCaseStatus[] {
  if (status === 'NEW') return ['IN_PROGRESS']
  if (status === 'IN_PROGRESS') return ['WAITING', 'RESOLVED']
  if (status === 'WAITING') return ['IN_PROGRESS', 'RESOLVED']
  return []
}

function moveItems(supportCase: SupportCase): DropdownMenuItem[] {
  if (!can.value.canOperateSupport) {
    return []
  }
  return legalTargets(supportCase.status).map(status => ({
    label: `Move to ${status}`,
    onSelect: () => {
      void moveCase(supportCase, status)
    },
  }))
}

function onDragStart(event: DragEvent, caseId: string) {
  event.dataTransfer?.setData('text/plain', caseId)
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function onDragOver(event: DragEvent) {
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

function onDrop(event: DragEvent, status: SupportCaseStatus) {
  const caseId = event.dataTransfer?.getData('text/plain')
  const supportCase = cases.value.find(item => item.caseId === caseId)
  if (supportCase) {
    void moveCase(supportCase, status)
  }
}

async function moveCase(supportCase: SupportCase, target: SupportCaseStatus) {
  if (supportCase.status === target) {
    return
  }
  const snapshot = { ...supportCase }
  cases.value = cases.value.map(item =>
    item.caseId === supportCase.caseId ? { ...item, status: target } : item,
  )
  const response = await patchCase(supportCase.caseId, { status: target }, etagOf(supportCase))
  if (response.data) {
    cases.value = cases.value.map(item =>
      item.caseId === response.data!.caseId ? response.data! : item,
    )
    return
  }
  cases.value = cases.value.map(item =>
    item.caseId === snapshot.caseId ? snapshot : item,
  )
  toast.error('Move failed', response.problem?.detail || response.problem?.title || 'Case was not moved')
}

function toggleSelected(caseId: string, checked: boolean | 'indeterminate') {
  const on = checked === true
  if (on && !selectedIds.value.includes(caseId)) {
    selectedIds.value = [...selectedIds.value, caseId]
    return
  }
  if (!on) {
    selectedIds.value = selectedIds.value.filter(id => id !== caseId)
  }
}

function openAssignModal() {
  assignOpen.value = true
}

async function runBulkAssign() {
  await postBulk(selectedIds.value)
}

async function retryFailed() {
  await postBulk(lastFailedIds.value)
}

async function postBulk(caseIds: string[]) {
  if (caseIds.length === 0) {
    return
  }
  bulkBusy.value = true
  assignOpen.value = true
  const response = await bulkAssign(caseIds, assigneeSubject.value.trim())
  bulkBusy.value = false
  assignOpen.value = false
  if (!response.data) {
    toast.error('Bulk assign failed', response.problem?.detail || response.problem?.title)
    return
  }
  lastResult.value = response.data
  lastFailedIds.value = response.data.failed.map(row => row.caseId)
  selectedIds.value = lastFailedIds.value
  resultOpen.value = true
  await reload()
}

function humanError(error: string): string {
  switch (error) {
    case 'already_resolved':
      return 'Already resolved'
    case 'not_found':
      return 'Not found'
    case 'precondition_failed':
      return 'Updated by another operator'
    default:
      return error.replaceAll('_', ' ')
  }
}

async function reload() {
  const response = await listCases()
  if (response.data) {
    cases.value = response.data.content
  }
}

onMounted(() => {
  void reload()
})

defineExpose({ reload })
</script>
