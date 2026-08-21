export default defineEventHandler(async (event) => {
  return backendApi(event, '/api/support/cases', {
    method: 'POST',
    body: await readBody(event),
  })
})
