export default defineEventHandler(async (event) => {
  requireMirrorLab(event)
  const id = getRouterParam(event, 'id')
  return backendApi(event, `/api/mirror-lab/consents/${id}/revoke`, { method: 'POST' })
})
