export default defineEventHandler(async (event) => {
  return backendApi(event, '/api/ops/feed/inject', {
    method: 'POST',
    body: await readBody(event),
  })
})
