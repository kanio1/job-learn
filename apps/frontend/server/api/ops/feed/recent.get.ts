export default defineEventHandler(async (event) => {
  return backendApi(event, '/api/ops/feed/recent', {
    method: 'GET',
  })
})
