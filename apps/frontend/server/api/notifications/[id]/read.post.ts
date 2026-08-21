export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')
  return backendApi(event, `/api/notifications/${id}/read`, {
    method: 'POST',
  })
})
