import type { ApiResponse } from '~/types/api'
import { searchResponseSchema, type SearchResponse } from '~/schemas/search.schema'

export function useEntitySearchApi() {
  const { request } = useApiClient()

  async function searchEntities(q: string, signal?: AbortSignal): Promise<ApiResponse<SearchResponse>> {
    return request('/api/search', searchResponseSchema, {
      query: { q: q.slice(0, 80) },
      signal,
    })
  }

  return { searchEntities }
}
