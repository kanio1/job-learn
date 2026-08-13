export default defineEventHandler(async (event) => {
  const bookingId = getRouterParam(event, 'bookingId')
  return checkoutLabApi(event, `/api/checkout-lab/bookings/${bookingId}`)
})
