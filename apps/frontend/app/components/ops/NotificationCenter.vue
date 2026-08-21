<template>
  <UPopover
    v-if="canReadNotifications"
    :content="{ side: 'right', align: 'start', sideOffset: 8, collisionPadding: 16 }"
  >
    <div class="inline-flex items-center">
      <UChip
        :show="unreadCount > 0"
        :text="String(unreadCount)"
        color="error"
        size="sm"
      >
        <UButton
          icon="i-lucide-bell"
          color="neutral"
          variant="ghost"
          square
          data-testid="notification-bell"
          aria-label="Notifications"
        />
      </UChip>
      <span
        v-if="unreadCount > 0"
        data-testid="notification-unread-count"
        class="sr-only"
      >{{ unreadCount }}</span>
    </div>
    <template #content>
      <div
        class="w-80 max-h-[min(28rem,calc(100vh-5rem))] overflow-y-auto p-3 space-y-3"
        data-testid="notification-popover"
      >
        <div class="flex items-center justify-between">
          <p class="text-sm font-semibold">Notifications</p>
          <UButton
            v-if="unreadCount > 0"
            size="xs"
            variant="ghost"
            data-testid="notification-read-all"
            @click="onReadAll"
          >
            Read all
          </UButton>
        </div>
        <div v-if="unreadItems.length > 0">
          <p class="text-xs text-muted mb-1">New</p>
          <ul class="space-y-2">
            <li
              v-for="item in unreadItems"
              :key="item.notificationId"
              :data-testid="`notification-item-${item.notificationId}`"
              class="rounded-md bg-elevated p-2"
            >
              <p class="text-sm font-medium">{{ item.title }}</p>
              <p class="text-xs text-muted">{{ item.body }}</p>
              <UButton
                size="xs"
                variant="ghost"
                class="mt-1"
                :data-testid="`notification-mark-read-${item.notificationId}`"
                @click="onMarkRead(item.notificationId)"
              >
                Mark as read
              </UButton>
            </li>
          </ul>
        </div>
        <div v-if="readItems.length > 0">
          <p class="text-xs text-muted mb-1">Earlier</p>
          <ul class="space-y-2">
            <li
              v-for="item in readItems"
              :key="item.notificationId"
              :data-testid="`notification-item-${item.notificationId}`"
              class="p-2"
            >
              <p class="text-sm">{{ item.title }}</p>
              <p class="text-xs text-muted">{{ item.body }}</p>
            </li>
          </ul>
        </div>
        <p v-if="items.length === 0" class="text-sm text-muted">No notifications.</p>
      </div>
    </template>
  </UPopover>
</template>

<script setup lang="ts">
const { can } = useAuthorization()
const canReadNotifications = computed(() => can.value.canReadNotifications)
const store = useNotificationStore()
const unreadCount = computed(() => store.unreadCount)
const unreadItems = computed(() => store.unreadItems)
const readItems = computed(() => store.readItems)
const items = computed(() => store.items)

watch(canReadNotifications, (allowed) => {
  if (allowed) {
    void store.refresh()
  }
}, { immediate: true })

async function onMarkRead(id: string) {
  await store.markItemRead(id)
}

async function onReadAll() {
  await store.markEveryRead()
}
</script>
