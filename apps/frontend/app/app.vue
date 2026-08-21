<template>
  <UApp :locale="uiLocale">
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </UApp>
</template>

<script setup lang="ts">
import { en, pl, sv } from '@nuxt/ui/locale'
import { bcp47For } from '~/utils/localeFormat'

const { locale } = useI18n()
const uiLocales = { en, pl, sv } as const
const uiLocale = computed(() => uiLocales[locale.value as keyof typeof uiLocales] ?? en)

useHead(() => ({
  htmlAttrs: { lang: bcp47For(locale.value) },
}))
</script>
