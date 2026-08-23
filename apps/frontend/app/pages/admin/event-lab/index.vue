<template>
  <UDashboardPanel id="event-lab">
    <template #header>
      <UDashboardNavbar title="Event Lab" :ui="{ right: 'gap-3' }">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>
    <template #body>
      <UContainer>
        <div v-if="loading" data-testid="event-lab-loading">Loading…</div>
        <div v-else-if="forbidden" data-testid="event-lab-forbidden">
          <UAlert title="Forbidden" description="Missing platform:event-lab:read" color="error" />
        </div>
        <div v-else-if="error" data-testid="event-lab-error">
          <UAlert title="Error" :description="error" color="error" />
        </div>
        <div v-else>
          <UAlert v-if="hasDead" data-testid="event-lab-dlt-banner" title="Dead-letter topic has records" color="warning" description="Check lab.event-lab.dlq.v1 in Lenses" />
          <UInput v-model="query" placeholder="Search paymentOrderId or eventId" data-testid="event-lab-search" />
          <div v-if="filtered.length === 0 && query" data-testid="event-lab-filtered-empty">No results for filter</div>
          <div v-else-if="filtered.length === 0" data-testid="event-lab-empty">No events yet — authorize a payment to see proof-of-delivery</div>
          <UTable v-else :data="filtered" :columns="columns" data-testid="event-lab-table">
            <template #status-cell="{ row }">
              <UBadge :label="row.original.status" :color="row.original.status==='DEAD' ? 'error' : 'success'" />
            </template>
          </UTable>
          <div class="mt-4 flex gap-2">
            <UButton label="Inject duplicate" data-testid="event-lab-inject-duplicate" @click="showDup=true" />
            <UButton label="Inject poison" data-testid="event-lab-inject-poison" color="error" @click="showPoison=true" />
          </div>
          <UModal v-model:open="showDup">
            <template #content>
              <UCard>
                <p>Inject duplicate will replay the first eventId — still 1 row.</p>
                <div class="flex gap-2 mt-4">
                  <UButton label="Confirm" data-testid="confirm-inject-duplicate" @click="doDuplicate" />
                  <UButton label="Cancel" variant="ghost" @click="showDup=false" />
                </div>
              </UCard>
            </template>
          </UModal>
          <UModal v-model:open="showPoison">
            <template #content>
              <UCard>
                <p>Poison will mark the record as DEAD and show DLT banner. Payment status stays unchanged.</p>
                <div class="flex gap-2 mt-4">
                  <UButton label="Confirm" color="error" data-testid="confirm-inject-poison" @click="doPoison" />
                  <UButton label="Cancel" variant="ghost" @click="showPoison=false" />
                </div>
              </UCard>
            </template>
          </UModal>
          <div v-if="notFound" data-testid="event-lab-not-found">Record not found</div>
        </div>
      </UContainer>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
const { list, injectDuplicate, injectPoison } = useEventLabApi()
const { can } = useAuthorization()
const query = ref('')
const loading = ref(true)
const forbidden = ref(false)
const error = ref<string|null>(null)
const notFound = ref(false)
const rows = ref<import('~/schemas/event-lab.schema').EventLabRecord[]>([])
const showDup = ref(false)
const showPoison = ref(false)

const columns = [
  { accessorKey: 'consumedAt', header: 'Time' },
  { accessorKey: 'action', header: 'Action' },
  { accessorKey: 'targetId', header: 'Target' },
  { accessorKey: 'status', header: 'Status' },
  { accessorKey: 'consumerGroup', header: 'Group' },
]

const filtered = computed(() => {
  if (!query.value) return rows.value
  return rows.value.filter(r => r.targetId.includes(query.value) || r.eventId.includes(query.value))
})
const hasDead = computed(() => rows.value.some(r => r.status === 'DEAD'))

async function refresh() {
  loading.value = true
  error.value = null
  forbidden.value = false
  notFound.value = false
  const res = await list()
  if (res.status === 403) { forbidden.value = true; loading.value = false; return }
  if (res.status >= 400) { error.value = res.problem?.detail || 'Failed to load'; loading.value = false; return }
  rows.value = res.data ?? []
  loading.value = false
}

async function doDuplicate() {
  if (!rows.value[0]) return
  await injectDuplicate(rows.value[0].eventId)
  showDup.value = false
  await refresh()
}
async function doPoison() {
  if (!rows.value[0]) return
  await injectPoison(rows.value[0].eventId)
  showPoison.value = false
  await refresh()
}

onMounted(()=> { refresh() })

// handle deep-link not-found via query id param
const route = useRoute()
watchEffect(()=> {
  if (route.query.id && typeof route.query.id === 'string') {
    const found = rows.value.find(r=> r.id===route.query.id)
    if (!found && !loading.value) notFound.value = true
  }
})

definePageMeta({ middleware: [] })
</script>
