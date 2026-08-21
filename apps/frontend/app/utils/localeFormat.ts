export const LOCALE_BCP47: Record<string, string> = {
  en: 'en-US',
  pl: 'pl-PL',
  sv: 'sv-SE',
}

export function bcp47For(localeCode: string): string {
  return LOCALE_BCP47[localeCode] ?? localeCode
}

export function formatMinorAmount(amountMinor: number, currency: string, localeTag: string): string {
  const formatter = new Intl.NumberFormat(localeTag, {
    style: 'currency',
    currency,
  })
  const parts = formatter.formatToParts(amountMinor / 100)
  if (parts.some(part => part.type === 'group')) {
    return joinParts(parts)
  }
  const group = formatter.formatToParts(1_234_567.89).find(part => part.type === 'group')?.value
  if (!group) {
    return joinParts(parts)
  }
  return joinParts(parts.map(part =>
    part.type === 'integer' ? insertGroupSeparator(part.value, group) : part.value,
  ))
}

function joinParts(parts: Array<string | Intl.NumberFormatPart>): string {
  return parts.map(part => typeof part === 'string' ? part : part.value).join('')
}

function insertGroupSeparator(digits: string, group: string): string {
  const negative = digits.startsWith('-')
  const raw = negative ? digits.slice(1) : digits
  if (raw.length <= 3) {
    return digits
  }
  const chunks: string[] = []
  for (let i = raw.length; i > 0; i -= 3) {
    chunks.unshift(raw.slice(Math.max(0, i - 3), i))
  }
  const grouped = chunks.join(group)
  return negative ? `-${grouped}` : grouped
}

export function formatDateOnly(iso: string, localeTag: string): string {
  return new Intl.DateTimeFormat(localeTag, {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(iso))
}
