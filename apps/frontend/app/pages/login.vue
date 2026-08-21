<template>
  <div
    data-testid="auth-required-surface"
    class="flex min-h-screen items-center justify-center"
  >
    <UCard class="w-full max-w-md">
      <template #header>
        <h1 class="text-xl font-semibold">
          Payment Quality Lab
        </h1>
      </template>

      <!-- Auth-failed message (shown on ?error=keycloak) -->
      <UAlert
        v-if="hasError"
        data-testid="auth-error-message"
        color="error"
        variant="soft"
        icon="i-lucide-alert-circle"
        title="Authentication failed"
        description="Sign-in was cancelled or an error occurred. Please try again."
        class="mb-4"
        role="alert"
      />

      <UAlert
        v-if="afterShallowLogout"
        data-testid="login-sso-resume-notice"
        color="warning"
        variant="soft"
        icon="i-lucide-info"
        title="Still signed in at Keycloak"
        description="The dashboard session is cleared, but Keycloak SSO may still be alive. Continue to Keycloak can skip the password form. Use a different account to force the login screen."
        class="mb-4"
        role="status"
      />

      <p
        v-else
        class="text-muted mb-4"
        role="status"
      >
        Sign in with your Keycloak account to continue.
      </p>

      <div class="flex flex-col gap-2">
        <UButton
          ref="loginButtonRef"
          data-testid="login-control"
          block
          :loading="pending"
          @click="signIn"
        >
          Continue to Keycloak
        </UButton>
        <UButton
          data-testid="login-different-account"
          block
          color="neutral"
          variant="outline"
          :loading="pendingDifferent"
          @click="signInDifferentAccount"
        >
          Use a different account
        </UButton>
      </div>
    </UCard>
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  layout: false,
})

const route = useRoute()

const hasError = computed(() => route.query.error === 'keycloak')
const afterShallowLogout = computed(() => route.query.logout === 'shallow')
const pending = ref(false)
const pendingDifferent = ref(false)

// Store the intended redirect destination in a cookie so the OIDC handler
// can send the user back there after login.
function captureRedirectTarget() {
  const redirectTo = route.query.redirectTo
  if (typeof redirectTo === 'string' && redirectTo.startsWith('/')) {
    useCookie('auth_redirect', { path: '/', maxAge: 300 }).value = redirectTo
  }
}

async function signIn() {
  pending.value = true
  captureRedirectTarget()
  await navigateTo('/auth/keycloak', { external: true })
}

async function signInDifferentAccount() {
  pendingDifferent.value = true
  captureRedirectTarget()
  await navigateTo('/auth/keycloak?prompt=login', { external: true })
}

// Move keyboard focus to the login button on mount so keyboard/AT users
// reach it immediately (Requirement 10.4 of iam-roles spec).
const loginButtonRef = useTemplateRef<{ $el: HTMLElement } | null>('loginButtonRef')

onMounted(() => {
  loginButtonRef.value?.$el?.focus()
})
</script>
