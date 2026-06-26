<template>
  <UDashboardPanel id="error-lab">
    <template #header>
      <UDashboardNavbar title="Error Lab">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
    <div class="space-y-4">
      <UAlert
        icon="i-lucide-flask-conical"
        color="warning"
        variant="subtle"
        title="Learning surface"
        description="Each button below crafts a request designed to produce the listed HTTP status code. Request and response details are shown immediately after the call completes."
      />

      <div class="grid grid-cols-1 gap-4 lg:grid-cols-2 xl:grid-cols-3">
        <UCard
          v-for="scenario in scenarios"
          :key="scenario.status"
          class="flex flex-col"
        >
          <template #header>
            <div class="flex items-center justify-between gap-2">
              <div class="flex items-center gap-2">
                <HttpStatusBadge :status="scenario.status" />
                <span class="text-sm font-semibold text-gray-800 dark:text-gray-200">
                  {{ scenario.title }}
                </span>
              </div>
            </div>
          </template>

          <div class="flex flex-col gap-4 flex-1">
            <p class="text-sm text-gray-600 dark:text-gray-400">{{ scenario.description }}</p>

            <UButton
              :data-testid="`error-lab-trigger-${scenario.status}`"
              color="error"
              variant="outline"
              icon="i-lucide-zap"
              :loading="scenario.state.isLoading.value"
              :disabled="scenario.state.isLoading.value"
              @click="triggerScenario(scenario)"
            >
              Trigger {{ scenario.status }}
            </UButton>

            <!-- Loading state -->
            <LoadingState v-if="scenario.state.isLoading.value" message="Calling backend…" />

            <!-- Result -->
            <template v-else-if="scenario.state.result.value !== null">
              <!-- Status + key headers -->
              <div class="space-y-2">
                <div class="flex items-center gap-2">
                  <span class="text-xs font-medium text-gray-500">Response status:</span>
                  <HttpStatusBadge :status="scenario.state.result.value.status" />
                </div>

                <div v-if="hasHeaders(scenario.state.result.value.responseHeaders)">
                  <p class="text-xs font-medium text-gray-500 mb-1">Response headers</p>
                  <HeaderKeyValuePanel :headers="scenario.state.result.value.responseHeaders" />
                </div>
              </div>

              <!-- Problem details -->
              <ProblemDetailsCard
                v-if="scenario.state.result.value.problem"
                :problem="scenario.state.result.value.problem"
              />

              <!-- Full request/response debug panel -->
              <ApiDebugPanel
                :request="scenario.state.result.value.requestInfo"
                :response="scenario.state.result.value.responseInfo"
              />
            </template>

            <!-- Error indication when backend unreachable -->
            <UAlert
              v-else-if="scenario.state.errorMessage.value"
              icon="i-lucide-wifi-off"
              color="error"
              variant="subtle"
              title="Backend unreachable"
              :description="scenario.state.errorMessage.value"
            />
          </div>
        </UCard>
      </div>
    </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
/**
 * Error Lab page — intentionally triggers the 9 supported HTTP error scenarios.
 *
 * Each scenario calls its own dedicated proxy route under /api/error-lab/trigger-{code}.
 * The proxy routes make the real backend call with the intentionally wrong inputs.
 *
 * Security: the bearer token is NEVER rendered. The Authorization header is
 * shown only as the fixed masked placeholder `Bearer ••••••••` (handled by
 * ApiDebugPanel and HeaderKeyValuePanel automatically).
 *
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7
 */

definePageMeta({ layout: 'dashboard' })

import { z } from 'zod'
import type { ProblemDetails } from '~/types/api'
import { problemDetailsSchema } from '~/schemas/problem-details.schema'

// ----- Per-scenario reactive state -----

interface ScenarioResult {
  status: number
  responseHeaders: Record<string, string>
  problem: ProblemDetails | null
  requestInfo: { method: string; path: string; headers: Record<string, string> }
  responseInfo: { status: number; headers: Record<string, string>; body: string }
}

function makeScenarioState() {
  return {
    isLoading: ref(false),
    result: ref<ScenarioResult | null>(null),
    errorMessage: ref<string | null>(null),
  }
}

type ScenarioState = ReturnType<typeof makeScenarioState>

interface ScenarioConfig {
  status: number
  title: string
  description: string
  /** Proxy path to call (relative, under /api/error-lab/) */
  proxyPath: string
  /** HTTP method used to call the proxy */
  proxyMethod: string
  /** Describes what request we are crafting for the debug panel (shown immediately on click) */
  requestLabel: { method: string; path: string; headers: Record<string, string> }
  state: ScenarioState
}

// ----- Scenario definitions -----
// proxyPath and proxyMethod describe the Nuxt proxy endpoint.
// requestLabel describes the actual backend-facing request intent (for learning display).

