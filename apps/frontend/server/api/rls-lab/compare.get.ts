export default defineEventHandler(async (event) => {
  await requireRlsLabSession(event)
  return backendApi(event, '/api/rls-lab/compare')
})
