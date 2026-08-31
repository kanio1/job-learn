/** UTC calendar day — same bound as Wave A payment date filters. Not local TZ. */
export function utcToday(): string {
  return new Date().toISOString().slice(0, 10)
}

export function utcDayBounds(isoDate = utcToday()) {
  return {
    from: `${isoDate}T00:00:00.000Z`,
    to: `${isoDate}T23:59:59.999Z`,
  }
}
