<template>
  <div
    data-testid="psp-redirect-simulator"
    class="flex min-h-screen items-center justify-center bg-gray-50 dark:bg-gray-950"
  >
    <UCard class="w-full max-w-md">
      <template #header>
        <div class="flex items-center gap-2">
          <UIcon name="i-lucide-external-link" class="text-gray-400" />
          <span class="text-sm font-semibold text-gray-500 dark:text-gray-400">
            Mock Payment Provider
          </span>
        </div>
      </template>

      <div v-if="outcome === null" class="space-y-4">
        <p class="text-sm text-gray-600 dark:text-gray-400">
          This is a standalone simulator of an external PSP checkout redirect —
          it demonstrates the multi-tab handoff pattern only. No card details,
          no real payment provider, no network call.
        </p>

        <div class="flex gap-2">
          <UButton
            data-testid="psp-approve"
            color="success"
            class="flex-1 justify-center"
            @click="outcome = 'approved'"
          >
            Approve
          </UButton>
          <UButton
            data-testid="psp-decline"
            color="error"
            variant="outline"
            class="flex-1 justify-center"
            @click="outcome = 'declined'"
          >
            Decline
          </UButton>
        </div>
      </div>

      <div v-else data-testid="psp-outcome" class="space-y-3">
        <UAlert
          :color="outcome === 'approved' ? 'success' : 'error'"
          variant="subtle"
          :icon="outcome === 'approved' ? 'i-lucide-check-circle' : 'i-lucide-x-circle'"
          :title="outcome === 'approved' ? 'Payment approved' : 'Payment declined'"
          description="You may now close this tab and return to Payment Quality Lab."
        />
      </div>
    </UCard>
  </div>
</template>

<script setup lang="ts">
/**
 * F-D2 — PSP Redirect Simulator.
 *
 * Standalone, unauthenticated (see auth.global.ts) mock of an external PSP
 * checkout page. Opened in a new browser tab from Error Lab, demonstrating
 * the multi-tab redirect handoff pattern used by real card/3DS checkouts —
 * without implementing any real PSP integration, card fields, or PAN data
 * (explicitly out of scope for this project).
 */

definePageMeta({ layout: false })

const outcome = ref<'approved' | 'declined' | null>(null)
</script>
