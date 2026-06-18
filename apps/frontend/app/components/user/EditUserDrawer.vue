<template>
  <USlideover
    v-model:open="open"
    :title="mode === 'roles' ? 'Manage user roles' : 'Edit user'"
    :description="user ? `User: ${user.username}` : 'User management'"
    :dismissible="!submitting"
  >
    <template #content>
      <div v-if="user" data-testid="edit-user-drawer" class="flex h-full flex-col">
        <div class="border-b border-default p-6">
          <h2 class="text-lg font-semibold text-highlighted">
            {{ mode === 'roles' ? 'Manage roles' : 'Edit user' }}
          </h2>
          <p class="mt-1 text-sm text-muted">{{ user.username }} · {{ user.tenantId }}</p>
        </div>

        <div class="flex-1 space-y-4 overflow-y-auto p-6">
          <UAlert
            v-if="conflictMessage"
            color="warning"
            variant="subtle"
            icon="i-lucide-triangle-alert"
            title="Username or email already exists"
            :description="conflictMessage"
            role="alert"
            data-testid="conflict-state"
          />
          <ErrorState
            v-else-if="problem || errorMessage"
            :problem="problem"
            :message="errorMessage || 'User update failed.'"
          />

          <UForm
            v-if="mode === 'details'"
            :schema="updateUserSchema"
            :state="editState"
            class="space-y-4"
            @submit="submitDetails"
          >
            <UFormField label="Email" name="email" required>
              <UInput v-model="editState.email" type="email" autocomplete="off" class="w-full" />
            </UFormField>

            <UFormField label="Account enabled" name="enabled">
              <USwitch v-model="editState.enabled" aria-label="Account enabled" />
            </UFormField>

            <UFormField label="Merchant reference (optional)" name="merchantId">
              <UInput v-model="merchantId" autocomplete="off" class="w-full" />
            </UFormField>

            <div class="flex justify-end gap-2 pt-2">
              <UButton color="neutral" variant="ghost" type="button" :disabled="submitting" @click="open = false">
                Cancel
              </UButton>
              <UButton type="submit" icon="i-lucide-save" :loading="submitting">
                Save changes
              </UButton>
            </div>
          </UForm>

          <UForm v-else :state="roleState" class="space-y-4" @submit="submitRoles">
            <UFormField label="Assigned roles" name="roles">
              <RoleAssignmentSelect v-model="roleState.roles" />
            </UFormField>

            <div class="flex justify-end gap-2 pt-2">
              <UButton color="neutral" variant="ghost" type="button" :disabled="submitting" @click="open = false">
                Cancel
              </UButton>
              <UButton type="submit" icon="i-lucide-shield-check" :loading="submitting">
                Save roles
              </UButton>
            </div>
          </UForm>
        </div>
      </div>
    </template>
  </USlideover>
</template>

<script setup lang="ts">
import type { FormSubmitEvent } from '@nuxt/ui'
import ErrorState from '~/components/shared/ErrorState.vue'
import RoleAssignmentSelect from '~/components/user/RoleAssignmentSelect.vue'
import { updateUserSchema, type CompositeRole, type UpdateUserInput, type UserDetail } from '~/schemas/user.schema'
import type { ProblemDetails } from '~/types/api'

const props = defineProps<{
  user: UserDetail | null
  mode: 'details' | 'roles'
}>()

const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{
  updated: [user: UserDetail]
}>()

const toast = useToast()
const { updateUser, assignUserRoles } = useUsersApi()
const submitting = ref(false)
const problem = ref<ProblemDetails | null>(null)
const errorMessage = ref<string | null>(null)
const conflictMessage = ref<string | null>(null)
const merchantId = ref('')
const editState = reactive({ email: '', enabled: true })
const roleState = reactive<{ roles: CompositeRole[] }>({ roles: [] })

function resetFromUser() {
  if (!props.user) return
  editState.email = props.user.email
  editState.enabled = props.user.enabled
  merchantId.value = props.user.merchantId ?? ''
  roleState.roles = [...props.user.roles]
  problem.value = null
  errorMessage.value = null
  conflictMessage.value = null
}

async function submitDetails(event: FormSubmitEvent<{ email?: string; enabled?: boolean }>) {
  if (!props.user) return
  submitting.value = true
  problem.value = null
  errorMessage.value = null
  conflictMessage.value = null

  const payload: UpdateUserInput = {
    email: event.data.email,
    enabled: event.data.enabled,
  }
  if ((props.user.merchantId ?? '') !== merchantId.value.trim()) {
    payload.attributes = { merchant_id: merchantId.value.trim() ? [merchantId.value.trim()] : [] }
  }

  try {
    const response = await updateUser(props.user.id, payload)
    handleResponse(response, 'User updated')
  } finally {
    submitting.value = false
  }
}

async function submitRoles() {
  if (!props.user) return
  submitting.value = true
  problem.value = null
  errorMessage.value = null
  conflictMessage.value = null

  const previous = new Set(props.user.roles)
  const selected = new Set(roleState.roles)

  try {
    const response = await assignUserRoles(props.user.id, {
      assign: roleState.roles.filter(role => !previous.has(role)),
      remove: props.user.roles.filter(role => !selected.has(role)),
    })
    handleResponse(response, 'User roles updated')
  } finally {
    submitting.value = false
  }
}

function handleResponse(response: Awaited<ReturnType<typeof updateUser>>, title: string) {
  if (response.data) {
    toast.add({ title, description: response.data.username, color: 'success' })
    emit('updated', response.data)
    open.value = false
  } else if (response.status === 409) {
    conflictMessage.value = response.problem?.detail || 'Use a different email.'
  } else {
    problem.value = response.problem
    errorMessage.value = response.problem?.detail
      || response.problem?.title
      || 'User update failed. Try again.'
  }
}

watch([open, () => props.user, () => props.mode], ([isOpen]) => {
  if (isOpen) resetFromUser()
})
</script>
