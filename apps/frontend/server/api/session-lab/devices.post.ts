export default defineEventHandler(async (event) => {
  await requireMirrorLabSession(event)
  const body = await readBody<{ id?: string, label?: string }>(event)
  const id = body?.id || crypto.randomUUID()
  const label = body?.label || 'device'
  return registerDevice(sessionKey(event), id, label)
})
