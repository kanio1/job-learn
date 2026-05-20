<template>
  <div class="flex min-h-screen items-center justify-center">
    <UCard class="w-full max-w-md">
      <template #header>
        <h1 class="text-xl font-semibold">Payment Quality Lab</h1>
      </template>
      <p class="text-muted mb-4" role="status">Redirecting to Keycloak sign-in...</p>
      <UButton block @click="signIn">
        Continue to Keycloak
      </UButton>
    </UCard>
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  layout: false
})

async function signIn() {
  await navigateTo('/auth/keycloak', { external: true })
}

onMounted(() => {
  signIn()
})
</script>