const scenarios: ScenarioConfig[] = [
  {
    status: 400,
    title: 'Bad Request',
    description:
      'POST a payment order with an invalid body (negative amountMinor). The backend validation layer rejects it and returns 400 with a problem+json body.',
    proxyPath: '/api/error-lab/trigger-400',
    proxyMethod: 'POST',
    requestLabel: {
      method: 'POST',
      path: '/api/merchants/error-lab-merchant/payment-orders',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ••••••••',
        'Idempotency-Key': '(generated)',
      },
    },
    state: makeScenarioState(),
  },
  {
    status: 401,
    title: 'Unauthorized',
    description:
      'Call a protected endpoint without a valid bearer token. The backend returns 401 because no Authorization header is sent.',
    proxyPath: '/api/error-lab/trigger-401',
    proxyMethod: 'GET',
    requestLabel: {
      method: 'GET',
      path: '/api/merchants',
      headers: { 'Content-Type': 'application/json' },
    },
    state: makeScenarioState(),
  },
  {
    status: 403,
    title: 'Forbidden',
    description:
      'Submit a request with an invalid/insufficient token. The backend returns 403 — the token is present but lacks the required authority.',
    proxyPath: '/api/error-lab/trigger-403',
    proxyMethod: 'GET',
    requestLabel: {
      method: 'GET',
      path: '/api/merchants',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ••••••••',
      },
    },
    state: makeScenarioState(),
  },
  {
    status: 404,
    title: 'Not Found',
    description:
      'Request a merchant UUID that does not exist. The backend returns 404 with a problem+json body.',
    proxyPath: '/api/error-lab/trigger-404',
    proxyMethod: 'GET',
    requestLabel: {
      method: 'GET',
      path: '/api/merchants/00000000-0000-0000-0000-000000000000',
      headers: {
        Authorization: 'Bearer ••••••••',
      },
    },
    state: makeScenarioState(),
  },
  {
    status: 406,
    title: 'Not Acceptable',
    description:
      'Send Accept: application/xml. The backend only produces application/json, so it returns 406.',
    proxyPath: '/api/error-lab/trigger-406',
    proxyMethod: 'GET',
    requestLabel: {
      method: 'GET',
      path: '/api/merchants',
      headers: {
        Authorization: 'Bearer ••••••••',
        Accept: 'application/xml',
      },
    },
    state: makeScenarioState(),
  },
  {
    status: 409,
    title: 'Conflict (Idempotency)',
    description:
      'Repeat the same Idempotency-Key with a different payload. The backend detects the key/payload mismatch and returns 409 idempotency_conflict.',
    proxyPath: '/api/error-lab/trigger-409',
    proxyMethod: 'POST',
    requestLabel: {
      method: 'POST',
      path: '/api/merchants/error-lab-merchant-409/payment-orders',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ••••••••',
        'Idempotency-Key': '(same key, different payload)',
      },
    },
    state: makeScenarioState(),
  },
  {
    status: 412,
    title: 'Precondition Failed (Stale ETag)',
    description:
      'Send a lifecycle action with a stale If-Match value. Creates a real order first, then authorizes it with the wrong ETag → 412.',
    proxyPath: '/api/error-lab/trigger-412',
    proxyMethod: 'POST',
    requestLabel: {
      method: 'POST',
      path: '/api/merchants/{id}/payment-orders/{orderId}/authorize',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ••••••••',
        'If-Match': '"stale-etag-version-0"',
        'Idempotency-Key': '(generated)',
      },
    },
    state: makeScenarioState(),
  },
  {
    status: 415,
    title: 'Unsupported Media Type',
    description:
      'POST with Content-Type: text/plain instead of application/json. The backend rejects the content type and returns 415.',
    proxyPath: '/api/error-lab/trigger-415',
    proxyMethod: 'POST',
    requestLabel: {
      method: 'POST',
      path: '/api/merchants',
      headers: {
        'Content-Type': 'text/plain',
        Authorization: 'Bearer ••••••••',
      },
    },
    state: makeScenarioState(),
  },
  {
    status: 428,
    title: 'Precondition Required (Missing If-Match)',
    description:
      'Send an authorize action without the required If-Match header. Creates a real order first, then authorizes it without If-Match → 428.',
    proxyPath: '/api/error-lab/trigger-428',
    proxyMethod: 'POST',
    requestLabel: {
      method: 'POST',
      path: '/api/merchants/{id}/payment-orders/{orderId}/authorize',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ••••••••',
        'Idempotency-Key': '(generated)',
        // No If-Match — intentional
      },
    },
    state: makeScenarioState(),
  },
]

// ----- Transport -----

const { request } = useApiClient()

async function triggerScenario(scenario: ScenarioConfig) {
  const state = scenario.state
  state.isLoading.value = true
  state.result.value = null
  state.errorMessage.value = null

  try {
    const response = await request(
      scenario.proxyPath,
      z.unknown(),
      {
        method: scenario.proxyMethod,
      }
    )

    // Build response headers record from the ApiHeaders shape
    const responseHeaders: Record<string, string> = {}
    const h = response.headers
    if (h.etag) responseHeaders['ETag'] = h.etag
    if (h.cacheControl) responseHeaders['Cache-Control'] = h.cacheControl
    if (h.vary) responseHeaders['Vary'] = h.vary
    if (h.correlationId) responseHeaders['X-Correlation-ID'] = h.correlationId
    if (h.location) responseHeaders['Location'] = h.location
    if (h.allow) responseHeaders['Allow'] = h.allow
    if (h.acceptPatch) responseHeaders['Accept-Patch'] = h.acceptPatch

    // Parse problem details from raw body if not already populated
    let problem = response.problem
    if (!problem && response.raw) {
      try {
        const parsed = problemDetailsSchema.safeParse(JSON.parse(response.raw))
        if (parsed.success) problem = parsed.data
      } catch {
        // raw body is not JSON — ignore
      }
    }

    state.result.value = {
      status: response.status,
      responseHeaders,
      problem,
      requestInfo: {
        method: scenario.requestLabel.method,
        path: scenario.requestLabel.path,
        headers: scenario.requestLabel.headers,
      },
      responseInfo: {
        status: response.status,
        headers: responseHeaders,
        body: response.raw,
      },
    }
  } catch (err: unknown) {
    // useApiClient catches errors internally and returns them as ApiResponse,
    // so this branch only fires on truly unexpected failures (e.g. network error)
    const message = err instanceof Error ? err.message : 'An unexpected error occurred'
    state.errorMessage.value = message
  } finally {
    state.isLoading.value = false
  }
}

// ----- Helpers -----

function hasHeaders(h: Record<string, string>): boolean {
  return Object.keys(h).length > 0
}
</script>
