<template>
  <div
    v-if="slides.length > 0"
    data-testid="evidence-carousel"
    :data-active-index="selectedIndex"
    class="space-y-2"
  >
    <UCarousel
      v-slot="{ item, index }"
      :items="slides"
      arrows
      dots
      @select="selectedIndex = $event"
    >
      <div
        class="flex min-h-48 flex-col items-center justify-center gap-3 p-4"
        :data-testid="`evidence-slide-${index}`"
      >
        <UAlert
          v-if="item.kind === 'error'"
          color="error"
          variant="subtle"
          title="Evidence not found"
          description="This attachment is missing or is not part of this payment order."
          data-testid="evidence-error-slide"
        />
        <img
          v-else-if="item.kind === 'image'"
          :src="item.src"
          :alt="item.name"
          :loading="index === selectedIndex ? 'eager' : 'lazy'"
          class="max-h-64 max-w-full rounded object-contain"
          data-testid="evidence-slide-image"
        >
        <div v-else class="flex flex-col items-center gap-2" data-testid="evidence-slide-file">
          <UIcon name="i-lucide-file-text" class="size-10 text-muted" />
          <p class="text-sm">{{ item.name }}</p>
          <UButton
            size="xs"
            variant="outline"
            :href="item.src"
            :download="item.name"
            data-testid="evidence-slide-download"
          >
            Download
          </UButton>
        </div>
      </div>
    </UCarousel>
  </div>
</template>

<script setup lang="ts">
import type { PaymentEvidence } from '~/schemas/payment-order.schema'

type Slide =
  | { kind: 'image', evidenceId: string, name: string, src: string }
  | { kind: 'file', evidenceId: string, name: string, src: string }
  | { kind: 'error', evidenceId: string, name: string, src: string }

const props = defineProps<{
  merchantId: string
  paymentOrderId: string
  evidence: PaymentEvidence[]
}>()

const route = useRoute()
const selectedIndex = ref(0)

function contentUrl(evidenceId: string) {
  return `/api/merchants/${props.merchantId}/payment-orders/${props.paymentOrderId}/evidence/${evidenceId}`
}

function isImage(contentType: string) {
  return contentType === 'image/png' || contentType === 'image/jpeg'
}

const requestedId = computed(() => {
  const value = route.query.evidence
  return typeof value === 'string' && value.length > 0 ? value : null
})

const slides = computed<Slide[]>(() => {
  const items: Slide[] = props.evidence.map((item) => {
    const src = contentUrl(item.evidenceId)
    if (isImage(item.contentType) && item.hasContent) {
      return { kind: 'image', evidenceId: item.evidenceId, name: item.originalFilename, src }
    }
    return { kind: 'file', evidenceId: item.evidenceId, name: item.originalFilename, src }
  })
  if (requestedId.value && !items.some(item => item.evidenceId === requestedId.value)) {
    return [{
      kind: 'error',
      evidenceId: requestedId.value,
      name: 'missing',
      src: contentUrl(requestedId.value),
    }, ...items]
  }
  return items
})

</script>
