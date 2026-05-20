<script setup lang="ts">
import type { DropdownMenuItem } from '@nuxt/ui'

defineProps<{
  collapsed?: boolean
}>()

const selectedTeam = ref({
  label: 'Payment Quality',
  avatar: {
    icon: 'i-lucide-landmark'
  }
})

const items = computed<DropdownMenuItem[][]>(() => [[{
  label: 'Payment Quality Lab',
  avatar: selectedTeam.value.avatar,
  type: 'label'
}], [{
  label: 'Merchant Registry',
  icon: 'i-lucide-store',
  to: '/admin/merchants'
}, {
  label: 'Phase 1 Scope',
  icon: 'i-lucide-clipboard-check',
  disabled: true
}]])
</script>

<template>
  <UDropdownMenu
    :items="items"
    :content="{ align: 'center', collisionPadding: 12 }"
    :ui="{ content: collapsed ? 'w-40' : 'w-(--reka-dropdown-menu-trigger-width)' }"
  >
    <UButton
      v-bind="{
        ...selectedTeam,
        label: collapsed ? undefined : selectedTeam.label,
        trailingIcon: collapsed ? undefined : 'i-lucide-chevrons-up-down'
      }"
      color="neutral"
      variant="ghost"
      block
      :square="collapsed"
      class="data-[state=open]:bg-elevated"
      :class="[!collapsed && 'py-2']"
      :ui="{
        trailingIcon: 'text-dimmed'
      }"
    />
  </UDropdownMenu>
</template>
