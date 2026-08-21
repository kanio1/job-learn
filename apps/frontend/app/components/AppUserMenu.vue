<script setup lang="ts">
import type { DropdownMenuItem } from '@nuxt/ui'

defineProps<{
  collapsed?: boolean
}>()

const auth = useAuthStore()
const colorMode = useColorMode()

const user = computed(() => ({
  name: auth.user?.username ?? 'Operator',
  avatar: {
    icon: 'i-lucide-user-round'
  }
}))

const items = computed<DropdownMenuItem[][]>(() => [[{
  type: 'label',
  label: user.value.name,
  avatar: user.value.avatar
}], [{
  label: 'Appearance',
  icon: 'i-lucide-sun-moon',
  children: [{
    label: 'Light',
    icon: 'i-lucide-sun',
    type: 'checkbox',
    checked: colorMode.value === 'light',
    onSelect(event: Event) {
      event.preventDefault()
      colorMode.preference = 'light'
    }
  }, {
    label: 'Dark',
    icon: 'i-lucide-moon',
    type: 'checkbox',
    checked: colorMode.value === 'dark',
    onSelect(event: Event) {
      event.preventDefault()
      colorMode.preference = 'dark'
    }
  }]
}], [{
  label: 'Sign out',
  icon: 'i-lucide-log-out',
  onSelect: () => auth.logout()
}, {
  label: 'Sign out of dashboard only',
  icon: 'i-lucide-monitor-off',
  onSelect: () => auth.logoutShallow()
}]])
</script>

<template>
  <UDropdownMenu
    :items="items"
    :content="{ align: 'center', collisionPadding: 12 }"
    :ui="{ content: collapsed ? 'w-48' : 'w-(--reka-dropdown-menu-trigger-width)' }"
  >
    <UButton
      v-bind="{
        ...user,
        label: collapsed ? undefined : user.name,
        trailingIcon: collapsed ? undefined : 'i-lucide-chevrons-up-down'
      }"
      data-testid="logout-control"
      color="neutral"
      variant="ghost"
      block
      :square="collapsed"
      class="data-[state=open]:bg-elevated"
      :ui="{
        trailingIcon: 'text-dimmed'
      }"
    />
  </UDropdownMenu>
</template>
