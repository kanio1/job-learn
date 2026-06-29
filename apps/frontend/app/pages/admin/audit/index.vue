<template>
  <UDashboardPanel id="audit">
    <template #header>
      <UDashboardNavbar title="Audit log">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
        <template #right>
          <UTooltip v-if="canViewAuditLog && !forbiddenByApi" text="Export audit log">
            <UButton
              data-testid="export-audit-log"
              color="neutral"
              variant="ghost"
              icon="i-lucide-download"
              label="Export audit log"
              aria-label="Export audit log"
              @click="handleExportAuditLog"
            />
          </UTooltip>
          <UTooltip v-if="canViewAuditLog && !forbiddenByApi" text="Refresh audit log">
            <UButton
              color="neutral"
              variant="ghost"
              square
              icon="i-lucide-refresh-cw"
              aria-label="Refresh audit log"
              @click="refreshFromRoute"
            />
          </UTooltip>
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <div class="space-y-6">
        <div>
          <h1 ref="heading" tabindex="-1" class="text-2xl font-semibold text-highlighted">Audit log</h1>
          <p class="mt-1 text-sm text-muted">Tenant-scoped, read-only visibility into important platform actions.</p>
        </div>

        <UAlert
          v-if="forbidden"
          color="warning"
          variant="subtle"
          icon="i-lucide-shield-alert"
          title="Audit log access denied"
          description="Your current role cannot view audit events."
          role="alert"
          data-testid="audit-state-forbidden"
        />

        <template v-else>
          <AuditFilters
            :model-value="filters"
            @apply="applyFilters"
            @clear="clearFilters"
          />

          <div v-if="loading" data-testid="audit-state-loading">
            <LoadingState message="Loading audit events…" />
          </div>

          <div v-else-if="loadError" data-testid="audit-state-error">
            <ErrorState :problem="loadProblem" :message="loadError" :on-retry="refreshFromRoute" />
          </div>

          <UAlert
            v-else-if="deepLinkNotFound"
            color="warning"
            variant="subtle"
            icon="i-lucide-file-question"
            title="Audit event not found"
            description="The requested event is unavailable in your current scope."
            role="alert"
            data-testid="audit-state-deep-link-not-found"
          >
            <template #actions>
              <UButton color="neutral" variant="soft" label="Dismiss" @click="closeEntry" />
            </template>
          </UAlert>

          <div
            v-else-if="list && list.content.length === 0 && !hasActiveFilters"
            data-testid="audit-state-empty"
          >
            <EmptyStateCard description="No audit events are available in your current scope." />
          </div>

          <div
            v-else-if="list && list.content.length === 0"
            data-testid="audit-state-filtered-empty"
          >
            <EmptyStateCard
              description="No audit events match the active filters."
              action-label="Clear filters"
              @action="clearFilters"
            />
          </div>

          <template v-else-if="list">
            <UCard>
              <template #header>
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h2 class="text-base font-semibold text-highlighted">Events</h2>
                    <p class="text-sm text-muted">{{ list.totalElements }} event(s) in scope</p>
                  </div>
                  <p class="text-sm text-muted">Page {{ list.page + 1 }} of {{ Math.max(list.totalPages, 1) }}</p>
                </div>
              </template>

              <AuditTable :events="list.content" @select="openEntry" />

              <template v-if="list.totalPages > 1" #footer>
                <div class="flex justify-center">
                  <UPagination
                    :default-page="list.page + 1"
                    :total="list.totalElements"
                    :page-count="list.size"
                    aria-label="Audit log pagination"
                    @update:page="changePage"
                  />
                </div>
              </template>
            </UCard>
          </template>
        </template>
      </div>

      <AuditEntryDrawer
        :open="drawerOpen"
        :entry="selectedEntry"
        @update:open="handleDrawerOpen"
      />
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
import AuditEntryDrawer from '~/components/audit/AuditEntryDrawer.vue'
import AuditFilters, { type AuditFilterState } from '~/components/audit/AuditFilters.vue'
import AuditTable from '~/components/audit/AuditTable.vue'
import EmptyStateCard from '~/components/shared/EmptyStateCard.vue'
import ErrorState from '~/components/shared/ErrorState.vue'
import LoadingState from '~/components/shared/LoadingState.vue'
import { auditQuerySchema, type AuditEvent, type AuditListResponse } from '~/schemas/audit.schema'
import type { ProblemDetails } from '~/types/api'

definePageMeta({ layout: 'dashboard' })

const route = useRoute()
const router = useRouter()
const { can } = useAuthorization()
const { list: listAudit, getEntry } = useAuditApi()

const heading = ref<HTMLElement | null>(null)
const list = ref<AuditListResponse | null>(null)
const selectedEntry = ref<AuditEvent | null>(null)
const drawerOpen = ref(false)
const loading = ref(false)
const forbiddenByApi = ref(false)
const deepLinkNotFound = ref(false)
const loadError = ref<string | null>(null)
const loadProblem = ref<ProblemDetails | null>(null)
const filters = reactive<AuditFilterState>({ actor: '', action: '', targetType: '', from: '', to: '' })
let requestSequence = 0

