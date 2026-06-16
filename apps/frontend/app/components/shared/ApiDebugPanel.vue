<template>
  <div data-testid="api-debug-panel">
    <UCard>
      <template #header>
        <div class="flex items-center gap-2">
          <UIcon name="i-lucide-activity" class="text-gray-500" />
          <span class="text-sm font-semibold">API Debug</span>
        </div>
      </template>

      <UTabs :items="tabs" class="w-full">
        <template #request>
          <div class="space-y-4 pt-3">
            <div v-if="request" class="space-y-3">
              <div class="flex items-center gap-2">
                <UBadge color="info" variant="subtle" size="sm" class="font-mono font-bold">
                  {{ request.method.toUpperCase() }}
                </UBadge>
                <span class="font-mono text-sm text-gray-800 dark:text-gray-200 break-all">{{ request.path }}</span>
              </div>
              <div v-if="request.headers && Object.keys(request.headers).length > 0">
                <p class="text-xs font-medium text-gray-500 mb-1">Request Headers</p>
                <HeaderKeyValuePanel :headers="maskedRequestHeaders" />
              </div>
              <p v-else class="text-sm text-gray-400 italic">No request headers</p>
            </div>
            <p v-else class="text-sm text-gray-400 italic">No request recorded</p>
          </div>
        </template>

        <template #response>
          <div class="space-y-4 pt-3">
            <div v-if="response" class="space-y-3">
              <div>
                <p class="text-xs font-medium text-gray-500 mb-1">Status</p>
                <HttpStatusBadge :status="response.status" />
              </div>
              <div v-if="response.headers && Object.keys(response.headers).length > 0">
                <p class="text-xs font-medium text-gray-500 mb-1">Response Headers</p>
                <HeaderKeyValuePanel :headers="maskedResponseHeaders" />
              </div>
              <div v-if="response.body != null">
                <p class="text-xs font-medium text-gray-500 mb-1">Response Body</p>
                <RawJsonViewer :content="response.body" />
              </div>
            </div>
            <p v-else class="text-sm text-gray-400 italic">No response recorded</p>
          </div>
        </template>
      </UTabs>
    </UCard>
  </div>
</template>

<script setup lang="ts">
/**
 * Shows request and response details for the most recent API call.
 * SECURITY: The Authorization header value is ALWAYS replaced with
 * the fixed masked placeholder `Bearer ••••••••`. No token character
 * is ever rendered.
 *
 * Requirements: 6.2, 6.6, 8.11, 11.3
 */

interface RequestInfo {
  method: string
  path: string
  headers?: Record<string, string>
}

interface ResponseInfo {
  status: number
  headers?: Record<string, string>
  body?: string
}

const props = defineProps<{
  request?: RequestInfo | null
  response?: ResponseInfo | null
}>()

const MASKED_AUTH = 'Bearer ••••••••'

function maskHeaders(headers: Record<string, string> | undefined): Record<string, string> {
  if (!headers) return {}
  return Object.fromEntries(
    Object.entries(headers).map(([k, v]) => [
      k,
      k.toLowerCase() === 'authorization' ? MASKED_AUTH : v,
    ])
  )
}

const maskedRequestHeaders = computed(() => maskHeaders(props.request?.headers))
const maskedResponseHeaders = computed(() => maskHeaders(props.response?.headers))

const tabs = [
  { label: 'Request', slot: 'request' as const },
  { label: 'Response', slot: 'response' as const },
]
</script>
