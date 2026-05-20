export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  return backendApi(event, '/api/merchants', {
    method: 'POST',
    body
  })
})
