<template>
  <div class="flex flex-col gap-1">
    <ULocaleSelect
      v-model="current"
      :locales="selectLocales"
      data-testid="locale-select"
      aria-label="Locale"
      class="w-36"
    />
    <p v-if="!collapsed" class="text-[11px] text-muted" data-testid="locale-sample">
      <span data-testid="locale-sample-amount">{{ sampleAmount }}</span>
      ·
      <span data-testid="locale-sample-date">{{ sampleDate }}</span>
    </p>
  </div>
</template>

<script setup lang="ts">
import { en, pl, sv } from '@nuxt/ui/locale'

defineProps<{
  collapsed?: boolean
}>()

type AppLocale = 'en' | 'pl' | 'sv'

function isAppLocale(value: string): value is AppLocale {
  return value === 'en' || value === 'pl' || value === 'sv'
}

const { locale, setLocale } = useI18n()
const { amount, dateOnly } = useLocaleFormat()
const localeCookie = useCookie<string>('pq-locale')

const selectLocales = [en, pl, sv]

const current = computed({
  get: () => locale.value,
  set: (code: string) => {
    if (!isAppLocale(code)) {
      return
    }
    localeCookie.value = code
    void setLocale(code)
  },
})

const sampleAmount = computed(() => amount(123456, 'EUR'))
const sampleDate = computed(() => dateOnly('2026-08-20T12:00:00.000Z'))

onMounted(() => {
  const saved = localeCookie.value
  if (saved && isAppLocale(saved) && saved !== locale.value) {
    void setLocale(saved)
  }
})
</script>
