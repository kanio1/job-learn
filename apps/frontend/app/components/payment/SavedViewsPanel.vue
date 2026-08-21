<template>
  <div class="flex items-center gap-2">
    <UPopover>
      <UButton
        data-testid="payment-views-open"
        color="neutral"
        variant="ghost"
        size="xs"
        icon="i-lucide-bookmark"
        label="Saved views"
      />
      <template #content>
        <div data-testid="payment-views-list" class="w-72 p-2 space-y-1">
          <p v-if="views.length === 0" class="text-xs text-muted px-1 py-2">No saved views yet.</p>
          <div
            v-for="view in views"
            :key="view.id"
            class="flex items-center gap-1"
          >
            <UButton
              color="neutral"
              variant="ghost"
              size="xs"
              class="flex-1 justify-start"
              :label="view.name"
              @click="emit('open', view)"
            />
            <UButton
              color="neutral"
              :variant="view.isDefault ? 'solid' : 'ghost'"
              size="xs"
              square
              icon="i-lucide-star"
              :aria-label="`Set ${view.name} as default`"
              @click="emit('setDefault', view)"
            />
          </div>
        </div>
      </template>
    </UPopover>

    <UButton
      data-testid="payment-view-save"
      color="neutral"
      variant="ghost"
      size="xs"
      icon="i-lucide-save"
      label="Save view"
      @click="saveOpen = true"
    />

    <UModal v-model:open="saveOpen" title="Save payment view">
      <template #body>
        <UFormField label="View name">
          <UInput
            v-model="viewName"
            data-testid="payment-view-name"
            aria-label="View name"
            placeholder="Large EUR captured"
          />
        </UFormField>
      </template>
      <template #footer>
        <UButton
          data-testid="payment-view-save-confirm"
          :disabled="!viewName.trim()"
          @click="confirmSave"
        >
          Save
        </UButton>
      </template>
    </UModal>
  </div>
</template>

<script setup lang="ts">
import type { PaymentView } from '~~/shared/types/payment-view'

const props = defineProps<{
  views: PaymentView[]
}>()

const emit = defineEmits<{
  open: [view: PaymentView]
  save: [name: string]
  setDefault: [view: PaymentView]
}>()

const saveOpen = ref(false)
const viewName = ref('')

function confirmSave() {
  const name = viewName.value.trim()
  if (!name) {
    return
  }
  emit('save', name)
  saveOpen.value = false
  viewName.value = ''
}
</script>