const canViewAuditLog = computed(() => can.value.canViewAuditLog)
const forbidden = computed(() => !canViewAuditLog.value || forbiddenByApi.value)
const hasActiveFilters = computed(() => Boolean(
  filters.actor || filters.action || filters.targetType || filters.from || filters.to
))

async function refreshFromRoute() {
  const sequence = ++requestSequence
  const routeFilters = readFiltersFromRoute()
  Object.assign(filters, routeFilters)

  if (!canViewAuditLog.value) {
    loading.value = false
    list.value = null
    selectedEntry.value = null
    drawerOpen.value = false
    return
  }

  const parsed = auditQuerySchema.safeParse({
    ...routeFilters,
    from: routeFilters.from || undefined,
    to: routeFilters.to || undefined,
    page: queryInteger(route.query.page, 0),
    size: queryInteger(route.query.size, 20),
  })

  if (!parsed.success) {
    list.value = null
    loadProblem.value = {
      type: 'about:blank',
      title: 'Invalid audit filters',
      status: 400,
      detail: parsed.error.issues.map(issue => issue.message).join('; '),
    }
    loadError.value = 'The audit URL contains invalid filters.'
    loading.value = false
    return
  }

  loading.value = true
  forbiddenByApi.value = false
  deepLinkNotFound.value = false
  loadError.value = null
  loadProblem.value = null

  const response = await listAudit(parsed.data)
  if (sequence !== requestSequence) return

  if (response.status === 403) {
    forbiddenByApi.value = true
    list.value = null
  } else if (response.data) {
    list.value = response.data
    await loadDeepLink(sequence)
  } else {
    list.value = null
    loadProblem.value = response.problem
    loadError.value = response.problem?.detail || response.problem?.title || 'Failed to load audit events.'
  }
  if (sequence === requestSequence) loading.value = false
}

async function loadDeepLink(sequence: number) {
  const id = queryText(route.query.entry)
  if (!id) {
    selectedEntry.value = null
    drawerOpen.value = false
    return
  }

  const response = await getEntry(id)
  if (sequence !== requestSequence) return
  if (response === null) {
    selectedEntry.value = null
    drawerOpen.value = false
    deepLinkNotFound.value = true
  } else if (response.status === 403) {
    forbiddenByApi.value = true
    selectedEntry.value = null
    drawerOpen.value = false
  } else if (response.data) {
    selectedEntry.value = response.data
    drawerOpen.value = true
  } else {
    loadProblem.value = response.problem
    loadError.value = response.problem?.detail || response.problem?.title || 'Failed to load the audit event.'
  }
}

async function applyFilters(next: AuditFilterState) {
  await replaceQuery({ ...next, page: 0, size: queryInteger(route.query.size, 20) })
}

async function clearFilters() {
  await replaceQuery({ actor: '', action: '', targetType: '', from: '', to: '', page: 0, size: 20 })
}

async function changePage(page: number) {
  await replaceQuery({ ...filters, page: page - 1, size: queryInteger(route.query.size, 20) })
}

async function openEntry(entry: AuditEvent) {
  await router.replace({ query: { ...route.query, entry: entry.id } })
}

async function closeEntry() {
  const { entry: _entry, ...query } = route.query
  await router.replace({ query })
}

function handleExportAuditLog() {
  const params = new URLSearchParams()

  for (const key of ['actor', 'action', 'targetType', 'from', 'to', 'page', 'size']) {
    const value = route.query[key]
    if (typeof value === 'string' && value) {
      params.set(key, value)
    }
  }

  const suffix = params.size > 0 ? `?${params.toString()}` : ''
  const link = document.createElement('a')
  link.href = `/api/audit/export.json${suffix}`
  link.click()
}

function handleDrawerOpen(open: boolean) {
  if (!open) closeEntry()
}

async function replaceQuery(next: AuditFilterState & { page: number; size: number }) {
  await router.replace({
    query: {
      ...(next.actor ? { actor: next.actor } : {}),
      ...(next.action ? { action: next.action } : {}),
      ...(next.targetType ? { targetType: next.targetType } : {}),
      ...(next.from ? { from: next.from } : {}),
      ...(next.to ? { to: next.to } : {}),
      page: String(next.page),
      size: String(next.size),
      ...(queryText(route.query.entry) ? { entry: queryText(route.query.entry) } : {}),
    },
  })
}

function readFiltersFromRoute(): AuditFilterState {
  return {
    actor: queryText(route.query.actor),
    action: queryText(route.query.action),
    targetType: queryText(route.query.targetType),
    from: queryText(route.query.from),
    to: queryText(route.query.to),
  }
}

function queryText(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function queryInteger(value: unknown, fallback: number): number {
  const parsed = typeof value === 'string' ? Number.parseInt(value, 10) : fallback
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback
}

watch([() => route.fullPath, canViewAuditLog], refreshFromRoute, { immediate: true })
onMounted(() => nextTick(() => heading.value?.focus()))
</script>
