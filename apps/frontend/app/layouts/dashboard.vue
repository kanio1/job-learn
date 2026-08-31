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
        <div class="flex items-center gap-1">
          <AppTeamsMenu :collapsed="collapsed" />
          <NotificationCenter v-if="!collapsed" />
        </div>
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
        >
          <template #merchant-navigation-group>
            <UIcon name="i-lucide-store" data-slot="linkLeadingIcon" class="shrink-0 size-5" />
            <span data-slot="linkLabel" class="truncate">Merchants</span>
            <UIcon
              name="i-lucide-chevron-down"
              data-slot="linkTrailingIcon"
              class="size-5 transform shrink-0 transition-transform duration-200 group-data-[state=open]:rotate-180"
            />
          </template>
        </UNavigationMenu>
      </template>

      <template #footer="{ collapsed }">
        <AppUserMenu :collapsed="collapsed" />
      </template>
    </UDashboardSidebar>

    <UDashboardSearch
      v-model:search-term="searchTerm"
      :search-delay="0"
      :loading="searchLoading"
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
const searchTerm = ref('')
const searchLoading = ref(false)
const entityMerchants = ref<Array<{ merchantId: string, merchantReference: string, displayName: string }>>([])
const entityPayments = ref<Array<{ paymentOrderId: string, merchantId: string, clientOrderReference: string }>>([])
let searchSeq = 0
let searchAbort: AbortController | undefined
const { searchEntities } = useEntitySearchApi()
const { can } = useAuthorization()
const { user } = useUserSession()
const route = useRoute()
const checkoutLabEnabled = computed(() => useRuntimeConfig().public.checkoutLabEnabled === true)
const mirrorLabEnabled = computed(() => useRuntimeConfig().public.mirrorLabEnabled === true)
const rlsLabEnabled = computed(() => useRuntimeConfig().public.rlsLabEnabled === true)
const eventLabEnabled = computed(() => useRuntimeConfig().public.eventLabEnabled === true)
const tenantId = computed(() => (user.value as { tenantId?: string })?.tenantId)
const canReadPayments = computed(() =>
  can.value.canReadMerchantPayments || can.value.canReadPlatformPayments,
)
const canSearchEntities = computed(() => can.value.canReadMerchants || canReadPayments.value)

const MERCHANT_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

function closeSidebar() {
  open.value = false
}

function firstQueryString(value: unknown): string | undefined {
  if (typeof value === 'string' && value.length > 0) {
    return value
  }
  if (Array.isArray(value) && typeof value[0] === 'string' && value[0].length > 0) {
    return value[0]
  }
  return undefined
}

function scopedMerchantUuid(): string | undefined {
  const param = route.params.merchantId
  if (typeof param === 'string' && MERCHANT_UUID.test(param)) {
    return param
  }
  const fromQuery = firstQueryString(route.query.merchantId)
  if (fromQuery && MERCHANT_UUID.test(fromQuery)) {
    return fromQuery
  }
  const sessionId = (user.value as { merchantId?: string })?.merchantId
  if (sessionId && MERCHANT_UUID.test(sessionId)) {
    return sessionId
  }
  return undefined
}

const paymentOrdersTo = computed(() => {
  const id = scopedMerchantUuid()
  return id ? `/admin/merchants/${id}/payments` : undefined
})

const onPaymentOrdersRoute = computed(() =>
  /\/admin\/merchants\/[^/]+\/payments(?:\/|$)/.test(route.path),
)

const onMerchantsSection = computed(() =>
  route.path === '/admin/merchants'
  || (/^\/admin\/merchants\/[^/]+/.test(route.path) && !onPaymentOrdersRoute.value),
)

function paymentOrdersNavItem(): NavigationMenuItem {
  return {
    label: 'Payment Orders',
    icon: 'i-lucide-receipt',
    to: paymentOrdersTo.value,
    active: onPaymentOrdersRoute.value,
    disabled: !paymentOrdersTo.value,
    onSelect: closeSidebar,
    'data-testid': 'nav-link-payment-orders',
  }
}

/**
 * Sidebar follows the Nuxt UI Dashboard nested-nav pattern: unique `to` per
 * item, parent groups as `type: 'trigger'`, and explicit `active` so registry
 * and merchant-scoped payments are never both current.
 */
