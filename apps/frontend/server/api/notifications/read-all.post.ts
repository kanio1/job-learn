export default defineEventHandler(async (event) => {
  return backendApi(event, '/api/notifications/read-all', {
    method: 'POST',
  })
})
