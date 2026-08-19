<template>
  <div v-if="tenantRef">
    <UAlert
      v-if="isSuspended"
      data-testid="tenant-suspended-banner"
      color="error"
      variant="subtle"
      icon="i-lucide-ban"
      title="Tenant suspended"
      description="This tenant account is suspended. Payment operations are unavailable."
      class="mx-4 my-2"
    />
    <div v-else class="px-4 py-1">
      <UBadge
        data-testid="tenant-context-badge"
        color="neutral"
        variant="subtle"
        size="sm"
      >
        {{ displayName }}
      </UBadge>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  tenantId?: string
}>()

const SUSPENDED_REFERENCES = ['PLACEHOLDER_TENANT_ID']

const tenantRef = computed(() => props.tenantId ?? null)

const isSuspended = computed(() => tenantRef.value !== null && SUSPENDED_REFERENCES.includes(tenantRef.value))

const displayName = computed(() => {
  if (!tenantRef.value) return ''
  const map = {
    PLATFORM_TENANT: 'Platform',
    TENANT_ALPHA: 'Alpha Tenant',
    PLACEHOLDER_TENANT_ID: 'Suspended Demo Tenant',
  } as const
  const key = tenantRef.value
  if (key === 'PLATFORM_TENANT' || key === 'TENANT_ALPHA' || key === 'PLACEHOLDER_TENANT_ID') {
    return map[key]
  }
  return key
})
</script>
