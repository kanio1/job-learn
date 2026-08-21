import { z } from 'zod'
import type { ApiResponse } from '~/types/api'
import {
  notificationListSchema,
  notificationSchema,
  type OpsNotification,
} from '~/schemas/notification.schema'

export function useNotificationsApi() {
  const { request } = useApiClient()

  function listNotifications(unreadOnly = false): Promise<ApiResponse<{ content: OpsNotification[] }>> {
    return request('/api/notifications', notificationListSchema, {
      query: unreadOnly ? { unreadOnly: true } : undefined,
    })
  }

  function markRead(notificationId: string): Promise<ApiResponse<OpsNotification>> {
    return request(`/api/notifications/${notificationId}/read`, notificationSchema, {
      method: 'POST',
    })
  }

  function markAllRead(): Promise<ApiResponse<unknown>> {
    return request('/api/notifications/read-all', z.unknown(), {
      method: 'POST',
    })
  }

  return { listNotifications, markRead, markAllRead }
}
