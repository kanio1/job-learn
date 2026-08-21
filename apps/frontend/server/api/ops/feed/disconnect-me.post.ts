export default defineEventHandler(async (event) => {
  return backendApi(event, '/api/ops/feed/disconnect-me', {
    method: 'POST',
  })
})
