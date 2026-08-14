<template>
  <UDashboardPanel id="rls-lab">
    <template #header>
      <UDashboardNavbar title="RLS Lab">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>
    <template #body>
      <div class="space-y-4 p-1 max-w-3xl">
        <UAlert
          icon="i-lucide-shield"
          color="info"
          variant="subtle"
          title="Java WHERE is not RLS"
          description="Merchants and payment_orders stay application-filtered. This lab uses dedicated tables, FORCE ROW LEVEL SECURITY, and a non-owner role. REST never returns the other tenant's row — 404, not a leak. Platform visibility uses a BYPASSRLS role, not a client-settable GUC."
        />

        <ProblemDetailsCard v-if="listProblem" :problem="listProblem" />

        <UCard data-testid="rls-lab-items-table">
          <h2 class="font-semibold mb-2">Visible rows for this JWT tenant</h2>
          <UTable
            :data="items"
            :columns="columns"
            class="shrink-0"
          />
          <p v-if="items.length === 0" class="text-sm text-muted mt-2">No rows visible under RLS.</p>
        </UCard>

        <UCard>
          <h2 class="font-semibold mb-2">Probe a UUID</h2>
          <p class="text-sm text-muted mb-2">
            Other-tenant seed id is hidden by RLS (404), not listed.
          </p>
          <div class="flex gap-2">
            <UInput v-model="probeId" data-testid="rls-lab-probe-id" />
            <UButton data-testid="rls-lab-probe" @click="probe">Load</UButton>
          </div>
          <ProblemDetailsCard v-if="problem" class="mt-3" :problem="problem" />
          <pre v-else-if="probeResult" data-testid="rls-lab-probe-result" class="text-xs mt-2">{{ probeResult }}</pre>
        </UCard>

        <UCard v-if="canCompare" data-testid="rls-lab-compare-panel">
          <h2 class="font-semibold mb-2">Platform leak compare</h2>
          <UButton data-testid="rls-lab-compare-load" @click="loadCompare">Load compare</UButton>
          <ProblemDetailsCard v-if="compareProblem" class="mt-3" :problem="compareProblem" />
          <dl v-else-if="compare" class="mt-3 grid grid-cols-3 gap-2 text-sm">
            <div>
              <dt class="text-muted">BYPASSRLS role</dt>
              <dd data-testid="rls-lab-compare-bypass-role">{{ compare.bypassRoleCount }}</dd>
            </div>
            <div>
              <dt class="text-muted">Restricted role, no tenant GUC</dt>
              <dd data-testid="rls-lab-compare-restricted-no-tenant">{{ compare.restrictedWithoutTenantGuc }}</dd>
            </div>
            <div>
              <dt class="text-muted">Unprotected table</dt>
              <dd data-testid="rls-lab-compare-unprotected">{{ compare.unprotected }}</dd>
            </div>
          </dl>
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
import { ofetch } from 'ofetch'
import type { TableColumn } from '@nuxt/ui'
import type { ProblemDetails } from '~/types/api'

definePageMeta({ layout: 'dashboard' })

const OTHER_ITEM = '00000000-0000-0000-0000-0000000000a2'

type RlsItem = { itemId: string, tenantId: string, label: string, amountMinor: number }
type RlsCompare = { bypassRoleCount: number, restrictedWithoutTenantGuc: number, unprotected: number }

const { can } = useAuthorization()
const canCompare = computed(() => can.value.canReadPlatformPayments)

const items = ref<RlsItem[]>([])
const probeId = ref(OTHER_ITEM)
const probeResult = ref('')
const problem = ref<ProblemDetails | null>(null)
const listProblem = ref<ProblemDetails | null>(null)
const compareProblem = ref<ProblemDetails | null>(null)
const compare = ref<RlsCompare | null>(null)

const columns: TableColumn<RlsItem>[] = [
  { accessorKey: 'label', header: 'Label' },
  { accessorKey: 'amountMinor', header: 'Amount' },
  { accessorKey: 'itemId', header: 'Id' },
]

function toProblem(error: unknown, fallbackTitle: string): ProblemDetails {
  const data = (error as { data?: ProblemDetails })?.data
  if (data && typeof data === 'object' && 'status' in data) {
    return data
  }
  return {
    type: 'about:blank',
    title: fallbackTitle,
    status: (error as { statusCode?: number })?.statusCode ?? 500,
    detail: String(error),
  }
}

onMounted(async () => {
  try {
    const body = await ofetch<{ items: RlsItem[] }>('/api/rls-lab/items')
    items.value = body.items ?? []
  }
  catch (error: unknown) {
    listProblem.value = toProblem(error, 'Failed to load RLS lab items')
  }
})

async function probe() {
  problem.value = null
  probeResult.value = ''
  try {
    const body = await ofetch<RlsItem>(`/api/rls-lab/items/${probeId.value}`)
    probeResult.value = JSON.stringify(body, null, 2)
  }
  catch (error: unknown) {
    problem.value = toProblem(error, 'Probe failed')
  }
}

async function loadCompare() {
  compareProblem.value = null
  try {
    compare.value = await ofetch<RlsCompare>('/api/rls-lab/compare')
  }
  catch (error: unknown) {
    compare.value = null
    compareProblem.value = toProblem(error, 'Compare failed')
  }
}
</script>