const visibleLinks = computed<NavigationMenuItem[]>(() => {
  const links: NavigationMenuItem[] = [
    {
      label: 'Overview',
      icon: 'i-lucide-layout-dashboard',
      to: '/',
      onSelect: closeSidebar,
      'data-testid': 'nav-link-overview',
    },
  ]

  if (can.value.canReadMerchants) {
    links.push({
      label: 'Merchants',
      icon: 'i-lucide-store',
      type: 'trigger',
      slot: 'merchant-navigation-group',
      defaultOpen: true,
      'data-testid': 'nav-group-merchants',
      children: [
        {
          label: 'Registry',
          icon: 'i-lucide-store',
          to: '/admin/merchants',
          active: onMerchantsSection.value,
          onSelect: closeSidebar,
          'data-testid': 'nav-link-merchants',
        },
        ...(canReadPayments.value ? [paymentOrdersNavItem()] : []),
      ],
    })
  }
  else if (canReadPayments.value) {
    links.push(paymentOrdersNavItem())
  }

  if (can.value.canManageUsers) {
    links.push({
      label: 'Users',
      icon: 'i-lucide-users',
      to: '/admin/users',
      onSelect: closeSidebar,
      'data-testid': 'nav-link-users',
    })
  }

  if (can.value.canViewAuditLog) {
    links.push({
      label: 'Audit Log',
      icon: 'i-lucide-scroll-text',
      to: '/admin/audit',
      onSelect: closeSidebar,
      'data-testid': 'nav-link-audit',
    })
  }

  if (can.value.canReadMerchants || can.value.canReadPlatformPayments) {
    links.push({
      label: 'Support',
      icon: 'i-lucide-headset',
      to: '/admin/support',
      onSelect: closeSidebar,
      'data-testid': 'nav-link-support',
    })
  }

  links.push({
    label: 'Error Lab',
    icon: 'i-lucide-flask-conical',
    to: '/error-lab',
    onSelect: closeSidebar,
    'data-testid': 'nav-link-error-lab',
  })

  if (checkoutLabEnabled.value) {
    links.push({
      label: 'Checkout Lab',
      icon: 'i-lucide-credit-card',
      to: '/admin/checkout-lab',
      onSelect: closeSidebar,
      'data-testid': 'nav-link-checkout-lab',
    })
  }

  if (mirrorLabEnabled.value) {
    links.push({
      label: 'Mirror Lab',
      icon: 'i-lucide-scan-eye',
      to: '/admin/mirror-lab',
      onSelect: closeSidebar,
      'data-testid': 'nav-link-mirror-lab',
    })
  }

  if (rlsLabEnabled.value) {
    links.push({
      label: 'RLS Lab',
      icon: 'i-lucide-shield',
      to: '/admin/rls-lab',
      onSelect: closeSidebar,
      'data-testid': 'nav-link-rls-lab',
    })
  }

  if (eventLabEnabled.value && can.value.canReadEventLab) {
    links.push({
      label: 'Event Lab',
      icon: 'i-lucide-activity',
      to: '/admin/event-lab',
      onSelect: closeSidebar,
      'data-testid': 'nav-link-event-lab',
    })
  }

  return links
})

type DashboardSearchGroup = {
  id: string
  label: string
  ignoreFilter?: boolean
  items: Array<{
    id: string
    label: string
    suffix?: string
    icon?: string
    to?: string
    'data-testid'?: string
  }>
}

function flattenNavForSearch(items: NavigationMenuItem[]): DashboardSearchGroup['items'] {
  return items.flatMap((link, index) => {
    const to = typeof link.to === 'string' ? link.to : undefined
    const testId = 'data-testid' in link ? String(link['data-testid']) : `nav-${index}`
    const self = to
      ? [{
          id: testId,
          label: String(link.label ?? ''),
          icon: typeof link.icon === 'string' ? link.icon : undefined,
          to,
        }]
      : []
    return [...self, ...flattenNavForSearch(link.children ?? [])]
  })
}

/**
 * UDashboardSearch groups — mirrors the visible links so search stays in sync
 * with role-aware navigation (Requirement 9.1 of iam-roles spec).
 */
watch(searchTerm, async (raw) => {
  const q = raw.trim().slice(0, 80)
  searchAbort?.abort()
  if (!q) {
    searchSeq += 1
    entityMerchants.value = []
    entityPayments.value = []
    searchLoading.value = false
    return
  }
  if (!canSearchEntities.value) {
    searchSeq += 1
    entityMerchants.value = []
    entityPayments.value = []
    searchLoading.value = false
    return
  }
  const seq = ++searchSeq
  searchAbort = new AbortController()
  const signal = searchAbort.signal
  searchLoading.value = true
  const response = await searchEntities(q, signal)
  if (seq !== searchSeq || response.status === 0) {
    return
  }
  entityMerchants.value = can.value.canReadMerchants ? (response.data?.merchants ?? []) : []
  entityPayments.value = canReadPayments.value ? (response.data?.payments ?? []) : []
  searchLoading.value = false
})

const searchGroups = computed<DashboardSearchGroup[]>(() => [
  ...(can.value.canReadMerchants && entityMerchants.value.length > 0
    ? [{
        id: 'merchants',
        label: 'Merchants',
        ignoreFilter: true,
        items: entityMerchants.value.map(merchant => ({
          id: `merchant:${merchant.merchantId}`,
          label: merchant.merchantReference,
          suffix: merchant.displayName,
          icon: 'i-lucide-store',
          to: `/admin/merchants?merchantId=${merchant.merchantId}`,
        })),
      }]
    : []),
  ...(canReadPayments.value && entityPayments.value.length > 0
    ? [{
        id: 'payments',
        label: 'Payments',
        ignoreFilter: true,
        items: entityPayments.value.map(payment => ({
          id: `payment:${payment.paymentOrderId}`,
          label: payment.clientOrderReference,
          suffix: payment.paymentOrderId,
          icon: 'i-lucide-receipt',
          to: `/admin/merchants/${payment.merchantId}/payments/${payment.paymentOrderId}`,
        })),
      }]
    : []),
  {
    id: 'links',
    label: 'Go to',
    items: flattenNavForSearch(visibleLinks.value),
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
      ...(paymentOrdersTo.value
        ? [{
            id: 'payment-orders',
            label: 'Payment orders',
            icon: 'i-lucide-receipt',
            to: paymentOrdersTo.value,
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
