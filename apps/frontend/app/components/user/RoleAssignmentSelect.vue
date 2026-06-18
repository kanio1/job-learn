<template>
  <USelectMenu
    v-model="model"
    :items="roleItems"
    value-key="value"
    label-key="label"
    multiple
    :search-input="{ placeholder: 'Search roles…' }"
    placeholder="Select roles"
    aria-label="Assigned roles"
    data-testid="role-assignment-select"
    class="w-full"
  />
</template>

<script setup lang="ts">
import { COMPOSITE_ROLES } from '~/utils/rbacMatrix'
import type { CompositeRole } from '~/schemas/user.schema'

const model = defineModel<CompositeRole[]>({ required: true })

const roleItems = COMPOSITE_ROLES.map(role => ({
  label: role.replaceAll('_', ' '),
  value: role,
}))
</script>
