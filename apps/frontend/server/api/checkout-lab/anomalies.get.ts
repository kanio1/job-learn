export default defineEventHandler(async (event) => {
  return checkoutLabApi(event, '/api/checkout-lab/anomalies')
})
