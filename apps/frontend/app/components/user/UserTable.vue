<template>
  <UTable
    :data="users"
    :columns="columns"
    aria-label="User management"
    data-testid="users-table"
    class="shrink-0"
    :ui="{
      base: 'table-fixed border-separate border-spacing-0',
      thead: '[&>tr]:bg-elevated/50 [&>tr]:after:content-none',
      tbody: '[&>tr]:last:[&>td]:border-b-0',
      th: 'py-2 first:rounded-l-lg last:rounded-r-lg border-y border-default first:border-l last:border-r',
      td: 'border-b border-default align-top',
      separator: 'h-0'
    }"
  />
</template>

<script setup lang="ts">
import { h, resolveComponent } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import type { UserSummary } from '~/schemas/user.schema'

const props = defineProps<{
  users: UserSummary[]
  canAssignRoles: boolean
}>()

const emit = defineEmits<{
  edit: [user: UserSummary]
  roles: [user: UserSummary]
  toggleEnabled: [user: UserSummary]
}>()

const UBadge = resolveComponent('UBadge')
const UButton = resolveComponent('UButton')

const columns: TableColumn<UserSummary>[] = [
  {
    accessorKey: 'username',
    header: 'User',
    cell: ({ row }) => h('div', [
      h('p', { class: 'font-medium text-highlighted' }, row.original.username),
      h('p', { class: 'text-sm text-muted' }, row.original.email),
    ]),
  },
  {
    accessorKey: 'enabled',
    header: 'Status',
    cell: ({ row }) => h(UBadge, {
      color: row.original.enabled ? 'success' : 'neutral',
      variant: 'subtle',
    }, () => row.original.enabled ? 'Enabled' : 'Disabled'),
  },
  {
    accessorKey: 'tenantId',
    header: 'Tenant',
    cell: ({ row }) => h('span', { class: 'text-sm' }, row.original.tenantId || '—'),
  },
  {
    accessorKey: 'merchantId',
    header: 'Merchant',
    cell: ({ row }) => h('span', { class: 'text-sm text-muted' }, row.original.merchantId || '—'),
  },
  {
    accessorKey: 'roles',
    header: 'Roles',
    cell: ({ row }) => h('div', { class: 'flex flex-wrap gap-1' },
      row.original.roles.map(role => h(UBadge, {
        key: role,
        color: 'info',
        variant: 'subtle',
        size: 'sm',
      }, () => role.replaceAll('_', ' ')))
    ),
  },
  {
    id: 'actions',
    header: 'Actions',
    cell: ({ row }) => h('div', {
      class: 'flex flex-wrap gap-1',
      'data-user-id': row.original.id,
      'data-testid': `user-actions-${row.original.id}`,
    }, [
      h(UButton, {
        size: 'xs',
        color: 'neutral',
        variant: 'ghost',
        icon: 'i-lucide-pencil',
        label: 'Edit',
        'aria-label': `Edit ${row.original.username}`,
        onClick: () => emit('edit', row.original),
      }),
      h(UButton, {
        size: 'xs',
        color: 'neutral',
        variant: 'ghost',
        icon: 'i-lucide-power',
        label: row.original.enabled ? 'Disable' : 'Enable',
        'aria-label': `${row.original.enabled ? 'Disable' : 'Enable'} ${row.original.username}`,
        onClick: () => emit('toggleEnabled', row.original),
      }),
      ...(props.canAssignRoles ? [h(UButton, {
        size: 'xs',
        color: 'neutral',
        variant: 'ghost',
        icon: 'i-lucide-shield-check',
        label: 'Roles',
        'aria-label': `Manage roles for ${row.original.username}`,
        onClick: () => emit('roles', row.original),
      })] : []),
    ]),
  },
]
</script>
