<template>
  <nav aria-label="Organization tree" class="min-w-56">
    <p class="mb-2 text-sm font-medium text-highlighted">Organization</p>
    <ul
      v-if="roots.length > 0"
      data-testid="org-tree"
      role="tree"
      aria-label="Organization"
      class="space-y-0.5 text-sm"
    >
      <li v-for="tenant in roots" :key="tenant.id" role="none">
        <div
          role="treeitem"
          :aria-expanded="isExpanded(tenant.id) ? 'true' : 'false'"
          aria-level="1"
          tabindex="0"
          class="cursor-pointer rounded-md px-2 py-1 hover:bg-elevated focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          @click="toggle(tenant)"
          @keydown="onTenantKeydown($event, tenant)"
        >
          {{ tenant.label }}
        </div>
        <ul v-if="isExpanded(tenant.id)" role="group" class="ms-4 mt-0.5 space-y-0.5">
          <li v-for="child in childrenOf(tenant.id)" :key="child.id" role="none">
            <div
              role="treeitem"
              aria-level="2"
              tabindex="0"
              class="rounded-md px-2 py-1 text-muted"
            >
              {{ child.label }}
            </div>
          </li>
        </ul>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import type { OrgTreeNode } from '~/schemas/org-tree.schema'

const props = defineProps<{
  treeSlug?: string
}>()

const { getOrgTree } = useOrgTreeApi()

const roots = ref<OrgTreeNode[]>([])
const childrenByParent = ref<Record<string, OrgTreeNode[]>>({})
const expanded = ref<Set<string>>(new Set())

function isExpanded(id: string): boolean {
  return expanded.value.has(id)
}

function childrenOf(id: string): OrgTreeNode[] {
  return childrenByParent.value[id] ?? []
}

function treeSlugOf(reference: string): string {
  return reference.toLowerCase().replaceAll('_', '-')
}

async function loadRoots(): Promise<void> {
  const response = await getOrgTree()
  roots.value = response.data?.nodes ?? []
}

async function expand(tenant: OrgTreeNode): Promise<void> {
  if (!childrenByParent.value[tenant.id]) {
    const response = await getOrgTree(tenant.id)
    childrenByParent.value = {
      ...childrenByParent.value,
      [tenant.id]: response.data?.nodes ?? [],
    }
  }
  const next = new Set(expanded.value)
  next.add(tenant.id)
  expanded.value = next
}

function collapse(tenant: OrgTreeNode): void {
  const next = new Set(expanded.value)
  next.delete(tenant.id)
  expanded.value = next
}

async function toggle(tenant: OrgTreeNode): Promise<void> {
  if (isExpanded(tenant.id)) {
    collapse(tenant)
    return
  }
  await expand(tenant)
}

async function onTenantKeydown(event: KeyboardEvent, tenant: OrgTreeNode): Promise<void> {
  if (event.key === 'ArrowRight' && !isExpanded(tenant.id)) {
    event.preventDefault()
    await expand(tenant)
    return
  }
  if (event.key === 'ArrowLeft' && isExpanded(tenant.id)) {
    event.preventDefault()
    collapse(tenant)
  }
}

async function applyDeepLink(): Promise<void> {
  const slug = props.treeSlug
  if (!slug) {
    return
  }
  const tenant = roots.value.find(node => treeSlugOf(node.reference) === slug)
  if (tenant) {
    await expand(tenant)
  }
}

onMounted(async () => {
  await loadRoots()
  await applyDeepLink()
})

watch(() => props.treeSlug, () => {
  void applyDeepLink()
})
</script>
