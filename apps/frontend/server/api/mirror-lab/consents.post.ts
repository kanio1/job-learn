export default defineEventHandler(async (event) => {
  requireMirrorLab(event)
  return backendApi(event, '/api/mirror-lab/consents', { method: 'POST', body: {} })
})
