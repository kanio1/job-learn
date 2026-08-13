<template>
  <div
    v-if="locked"
    data-testid="session-lab-idle-lock"
    class="fixed inset-0 z-[80] flex items-center justify-center bg-black/70 p-6"
  >
    <UCard class="max-w-md">
      <h2 class="font-semibold mb-2">Session locked</h2>
      <p class="text-sm text-muted mb-4">
        Idle for {{ idleSeconds }} seconds on the dashboard. Hosted checkout uses layout false, so this overlay never appears there.
      </p>
      <UButton data-testid="session-lab-idle-unlock" @click="unlock">
        Unlock
      </UButton>
    </UCard>
  </div>
</template>

<script setup lang="ts">
const { locked, idleSeconds } = useIdleLock()
const auth = useAuthStore()

async function unlock() {
  await auth.logout()
}
</script>
