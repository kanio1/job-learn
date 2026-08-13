export default defineEventHandler(async (event) => {
  requireMirrorLab(event)
  const id = getRouterParam(event, 'id')
  return backendApi(event, `/api/mirror-lab/refund-approvals/${id}/approve`, { method: 'POST' })
})
