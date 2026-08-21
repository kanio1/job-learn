import { defineStore } from 'pinia'
import type { OpsNotification } from '~/schemas/notification.schema'

export const useNotificationStore = defineStore('notifications', () => {
  const items = ref<OpsNotification[]>([])
  const { listNotifications, markRead, markAllRead } = useNotificationsApi()

  const unreadCount = computed(() => items.value.filter(item => !item.readAt).length)
  const unreadItems = computed(() => items.value.filter(item => !item.readAt))
  const readItems = computed(() => items.value.filter(item => Boolean(item.readAt)))

  async function refresh() {
    const response = await listNotifications()
    if (response.data?.content) {
      items.value = response.data.content
    }
  }

  async function markItemRead(notificationId: string) {
    const response = await markRead(notificationId)
    if (response.data) {
      items.value = items.value.map(item =>
        item.notificationId === notificationId ? response.data! : item)
    }
  }

  async function markEveryRead() {
    await markAllRead()
    await refresh()
  }

  return { items, unreadCount, unreadItems, readItems, refresh, markItemRead, markEveryRead }
})
