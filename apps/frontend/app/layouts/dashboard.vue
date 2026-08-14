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
        <TenantContextBadge v-if="!collapsed" :tenant-id="tenantId" />

        <UDashboardSearchButton
          :collapsed="collapsed"
          label="Search..."
          class="bg-transparent ring-default"
        />

        <UNavigationMenu
          :collapsed="collapsed"
          :items="visibleLinks"
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
      :groups="searchGroups"
    />

    <slot />
    <SessionLabIdleLock />
  </UDashboardGroup>
</template>

<script setup lang="ts">
import type { NavigationMenuItem } from '@nuxt/ui'

const open = ref(false)
const { can } = useAuthorization()
const { user } = useUserSession()
const checkoutLabEnabled = computed(() => useRuntimeConfig().public.checkoutLabEnabled === true)
const mirrorLabEnabled = computed(() => useRuntimeConfig().public.mirrorLabEnabled === true)
const rlsLabEnabled = computed(() => useRuntimeConfig().public.rlsLabEnabled === true)
const tenantId = computed(() => (user.value as { tenantId?: string })?.tenantId)

function closeSidebar() {
  open.value = false
}

/**
 * All possible sidebar links.  Each carries a stable data-testid so future
 * Playwright tests can assert visibility per role.
 */
const allLinks: (NavigationMenuItem & { testid: string; visible: ComputedRef<boolean> })[] = [
  {
    testid: 'nav-link-overview',
    label: 'Overview',
    icon: 'i-lucide-layout-dashboard',
    to: '/',
    onSelect: closeSidebar,
    // Overview is always visible to authenticated users
    visible: computed(() => true),
  },
  {
    testid: 'nav-link-merchants',
    label: 'Merchants',
    icon: 'i-lucide-store',
    to: '/admin/merchants',
    onSelect: closeSidebar,
    visible: computed(() => can.value.canReadMerchants),
  },
  {
    testid: 'nav-link-payment-orders',
    label: 'Payment Orders',
    icon: 'i-lucide-receipt',
    to: '/admin/merchants',
    onSelect: closeSidebar,
    visible: computed(() => can.value.canReadMerchants || can.value.canReadPlatformPayments),
  },
  {
    testid: 'nav-link-users',
    label: 'Users',
    icon: 'i-lucide-users',
    to: '/admin/users',
    onSelect: closeSidebar,
    visible: computed(() => can.value.canManageUsers),
  },
  {
    testid: 'nav-link-audit',
    label: 'Audit Log',
    icon: 'i-lucide-scroll-text',
    to: '/admin/audit',
    onSelect: closeSidebar,
    visible: computed(() => can.value.canViewAuditLog),
  },
  {
    testid: 'nav-link-support',
    label: 'Support',
    icon: 'i-lucide-headset',
    to: '/admin/support',
    onSelect: closeSidebar,
    visible: computed(() => can.value.canReadMerchants || can.value.canReadPlatformPayments),
  },
  {
    testid: 'nav-link-error-lab',
    label: 'Error Lab',
    icon: 'i-lucide-flask-conical',
    to: '/error-lab',
    onSelect: closeSidebar,
    // Error Lab is always visible — it is a learning surface, not a role-gated screen
    visible: computed(() => true),
  },
  {
    testid: 'nav-link-checkout-lab',
    label: 'Checkout Lab',
    icon: 'i-lucide-credit-card',
    to: '/admin/checkout-lab',
    onSelect: closeSidebar,
    visible: computed(() => checkoutLabEnabled.value),
  },
  {
    testid: 'nav-link-mirror-lab',
    label: 'Mirror Lab',
    icon: 'i-lucide-scan-eye',
    to: '/admin/mirror-lab',
    onSelect: closeSidebar,
    visible: computed(() => mirrorLabEnabled.value),
  },
  {
    testid: 'nav-link-rls-lab',
    label: 'RLS Lab',
    icon: 'i-lucide-shield',
    to: '/admin/rls-lab',
    onSelect: closeSidebar,
    visible: computed(() => rlsLabEnabled.value),
  },
]

/**
 * Filtered navigation items based on the current session's capabilities.
 * Omit the testid/visible fields from the final items passed to UNavigationMenu.
 */
const visibleLinks = computed<NavigationMenuItem[]>(() =>
  allLinks
    .filter(link => link.visible.value)
    .map(({ testid, visible: _v, ...item }) => ({
      ...item,
      // Carry the testid forward via an arbitrary prop — UNavigationMenu passes it
      // through to the rendered element so Playwright can use getByTestId.
      'data-testid': testid,
    }))
)

/**
 * UDashboardSearch groups — mirrors the visible links so search stays in sync
 * with role-aware navigation (Requirement 9.1 of iam-roles spec).
 */
const searchGroups = computed<any[]>(() => [
  {
    id: 'links',
    label: 'Go to',
    items: visibleLinks.value,
  },
  {
    id: 'actions',
    label: 'Actions',
    items: [
      ...(can.value.canCreateMerchant
        ? [{
            id: 'create-merchant',
            label: 'Create merchant',
            icon: 'i-lucide-plus',
            to: '/admin/merchants',
          }]
        : []),
      ...(can.value.canReadMerchants
        ? [{
            id: 'merchant-registry',
            label: 'Merchant registry',
            icon: 'i-lucide-store',
            to: '/admin/merchants',
          }]
        : []),
      ...(can.value.canReadMerchants || can.value.canReadPlatformPayments
        ? [{
            id: 'payment-orders',
            label: 'Payment orders',
            icon: 'i-lucide-receipt',
            to: '/admin/merchants',
          }]
        : []),
      ...(can.value.canManageUsers
        ? [{
            id: 'user-management',
            label: 'User management',
            icon: 'i-lucide-users',
            to: '/admin/users',
          }]
        : []),
      ...(can.value.canViewAuditLog
        ? [{
            id: 'audit-log',
            label: 'Audit log',
            icon: 'i-lucide-scroll-text',
            to: '/admin/audit',
            'data-testid': 'search-link-audit',
          }]
        : []),
      {
        id: 'error-lab',
        label: 'Error Lab',
        icon: 'i-lucide-flask-conical',
        to: '/error-lab',
      },
      ...(checkoutLabEnabled.value
        ? [
            {
              id: 'checkout-lab',
              label: 'Checkout Lab',
              icon: 'i-lucide-credit-card',
              to: '/admin/checkout-lab',
              'data-testid': 'search-link-checkout-lab',
            },
            {
              id: 'checkout-lab-booking',
              label: 'Booking Lab',
              icon: 'i-lucide-calendar',
              to: '/admin/checkout-lab/booking',
              'data-testid': 'search-link-checkout-lab-booking',
            },
            {
              id: 'checkout-lab-inspector',
              label: 'Event Inspector',
              icon: 'i-lucide-search',
              to: '/admin/checkout-lab/inspector',
              'data-testid': 'search-link-checkout-lab-inspector',
            },
          ]
        : []),
      ...(mirrorLabEnabled.value
        ? [
            {
              id: 'mirror-lab',
              label: 'Mirror Lab',
              icon: 'i-lucide-scan-eye',
              to: '/admin/mirror-lab',
              'data-testid': 'search-link-mirror-lab',
            },
          ]
        : []),
    ],
  },
])
</script>
