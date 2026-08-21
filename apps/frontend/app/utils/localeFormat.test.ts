import { describe, expect, it } from 'vitest'
import { formatDateOnly, formatMinorAmount } from './localeFormat'

const ORACLE_ISO = '2026-08-20T12:00:00.000Z'

describe('locale format oracles', () => {
  it('en-US amount 123456 minor EUR is €1,234.56', () => {
    expect(formatMinorAmount(123456, 'EUR', 'en-US')).toBe('€1,234.56')
  })

  it('pl-PL amount groups thousands with a space/NBSP', () => {
    expect(formatMinorAmount(123456, 'EUR', 'pl-PL')).toMatch(/1[\s\u00a0\u202f]234,56/)
  })

  it('sv-SE amount groups thousands with a space/NBSP', () => {
    expect(formatMinorAmount(123456, 'EUR', 'sv-SE')).toMatch(/1[\s\u00a0\u202f]234,56/)
  })

  it('en-US date 2026-08-20 is 8/20/2026', () => {
    expect(formatDateOnly(ORACLE_ISO, 'en-US')).toBe('8/20/2026')
  })

  it('pl-PL date 2026-08-20 is 20.08.2026', () => {
    expect(formatDateOnly(ORACLE_ISO, 'pl-PL')).toBe('20.08.2026')
  })

  it('sv-SE date 2026-08-20 is 2026-08-20', () => {
    expect(formatDateOnly(ORACLE_ISO, 'sv-SE')).toBe('2026-08-20')
  })
})
