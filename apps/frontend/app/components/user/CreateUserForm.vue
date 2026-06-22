<template>
  <UButton
    icon="i-lucide-user-plus"
    label="Create user"
    aria-label="Create user"
    data-testid="create-user-button"
    @click="open = true"
  />

  <UModal v-model:open="open" title="Create user" description="Create an enabled Keycloak user with an initial role set.">
    <template #content>
      <div class="p-6">
        <div class="mb-6">
          <h2 class="text-lg font-semibold text-highlighted">Create user</h2>
          <p class="mt-1 text-sm text-muted">
            The temporary password is sent once and cleared immediately after submission.
          </p>
        </div>

        <UAlert
          v-if="conflictMessage"
          color="warning"
          variant="subtle"
          icon="i-lucide-triangle-alert"
          title="Username or email already exists"
          :description="conflictMessage"
          role="alert"
          data-testid="conflict-state"
          class="mb-4"
        />

        <ErrorState
          v-else-if="problem || errorMessage"
          :problem="problem"
          :message="errorMessage || 'Failed to create user.'"
          class="mb-4"
        />

        <UForm
          :schema="createFormSchema"
          :state="state"
          data-testid="create-user-form"
          class="space-y-4"
          @submit="onSubmit"
        >
          <UFormField label="Username" name="username" required>
            <UInput v-model="state.username" autocomplete="off" class="w-full" />
          </UFormField>

          <UFormField label="Email" name="email" required>
            <UInput v-model="state.email" type="email" autocomplete="off" class="w-full" />
          </UFormField>

          <UFormField label="Temporary password" name="temporaryPassword" required>
            <UInput
              v-model="state.temporaryPassword"
              type="password"
              autocomplete="new-password"
              class="w-full"
            />
          </UFormField>

          <UFormField v-if="isPlatformAdmin" label="Tenant reference" name="tenantId" required>
            <UInput v-model="state.tenantId" autocomplete="off" class="w-full" />
          </UFormField>

          <UFormField label="Merchant reference (optional)" name="merchantId">
            <UInput v-model="state.merchantId" autocomplete="off" class="w-full" />
          </UFormField>

          <UFormField label="Initial roles" name="roles" required>
            <RoleAssignmentSelect v-model="state.roles" />
          </UFormField>

          <div class="flex justify-end gap-2 pt-2">
            <UButton color="neutral" variant="ghost" type="button" :disabled="submitting" @click="close">
              Cancel
            </UButton>
            <UButton type="submit" icon="i-lucide-user-plus" :loading="submitting">
              Create user
            </UButton>
          </div>
        </UForm>
      </div>
    </template>
  </UModal>
</template>

<script setup lang="ts">
import type { FormSubmitEvent } from '@nuxt/ui'
import ErrorState from '~/components/shared/ErrorState.vue'
import RoleAssignmentSelect from '~/components/user/RoleAssignmentSelect.vue'
import { createUserSchema, type CreateUserInput, type CompositeRole, type UserDetail } from '~/schemas/user.schema'
import type { ProblemDetails } from '~/types/api'

const props = defineProps<{
  isPlatformAdmin: boolean
}>()

const emit = defineEmits<{
  created: [user: UserDetail]
}>()

const toast = useToast()
const { createUser } = useUsersApi()
const open = ref(false)
const submitting = ref(false)
const problem = ref<ProblemDetails | null>(null)
const errorMessage = ref<string | null>(null)
const conflictMessage = ref<string | null>(null)

const createFormSchema = computed(() => props.isPlatformAdmin
  ? createUserSchema.refine(
      value => Boolean(value.tenantId?.trim()),
      { path: ['tenantId'], message: 'Tenant reference is required' }
    )
  : createUserSchema)

interface CreateState {
  username: string
  email: string
  temporaryPassword: string
  tenantId?: string
  merchantId?: string
  roles: CompositeRole[]
}

const state = reactive<CreateState>(emptyState())

function emptyState(): CreateState {
  return {
    username: '',
    email: '',
    temporaryPassword: '',
    tenantId: undefined,
    merchantId: undefined,
    roles: [],
  }
}

async function onSubmit(event: FormSubmitEvent<CreateUserInput>) {
  problem.value = null
  errorMessage.value = null
  conflictMessage.value = null
  submitting.value = true

  const payload: CreateUserInput = {
    ...event.data,
    tenantId: props.isPlatformAdmin ? event.data.tenantId : undefined,
  }
  state.temporaryPassword = ''

  try {
    const response = await createUser(payload)
    if (response.data) {
      toast.add({ title: 'User created', description: response.data.username, color: 'success' })
      emit('created', response.data)
      resetAndClose()
    } else if (response.status === 409) {
      conflictMessage.value = response.problem?.detail || 'Use a different username or email.'
    } else {
      problem.value = response.problem
      errorMessage.value = response.problem?.detail
        || response.problem?.title
        || 'Failed to create user. Try again.'
    }
  } finally {
    submitting.value = false
  }
}

function close() {
  state.temporaryPassword = ''
  open.value = false
  problem.value = null
  errorMessage.value = null
  conflictMessage.value = null
}

function resetAndClose() {
  Object.assign(state, emptyState())
  close()
}

watch(open, (isOpen) => {
  if (!isOpen) state.temporaryPassword = ''
})

defineExpose({
  openModal: () => { open.value = true },
})
</script>
