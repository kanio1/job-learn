import { expect, type APIResponse, type Page } from '@playwright/test'
import { z } from 'zod'
import { expectNoAuthorizationInNetworkResponse } from './network'

const problemStatusSchema = z.object({ status: z.number() }).passthrough()

export function triggerMethod(status: number, getStatuses: readonly number[]): 'GET' | 'POST' {
  return getStatuses.includes(status) ? 'GET' : 'POST'
}

export async function liveErrorLabTrigger(
  page: Page,
  status: number,
  getStatuses: readonly number[],
): Promise<APIResponse> {
  return page.request.fetch(`/api/error-lab/trigger-${status}`, {
    method: triggerMethod(status, getStatuses),
  })
}

export async function expectProblemStatus(response: APIResponse, status: number): Promise<void> {
  expect(response.status()).toBe(status)
  expect(response.headers()['content-type'] ?? '').toMatch(/application\/problem\+json/)
  expect(problemStatusSchema.parse(await response.json()).status).toBe(status)
  expectNoAuthorizationInNetworkResponse(response)
}
