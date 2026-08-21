export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  return backendApi(event, '/api/merchants/import/commit', {
    method: 'POST',
    body,
  })
})
