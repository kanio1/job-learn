<template>
  <UCard data-testid="audit-filters">
    <template #header>
      <div class="flex items-center justify-between gap-3">
        <div>
          <h2 class="text-base font-semibold text-highlighted">Filters</h2>
          <p class="text-sm text-muted">Narrow the read-only audit view.</p>
        </div>
        <UButton
          v-if="hasFilters"
          color="neutral"
          variant="ghost"
          icon="i-lucide-x"
          label="Clear filters"
          data-testid="audit-filter-clear"
          @click="clearFilters"
        />
      </div>
    </template>

    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">
      <UFormField label="Actor">
        <UInput
          v-model="local.actor"
          class="w-full"
          placeholder="Actor display name"
          data-testid="audit-filter-actor"
          @keyup.enter="applyFilters"
        />
      </UFormField>

      <UFormField label="Action">
        <USelect
          v-model="local.action"
          :items="actionOptions"
          value-key="value"
          label-key="label"
          class="w-full"
          data-testid="audit-filter-action"
        />
      </UFormField>

      <UFormField label="Target type">
        <USelect
          v-model="local.targetType"
          :items="targetTypeOptions"
          value-key="value"
          label-key="label"
          class="w-full"
          data-testid="audit-filter-target-type"
        />
      </UFormField>

      <UFormField label="From date">
        <UInput
          v-model="local.from"
          type="date"
          class="w-full"
          data-testid="audit-filter-from"
        />
      </UFormField>

      <UFormField label="To date">
        <UInput
          v-model="local.to"
          type="date"
          class="w-full"
          data-testid="audit-filter-to"
        />
      </UFormField>
    </div>

    <template #footer>
      <div class="flex justify-end">
        <UButton icon="i-lucide-filter" label="Apply filters" @click="applyFilters" />
      </div>
    </template>
  </UCard>
</template>

<script setup lang="ts">
export interface AuditFilterState {
  actor: string
  action: string
  targetType: string
  from: string
  to: string
}

const props = defineProps<{
  modelValue: AuditFilterState
}>()

const emit = defineEmits<{
  apply: [filters: AuditFilterState]
  clear: []
}>()

function toLocal(state: AuditFilterState): AuditFilterState {
  return {
    ...state,
    action: state.action || 'all',
    targetType: state.targetType || 'all',
  }
}

function fromLocal(state: AuditFilterState): AuditFilterState {
  return {
    ...state,
    actor: state.actor.trim(),
    action: state.action === 'all' ? '' : state.action,
    targetType: state.targetType === 'all' ? '' : state.targetType,
  }
}

const local = reactive<AuditFilterState>(toLocal(props.modelValue))

const actionOptions = [
  { label: 'All actions', value: 'all' },
  { label: 'Merchant created', value: 'MERCHANT_CREATED' },
  { label: 'Merchant activated', value: 'MERCHANT_ACTIVATED' },
  { label: 'Merchant suspended', value: 'MERCHANT_SUSPENDED' },
  { label: 'Payment authorized', value: 'PAYMENT_AUTHORIZED' },
  { label: 'Payment captured', value: 'PAYMENT_CAPTURED' },
  { label: 'Payment cancelled', value: 'PAYMENT_CANCELLED' },
  { label: 'Payment refunded', value: 'PAYMENT_REFUNDED' },
  { label: 'User created', value: 'USER_CREATED' },
  { label: 'User updated', value: 'USER_UPDATED' },
  { label: 'User roles assigned', value: 'USER_ROLES_ASSIGNED' },
]

const targetTypeOptions = [
  { label: 'All target types', value: 'all' },
  { label: 'Merchant', value: 'MERCHANT' },
  { label: 'Payment order', value: 'PAYMENT_ORDER' },
  { label: 'User', value: 'USER' },
]

const hasFilters = computed(() => Boolean(
  local.actor.trim() || (local.action && local.action !== 'all')
  || (local.targetType && local.targetType !== 'all') || local.from || local.to
))

function applyFilters() {
  emit('apply', fromLocal(local))
}

function clearFilters() {
  Object.assign(local, { actor: '', action: 'all', targetType: 'all', from: '', to: '' })
  emit('clear')
}

watch(() => props.modelValue, value => Object.assign(local, toLocal(value)), { deep: true })
</script>
