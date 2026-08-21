export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')
  return backendApi(event, `/api/users/me/payment-views/${id}`, {
    method: 'DELETE',
  })
})
