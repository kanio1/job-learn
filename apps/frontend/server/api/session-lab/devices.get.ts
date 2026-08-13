export default defineEventHandler(async (event) => {
  await requireMirrorLabSession(event)
  return listDevices(sessionKey(event))
})
