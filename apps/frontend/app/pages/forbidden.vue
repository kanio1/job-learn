<template>
  <div
    data-testid="forbidden-page"
    class="flex min-h-screen items-center justify-center"
  >
    <UCard class="w-full max-w-md text-center">
      <template #header>
        <h1 ref="heading" tabindex="-1" class="text-xl font-semibold">
          Access Denied
        </h1>
      </template>

      <div class="space-y-4">
        <UIcon
          name="i-lucide-lock"
          class="mx-auto size-12 text-red-500"
          aria-hidden="true"
        />

        <p class="text-muted">
          You are signed in but do not have permission to access this page.
          This is not a sign-in issue — your account does not have the required role.
        </p>

        <p class="text-sm text-muted">
          Signed in as
          <span class="font-medium text-default">{{ username }}</span>
          <span v-if="roles.length" class="ml-1">({{ roles.join(', ') }})</span>
        </p>
      </div>

      <template #footer>
        <UButton
          data-testid="forbidden-home-link"
          to="/"
          icon="i-lucide-layout-dashboard"
          variant="outline"
          block
        >
          Go to Overview
        </UButton>
      </template>
    </UCard>
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  layout: false,
})

const { user } = useUserSession()
const username = computed(() => (user.value as { username?: string })?.username ?? 'Unknown')
const roles = computed<string[]>(() => {
  const r = (user.value as { roles?: unknown })?.roles
  return Array.isArray(r) ? (r as string[]) : []
})

const heading = ref<HTMLElement | null>(null)

onMounted(() => {
  heading.value?.focus()
})
</script>
