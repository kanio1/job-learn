<template>
  <div data-testid="http-headers-panel">
    <UCard>
      <template #header>
        <span class="text-sm font-medium">HTTP Headers</span>
      </template>

      <p v-if="normalizedHeaders.length === 0" class="text-sm text-gray-400 italic">
        No headers present
      </p>

      <table v-else class="w-full text-sm">
        <tbody>
          <tr
            v-for="header in normalizedHeaders"
            :key="header.key"
            class="border-b border-gray-100 dark:border-gray-800 last:border-0"
          >
            <td class="py-1.5 pr-4 font-mono font-medium text-gray-600 dark:text-gray-400 align-top whitespace-nowrap">
              {{ header.key }}
            </td>
            <td class="py-1.5 font-mono text-gray-900 dark:text-gray-100 break-all">
              {{ header.value }}
            </td>
          </tr>
        </tbody>
      </table>
    </UCard>
  </div>
</template>

<script setup lang="ts">
/**
 * Renders HTTP headers as a key-value table.
 * - Accepts both object and array shapes.
 * - Shows an explicit empty indicator when zero headers are present.
 * - SECURITY: Any header whose key is "authorization" (case-insensitive)
 *   has its value replaced with the fixed masked placeholder `Bearer ••••••••`.
 *   No character of an actual token is ever rendered.
 *
 * Requirements: 8.3, 8.4, 6.6, 11.3, 12.8
 */

const props = defineProps<{
  headers: Record<string, string> | Array<{ key: string; value: string }>
}>()

const MASKED_AUTH = 'Bearer ••••••••'

function maskValue(key: string, value: string): string {
  return key.toLowerCase() === 'authorization' ? MASKED_AUTH : value
}

const normalizedHeaders = computed<Array<{ key: string; value: string }>>(() => {
  if (Array.isArray(props.headers)) {
    return props.headers.map(h => ({
      key: h.key,
      value: maskValue(h.key, h.value),
    }))
  }
  return Object.entries(props.headers).map(([key, value]) => ({
    key,
    value: maskValue(key, value),
  }))
})
</script>
