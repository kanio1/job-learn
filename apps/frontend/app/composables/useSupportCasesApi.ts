import type { ApiResponse } from '~/types/api'
import {
  bulkAssignResultSchema,
  supportCaseListSchema,
  supportCaseSchema,
  type BulkAssignResult,
  type SupportCase,
} from '~/schemas/support-case.schema'

export function useSupportCasesApi() {
  const { request } = useApiClient()

  async function listCases(
    query?: Record<string, string | number | boolean | null | undefined>,
  ): Promise<ApiResponse<{ content: SupportCase[] }>> {
    const params: Record<string, string | number | boolean> = {}
    if (query) {
      for (const [key, value] of Object.entries(query)) {
        if (value !== undefined && value !== null && value !== '') {
          params[key] = value
        }
      }
    }
    return request('/api/support/cases', supportCaseListSchema, {
      query: Object.keys(params).length > 0 ? params : undefined,
    })
  }

  async function getCase(caseId: string): Promise<ApiResponse<SupportCase>> {
    return request(`/api/support/cases/${caseId}`, supportCaseSchema)
  }

  async function createCase(payload: {
    merchantId: string
    title: string
    paymentOrderId?: string | null
    priority?: string
    caseReference?: string
    assigneeSubject?: string
  }): Promise<ApiResponse<SupportCase>> {
    return request('/api/support/cases', supportCaseSchema, {
      method: 'POST',
      body: payload,
    })
  }

  async function patchCase(
    caseId: string,
    payload: { status?: string, assigneeSubject?: string | null },
    ifMatch: string,
  ): Promise<ApiResponse<SupportCase>> {
    return request(`/api/support/cases/${caseId}`, supportCaseSchema, {
      method: 'PATCH',
      body: payload,
      headers: { 'If-Match': ifMatch },
    })
  }

  async function bulkAssign(
    caseIds: string[],
    assigneeSubject: string,
  ): Promise<ApiResponse<BulkAssignResult>> {
    return request('/api/support/cases/bulk-assign', bulkAssignResultSchema, {
      method: 'POST',
      body: { caseIds, assigneeSubject },
    })
  }

  return { listCases, getCase, createCase, patchCase, bulkAssign }
}
