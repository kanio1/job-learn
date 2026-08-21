import { bcp47For, formatDateOnly, formatMinorAmount, LOCALE_BCP47 } from '~/utils/localeFormat'

function normalizeLocale(raw: string | null | undefined): string | null {
  if (!raw) {
    return null
  }
  const value = raw.replace(/['"]/g, '').trim()
  return value in LOCALE_BCP47 ? value : null
}

function browserLocale(): string | null {
  if (!import.meta.client) {
    return null
  }
  const nav = navigator.language.toLowerCase()
  if (nav.startsWith('pl')) {
    return 'pl'
  }
  if (nav.startsWith('sv')) {
    return 'sv'
  }
  if (nav.startsWith('en')) {
    return 'en'
  }
  return null
}

export function useLocaleFormat() {
  const { locale } = useI18n()
  const localeCookie = useCookie<string | null>('pq-locale')
  const localeTag = computed(() => {
    const fromI18n = normalizeLocale(locale.value)
    const fromCookie = normalizeLocale(localeCookie.value)
    if (fromI18n && fromI18n !== 'en') {
      return bcp47For(fromI18n)
    }
    if (fromCookie) {
      return bcp47For(fromCookie)
    }
    return bcp47For(browserLocale() ?? fromI18n ?? 'en')
  })

  function amount(amountMinor: number, currency: string): string {
    return formatMinorAmount(amountMinor, currency, localeTag.value)
  }

  function dateOnly(iso: string): string {
    return formatDateOnly(iso, localeTag.value)
  }

  return { localeTag, amount, dateOnly }
}
