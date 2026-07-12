<template>
  <div data-testid="expiration-countdown">
    <UBadge v-if="remainingMs <= 0" color="warning" variant="subtle" data-testid="expiration-countdown-expired">
      Authorization expired
    </UBadge>
    <span v-else class="font-mono text-sm" data-testid="expiration-countdown-remaining">
      Expires in {{ formatted }}
    </span>
  </div>
</template>

<script setup lang="ts">
/**
 * F-D1 — live countdown to authorization expiry.
 *
 * Purely a display concern: server-side expiry (scheduled sweep +
 * lazy-on-capture check) is authoritative. This countdown reaching zero
 * does not itself flip anything — it just tells the operator the order is
 * now eligible for expiry on the next sweep tick or capture attempt, so a
 * stale "AUTHORIZED" screen doesn't look actionable when it no longer is.
 */

const props = defineProps<{
  expiresAt: string
}>()

const now = ref(Date.now())
let intervalId: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  intervalId = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
})

const remainingMs = computed(() => new Date(props.expiresAt).getTime() - now.value)

const formatted = computed(() => {
  const totalSeconds = Math.max(0, Math.floor(remainingMs.value / 1000))
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  if (hours > 0) return `${hours}h ${minutes}m`
  if (minutes > 0) return `${minutes}m ${seconds}s`
  return `${seconds}s`
})
</script>
