export default defineEventHandler(async (event) => {
  return backendApi(event, '/api/users/me/payment-views', {
    method: 'GET',
  })
})
