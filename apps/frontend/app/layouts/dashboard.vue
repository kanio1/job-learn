<template>
  <UDashboardGroup unit="rem">
    <UDashboardSidebar
      id="default"
      v-model:open="open"
      collapsible
      resizable
      class="bg-elevated/25"
      :ui="{ footer: 'lg:border-t lg:border-default' }"
    >
      <template #header="{ collapsed }">
        <AppTeamsMenu :collapsed="collapsed" />
      </template>

      <template #default="{ collapsed }">
        <UDashboardSearchButton :collapsed="collapsed" label="Search..." class="bg-transparent ring-default" />

        <UNavigationMenu
          :collapsed="collapsed"
          :items="links[0]"
          orientation="vertical"
          tooltip
          popover
        />
      </template>

      <template #footer="{ collapsed }">
        <AppUserMenu :collapsed="collapsed" />
      </template>
    </UDashboardSidebar>

    <UDashboardSearch
      title="Search Payment Quality Lab"
      description="Quickly navigate to dashboard areas and merchant registry actions."
      placeholder="Search dashboard..."
      :groups="groups"
    />

    <slot />
  </UDashboardGroup>
</template>

<script setup lang="ts">
import type { NavigationMenuItem } from '@nuxt/ui'

const open = ref(false)

const links = [[
  {
    label: 'Overview',
    icon: 'i-lucide-layout-dashboard',
    to: '/',
    onSelect: () => {
      open.value = false
    }
  },
  {
    label: 'Merchants',
    icon: 'i-lucide-store',
    to: '/admin/merchants',
    onSelect: () => {
      open.value = false
    }
  },
  {
    label: 'Payment Orders',
    icon: 'i-lucide-receipt',
    to: '/admin/merchants',
    onSelect: () => {
      open.value = false
    }
  },
  {
    label: 'Error Lab',
    icon: 'i-lucide-flask-conical',
    to: '/error-lab',
    onSelect: () => {
      open.value = false
    }
  }
]] satisfies NavigationMenuItem[][]

const groups = computed(() => [{
  id: 'links',
  label: 'Go to',
  items: links.flat()
}, {
  id: 'actions',
  label: 'Actions',
  items: [{
    id: 'create-merchant',
    label: 'Create merchant',
    icon: 'i-lucide-plus',
    to: '/admin/merchants'
  }, {
    id: 'merchant-registry',
    label: 'Merchant registry',
    icon: 'i-lucide-store',
    to: '/admin/merchants'
  }, {
    id: 'payment-orders',
    label: 'Payment orders',
    icon: 'i-lucide-receipt',
    to: '/admin/merchants'
  }, {
    id: 'error-lab',
    label: 'Error Lab',
    icon: 'i-lucide-flask-conical',
    to: '/error-lab'
  }]
}])
</script>
