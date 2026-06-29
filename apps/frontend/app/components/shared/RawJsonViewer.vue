<template>
  <div data-testid="raw-json-viewer">
    <UCard>
      <template #header>
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium">Raw Response Body</span>
          <UBadge v-if="!isValidJson" color="warning" variant="subtle" size="xs">
            Not valid JSON
          </UBadge>
        </div>
      </template>

      <p v-if="!props.content || !props.content.trim()" class="text-sm text-gray-400 italic">
        No body
      </p>
      <pre v-else class="overflow-auto text-xs font-mono text-gray-800 dark:text-gray-200 whitespace-pre-wrap break-all">{{ displayContent }}</pre>
    </UCard>
  </div>
</template>

<script setup lang="ts">
/**
 * Renders raw response body content.
 * - If the content is valid JSON: displays it indented with 2-space indent.
 *   Key order is preserved by using the original raw string as the data source
 *   (we parse only to verify validity, then re-indent via string manipulation
 *   of the parsed output — JSON.stringify preserves the parsed key order which
 *   mirrors the original wire order for well-formed JSON).
 * - If the content is NOT valid JSON: renders raw content with a "Not valid JSON" label.
 *
 * Requirements: 8.6, 8.7
 */

const props = defineProps<{
  content: string
}>()

const isValidJson = computed<boolean>(() => {
  if (!props.content.trim()) return false
  try {
    JSON.parse(props.content)
    return true
  } catch {
    return false
  }
})

const displayContent = computed<string>(() => {
  if (!props.content) return ''
  if (!isValidJson.value) return props.content
  try {
    // Parse and re-stringify with 2-space indent.
    // JSON.parse preserves the key order present in the wire string (insertion order);
    // JSON.stringify with indent then reproduces that order in a human-readable form.
    return JSON.stringify(JSON.parse(props.content), null, 2)
  } catch {
    return props.content
  }
})
</script>
