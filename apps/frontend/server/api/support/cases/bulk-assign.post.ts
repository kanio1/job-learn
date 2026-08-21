export default defineEventHandler(async (event) => {
  return backendApi(event, '/api/support/cases/bulk-assign', {
    method: 'POST',
    body: await readBody(event),
  })
})
