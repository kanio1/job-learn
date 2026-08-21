import type { ApiResponse } from '~/types/api'
import {
  orgTreeResponseSchema,
  type OrgTreeResponse,
} from '~/schemas/org-tree.schema'

export function useOrgTreeApi() {
  const { request } = useApiClient()

  async function getOrgTree(parent?: string): Promise<ApiResponse<OrgTreeResponse>> {
    return request('/api/org-tree', orgTreeResponseSchema, {
      query: parent ? { parent } : undefined,
    })
  }

  return { getOrgTree }
}
