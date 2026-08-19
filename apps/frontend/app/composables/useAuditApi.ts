import type { ApiResponse } from '~/types/api'
import {
  auditEventSchema,
  auditListResponseSchema,
  auditQuerySchema,
  type AuditEvent,
  type AuditListResponse,
  type AuditQuery,
} from '~/schemas/audit.schema'

export function useAuditApi() {
  const { request } = useApiClient()

  async function list(query?: AuditQuery): Promise<ApiResponse<AuditListResponse>> {
    const safeQuery = auditQuerySchema.parse(query ?? {})
    return request('/api/audit', auditListResponseSchema, {
      query: safeQuery,
    })
  }

  async function getEntry(id: string): Promise<ApiResponse<AuditEvent> | null> {
    const response = await request(
      `/api/audit/${encodeURIComponent(id)}`,
      auditEventSchema
    )
    return response.status === 404 ? null : response
  }

  return { list, getEntry }
}

export type { AuditEvent, AuditListResponse, AuditQuery }

