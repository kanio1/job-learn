export default defineEventHandler(async (event) => {
  await requireMirrorLabSession(event)
  const id = getRouterParam(event, 'id') || ''
  const revoked = revokeDevice(sessionKey(event), id)
  return { revoked }
})
